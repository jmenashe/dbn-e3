import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;

import java.util.*;

/**
 * An implementation of the E3 algorithm in a discounted factored MDP.
 */
public class E3DBN {
	private static Map<List<List<Integer>>, List<List<Integer>>> cache = new HashMap<>();

	private static List<List<Integer>> allCombinations(List<List<Integer>> input) {
		List<List<Integer>> returnList = cache.get(input);

		if (returnList != null) {
			return returnList;
		}

		int max = 1;
		int[] sizes = new int[input.size()];
		int k = 0;
		for (List<Integer> l : input) {
			max *= l.size();
			sizes[k++] = l.size();
		}
		returnList = new ArrayList<>(max);
		for (int i = 0; i < max; i++) {
			int iCpy = i;
			List<Integer> subList = new ArrayList<>(input.size());
			for (int j = 0; j < input.size(); j++) {
				subList.add(input.get(j).get(iCpy % sizes[j]));
				iCpy = iCpy / sizes[j];
			}
			returnList.add(subList);
		}
		cache.put(input, returnList);
		return returnList;
	}

	private class AllActions implements AllActionsGetter {

		@Override
		public List<Action> getAllActions(List<PartialState> state) {
			List<Action> actions = new ArrayList<>();
			List<List<Integer>> partialActions = new ArrayList<>(state.size());
			for (PartialState partial : state) {
				partialActions.add(partial.possibleActions());
			}
			for (List<Integer> actInts : allCombinations(partialActions)) {
				Action act = new Action(E3Agent.NBR_REACHES, 0, 0);

				for (int i = 0; i < E3Agent.NBR_REACHES; i++) {
					act.setInt(i, actInts.get(i));
				}

				actions.add(act);
			}

			return actions;
		}

	}

	private AllActions allActions;
	private final double exploreThreshold = 0.05;
	private double discount;
	private long horizonTime;
	private double maxReward;
	private final int partialStateKnownLimit = 5;
	private int prevExplorationKnownCount = -1;
	private int prevExploitationKnownCount = -1;
	public String policy = "";

	// mostly for debugging
	private int balancingCount = 0;
	double chanceToExplore;

	// State, Action, State -> Visits
	private Map<List<PartialState>, Integer> stateVisits;

	public PartialTransitionProbabilityLogger ptpl;

	// State -> Reward
	private Map<List<PartialState>, Double> rewards;

	// Current Partial policy
	private Map<Integer, Map<ParentValues, Integer>> currentPartialPolicies;
	private Map<Integer, Map<ParentValues, Integer>> prevExplorationPartialPolicies;
	private Map<Integer, Map<ParentValues, Integer>> prevExploitationPartialPolicies;

	
	// Current time we have expl*
	private long currentTime;


	private Map<Integer, List<Integer>> connections;

	public enum SimulatorState {
		Frozen, UnFrozen
	}

	private SimulatorState simulatorState = SimulatorState.UnFrozen;

	/**
	 * @param discount
	 *            discount factor
	 * @param eps
	 *            the epsilon parameter
	 * @param maxReward
	 *            the maximum possible reward
	 * @param taskspec
	 *            the taskspec
	 */
	public E3DBN(double discount, double eps, double maxReward,
			TaskSpec taskspec) {

		stateVisits = new HashMap<>();

		String edges = taskspec.getExtraString();

		connections = Graph.graphFromString(edges);
		System.out.println(edges);
		System.out.println(connections);

		allActions = new AllActions();
		ptpl = new PartialTransitionProbabilityLogger(connections,
				partialStateKnownLimit, allActions);

		rewards = new HashMap<>();

		this.maxReward = maxReward;

		this.discount = discount;
		horizonTime = Math.round((1 / (1 - discount)));
	}


	// Picking the next action {{{
	/**
	 * Find the next action.
	 */
	public Action nextAction(List<PartialState> state) {

		if (simulatorState == SimulatorState.Frozen) {
			if (ptpl.isKnown(state)) {
				policy = "frozen policy";
				return actionFromPartialPolicy(currentPartialPolicies, state);
			} else {
				policy = "frozen balancing";
				return balancedWandering(state);
			}
		} else {

			// Unknown state
			if (!ptpl.isKnown(state)) {
				policy = "balancing";
				currentPartialPolicies = null;
				currentTime = 0;
				balancingCount++;
				return balancedWandering(state);
			}

			// If expl* long enough: balanced wandering
			if (currentTime >= horizonTime) {
				currentPartialPolicies = null;
				currentTime = 0;
			}

			// Start exploitation/exploration (the second condition is there
			// to deal with states becoming known while expl*ing.)
			if (currentTime == 0) {
				if (prevExplorationKnownCount < ptpl.knownCount) {

					currentPartialPolicies = findPartialPolicies(true);
					prevExplorationPartialPolicies = currentPartialPolicies;
					prevExplorationKnownCount = ptpl.knownCount;
				} else {
					currentPartialPolicies = prevExplorationPartialPolicies;
				}
				
				policy = "exploration";
				chanceToExplore = chanceToExplorePartial(currentPartialPolicies, state);

				// Should we really explore?
				if (chanceToExplore < exploreThreshold) {
					policy = "exploitation";
					if (prevExploitationKnownCount < ptpl.knownCount) {
						currentPartialPolicies = findPartialPolicies(false);
						prevExploitationPartialPolicies = currentPartialPolicies;
						prevExploitationKnownCount = ptpl.knownCount;
					} else {
						currentPartialPolicies = prevExploitationPartialPolicies;
					}
				}

			}

			currentTime++;

			return actionFromPartialPolicy(currentPartialPolicies, state);

		}
	}

	/**
	 * Find the balanced wandering action
	 */
	private Action balancedWandering(List<PartialState> state) {

		Action action = new Action(state.size(), 0);

		for (int stateIndex = 0; stateIndex < state.size(); stateIndex++) {
			ParentValues pv = new ParentValues(state, ptpl.getConnections()
					.get(stateIndex));
			int smallestCount = Integer.MAX_VALUE;
			int bestAction = 0;
			for (int partialAction : state.get(stateIndex).possibleActions()) {
				int newCount = ptpl.getParentSettingPartialActionCount(
						stateIndex, pv, partialAction);
				if (smallestCount > newCount) {
					smallestCount = newCount;
					bestAction = partialAction;
				}
			}
			action.intArray[stateIndex] = bestAction;
		}

		return action;
	}
	
	private double chanceToExplorePartial( Map<Integer, Map<ParentValues, Integer>> PartialPolicies, List<PartialState> currentState) {
		//Find by simulation
		final int maxIterations = 1000;
		Random r = new Random();
		ParentValues[] pvs = new ParentValues[currentState.size()];
		for(int i = 0; i < currentState.size(); i++) {
			pvs[i] = new ParentValues(currentState, connections.get(i));
		}
		int exploredCount = 0;
		for(int iteration = 0; iteration < maxIterations; iteration++) {
			for(int step = 0; step < horizonTime; step++) {
				List<PartialState> nextState = new ArrayList<>(currentState.size()); 
				for(int stateIndex = 0; stateIndex < currentState.size(); stateIndex++) {
					double roll = r.nextDouble();
					double acc = 0;
					Set<PartialState> set = Reach.allPartials(E3Agent.HABITATS_PER_REACHES);

					for(PartialState ps : set) {
						acc += ptpl.getProbability(stateIndex, pvs[stateIndex],
								currentPartialPolicies.get(stateIndex).get(pvs[stateIndex])
								, ps);
						if (acc > roll) {
							nextState.add(ps);
							break;
						}
					}
				
				}
				if (!ptpl.isKnown(nextState)) {
					exploredCount++;
					break;
				}
			}
		}

		return exploredCount / (double)maxIterations;
	}

	private Map<Integer, Map<ParentValues, Integer>> foundPartialPolicies = new HashMap<>();
	private Map<Integer, Map<PartialState, Map<PartialState, Double>>> 
		markovs = new HashMap<>();
	
	public Map<Integer, Map<ParentValues, Integer>> findPartialPolicies(boolean findExplorationPolicy) {
		
		
		foundPartialPolicies.clear();
		//This happens when the experiment calls for a frozen policy at the start
		if (ptpl.knownCount == 0) {
			return foundPartialPolicies;
		}markovs.clear();
		for(int i = 0; i < E3Agent.NBR_REACHES; i++) {
			findPartialPolicy(i, findExplorationPolicy);
		}
		return foundPartialPolicies;
	}
	
	public Action actionFromPartialPolicy(Map<Integer, Map<ParentValues, Integer>> partialPolicies, List<PartialState> state) {
		Action action = new Action(state.size(), 0);
		for(int i = 0; i < state.size(); i++) {		
			int partialAction = partialPolicies.get(i).get(
					new ParentValues(state, connections.get(i)));
			action.intArray[i] = partialAction;
		}
		return action;
		
	}
	
	private void findPartialPolicy(int stateIndex, boolean findExplorationPolicy) {
		
		
		Map<ParentValues, Double> vf = new HashMap<>();
		Map<ParentValues, Double> prevVf = new HashMap<>();
		
		Map<ParentValues, Integer> policy= new HashMap<>();
		Map<PartialState, Map<PartialState, Double>> transProbs = new HashMap<>();
		
	
		//Find policies of parents first
		for(Integer i : connections.get(stateIndex)) {
			if (foundPartialPolicies.get(i) == null && i != stateIndex) {
				findPartialPolicy(i, findExplorationPolicy);
			}
		}
		for (ParentValues pv : ptpl.knownPartialStates.get(
				connections.get(stateIndex).size()).keySet()) {
			int knownSize = ptpl.knownPartialStates.get(connections.get(stateIndex).size()).get(pv).size();
			int allSize = pv.getSelfParent().possibleActions().size();
			//if all actions from the state have known transitions, put in this list
			if (knownSize ==  allSize) {
				vf.put(pv, 0.0);
			}
		}
		ParentValues dummy = new ParentValues(null, new ArrayList<Integer>());
		vf.put(dummy, findExplorationPolicy ? maxReward : 0);
		prevVf = new HashMap<>(vf);				
		for (int t = 0; t < horizonTime; t++) {
			for (ParentValues pv : vf.keySet()) {
				double bestValue = Double.NEGATIVE_INFINITY;
				int bestAction = 0;

				if (pv.equals(dummy)) {
					vf.put(dummy, (findExplorationPolicy ? maxReward : 0) + 
							discount * prevVf.get(dummy));
					continue;
				}
				
				for (int action : pv.getSelfParent().possibleActions()) {
					double currentValue = 0;
					Map<PartialState,Double> transProb = new HashMap<>();
					Set<ParentValues> set = Reach.allParentValues(E3Agent.HABITATS_PER_REACHES, 
							E3Agent.NBR_REACHES, connections.get(stateIndex));
					
					double totalProb = 0;
					for (ParentValues nextState : set) {
						double thisProb = 1.0;
						int i = 0;
						for (PartialState ps : nextState.getParents()) {
							if (ps == nextState.getSelfParent()) {
								thisProb *= ptpl.getProbability(stateIndex, pv, action, ps);
								i++;
							} else {
								Map<PartialState, Map<PartialState, Double>> map =
										markovs.get(connections.get(stateIndex).get(i++));
								PartialState a = pv.getParent(nextState.getParents().indexOf(ps));
								Map<PartialState, Double> map2 = map.get(a);
								if (map2 == null)  {
									thisProb = 0;
								} else {
									thisProb *= map2.get(ps);
								}
								
							}
						}
						Double oldval = transProb.get(nextState.getSelfParent());
						oldval = oldval == null ? 0 : oldval;
						transProb.put(nextState.getSelfParent(), oldval + thisProb);

						if (vf.keySet().contains(nextState)) {
							currentValue += thisProb * prevVf.get(nextState);
						} else {
							currentValue += thisProb * prevVf.get(dummy);
						}
						totalProb += thisProb;
					}
					if ((totalProb > 1.1 || totalProb < 0.9) && totalProb != 0.0) {
						System.out.println("hej " + totalProb);
						throw new ArithmeticException("probabilities in disarray");
					}
					if (currentValue > bestValue) {
						transProbs.put(pv.getSelfParent(), transProb);
						bestValue = currentValue;
						bestAction = action;

					}
				}
				double d = pv.getSelfParent().getReward(bestAction);
				vf.put(pv, ((findExplorationPolicy) ? 0 : d)
						+ discount * bestValue);
				policy.put(pv, bestAction);
			}
			prevVf = vf;
		}		
		markovs.put(stateIndex,transProbs);
		foundPartialPolicies.put(stateIndex, policy);
		
	}

	// }}}

	// Updating necessary statistics {{{

	/**
	 * Observe a state transition.
	 */
	public void observe(List<PartialState> from, Action action,
			List<PartialState> to, double reward) {
		if (simulatorState == SimulatorState.Frozen) {
			return;
		}
		// Log the visit
		updateVisits(from, action, to);

		// Update reward table
		updateReward(from, reward);

		ptpl.record(from, action, to);
	}

	/**
	 * Update visits statistics
	 */
	private void updateVisits(List<PartialState> from, Action action,
			List<PartialState> to) {
		// State visits
		stateVisits.put(to,
				stateVisits.containsKey(to) ? stateVisits.get(to) + 1 : 1);

	}

	/**
	 * Update reward table.
	 */
	private void updateReward(List<PartialState> from, double reward) {
		int visits = getVisits(from);

		double oldReward = 0;

		if (rewards.containsKey(from)) {
			oldReward = rewards.get(from);
		}

		rewards.put(from, (reward + oldReward * visits) / (visits + 1));
	}

	/**
	 * Update reward table.
	 */
	private void setReward(List<PartialState> to, double reward) {
		rewards.put(to, reward);
	}

	// }}}

	// Get state, action, state visit statistics {{{

	private int getVisits(List<PartialState> state) {
		return stateVisits.containsKey(state) ? stateVisits.get(state) : 0;
	}

	private double getReward(List<PartialState> state) {
		//TODO: "unknown" state. Not observed, but all partial transitions are known
		if (rewards.get(state) == null)
		{
			return 0;
		}
		return rewards.get(state);
	}

	public int getKnownPartialsCount() {
		return ptpl.knownCount;
	}

	public int getKnownFullCount() {
		return ptpl.knownStates.size();
	}

	public int getBalancingActionCount() {
		return balancingCount;
	}

	public double getChanceToExplore() {
		return chanceToExplore;
	}

	// }}}

	// {{{ Simulator shiz

	public void freezePolicy() {
		System.out.println("Freezing");
		this.simulatorState = SimulatorState.Frozen;
		currentPartialPolicies = findPartialPolicies(false);
		//currentPolicy = findExploitationPolicy();
	}

	public void unFreezePolicy() {
		System.out.println("UnFreezing");
		this.simulatorState = SimulatorState.UnFrozen;
	}

	// }}}

}
