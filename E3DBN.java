import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import java.security.Policy;
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

	// Current policy
	private Map<List<PartialState>, Action> currentPolicy;
	private Map<List<PartialState>, Action> prevExplorationPolicy;
	private Map<List<PartialState>, Action> prevExploitationPolicy;

	// Current time we have expl*
	private long currentTime;

	private Set<List<PartialState>> allStates;

	// This is the absorbing state which represents all unvisited states
	private List<PartialState> dummyState;

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
	 * @param dummyState
	 *            representation of the absorbing dummystate
	 */
	public E3DBN(double discount, double eps, double maxReward,
			TaskSpec taskspec, List<PartialState> dummyState) {

		stateVisits = new HashMap<>();

		String edges = taskspec.getExtraString();

		connections = Graph.graphFromString(edges);
		System.out.println(edges);
		System.out.println(connections);

		allActions = new AllActions();
		ptpl = new PartialTransitionProbabilityLogger(connections,
				partialStateKnownLimit, allActions);

		rewards = new HashMap<>();

		allStates = new HashSet<>();
		computeAllStates();
		// This is the absorbing state which represents all unvisited states
		this.dummyState = dummyState;

		this.maxReward = maxReward;

		this.discount = discount;
		horizonTime = Math.round((1 / (1 - discount)));
	}

	/*
	 * private Map<Integer, List<Integer>> parseConnectionsMessage( String
	 * responseMessage) { Map<Integer, List<Integer>> returnMap = new
	 * HashMap<>(); String[] connections = responseMessage.split(":"); for (int
	 * i = 0; i < connections.length; i++) { List<Integer> list = new
	 * ArrayList<Integer>(); for (String nr : connections[i].split("\\s+")) {
	 * int a = Integer.parseInt(nr); list.add(a); } returnMap.put(i, list); }
	 * return returnMap; }
	 */

	public Set<List<PartialState>> getAllStates() {
		return allStates;
	}

	private void computeAllStates() {
		int habitatCount = E3Agent.HABITATS_PER_REACHES * E3Agent.NBR_REACHES;
		System.out.println(habitatCount);
		List<List<Integer>> possibleStates = new ArrayList<>(habitatCount);
		for (int i = 0; i < habitatCount; i++) {
			List<Integer> inner = new ArrayList<>(3);
			possibleStates.add(inner);
			for (int j = 1; j <= 3; j++) {
				inner.add(j);
			}
		}
		System.out.println(possibleStates);
		for (List<Integer> intList : allCombinations(possibleStates)) {
			Observation o = new Observation(habitatCount, 0);
			for (int i = 0; i < intList.size(); i++) {
				o.intArray[i] = intList.get(i);
			}

			List<PartialState> state = Reach.allReaches(o, E3Agent.NBR_REACHES,
					E3Agent.HABITATS_PER_REACHES);
			allStates.add(state);

		}

	}
	private Map<Integer, Map<ParentValues, Integer>> partialPolicies;

	// Picking the next action {{{
	/**
	 * Find the next action.
	 */
	public Action nextAction(List<PartialState> state) {

		if (simulatorState == SimulatorState.Frozen) {
			if (ptpl.isKnown(state)) {
				policy = "frozen policy";
				return currentPolicy.get(state);
			} else {
				policy = "frozen balancing";
				return balancedWandering(state);
			}
		} else {

			// Unknown state
			if (!ptpl.isKnown(state)) {
				policy = "balancing";
				currentPolicy = null;
				currentTime = 0;
				balancingCount++;
				return balancedWandering(state);
			}

			// If expl* long enough: balanced wandering
			if (currentTime >= horizonTime) {
				currentPolicy = null;
				currentTime = 0;
			}

			// Start exploitation/exploration (the second condition is there
			// to deal with states becoming known while expl*ing.)
			if (currentTime == 0 || currentPolicy.get(state) == null) {
				if (prevExplorationKnownCount < ptpl.knownCount
						|| prevExplorationPolicy == null
						|| prevExplorationPolicy.get(state) == null) {

					currentPolicy = findExplorationPolicy();
					//partialPolicies = findPartialPolicies(true);
					prevExplorationPolicy = currentPolicy;
					prevExplorationKnownCount = ptpl.knownCount;
				} else {
					currentPolicy = prevExplorationPolicy;
				}
				
				policy = "exploration";
				//chanceToExplore = chanceToExplorePartial(partialPolicies, state);
				chanceToExplore = chanceToExplore(currentPolicy, state);

				// Should we really explore?
				if (chanceToExplore < exploreThreshold) {
					policy = "exploitation";
					if (prevExploitationKnownCount < ptpl.knownCount) {
						currentPolicy = findExploitationPolicy();
						prevExploitationPolicy = currentPolicy;
						prevExploitationKnownCount = ptpl.knownCount;
					} else {
						currentPolicy = prevExploitationPolicy;
					}
				}

			}

			currentTime++;

			/*
			 * if (policy.equals("exploration")) { Action a = new Action(4,0);
			 * a.intArray[0] = 1; a.intArray[1] = 1; a.intArray[2] = 1;
			 * a.intArray[3] = 1; return a; }
			 */
//			if (policy.equals("exploration")) {
//				actionFromPartialPolicy(partialPolicies, state);
//			}
			return currentPolicy.get(state);
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
		/*
		 * Action balancingAction = null; int lowest = Integer.MAX_VALUE; for
		 * (Action action : allActions.getAllActions(state)) { if
		 * (balancingAction == null) { lowest =
		 * ptpl.getSmallestPartialStateActionVisitCount(action, state);
		 * balancingAction = action; continue; } int temp =
		 * ptpl.getSmallestPartialStateActionVisitCount(action, state); if (temp
		 * < lowest) { lowest = temp; balancingAction = action; } }
		 * 
		 * return balancingAction;
		 */
	}
	
	private double chanceToExplorePartial( Map<Integer, Map<ParentValues, Integer>> PartialPolicies, List<PartialState> currentState) {
		//Find by simulation
		final int maxIterations = 400;
		Random r = new Random();
		ParentValues[] pvs = new ParentValues[currentState.size()];
		for(int i = 0; i < currentState.size(); i++) {
			pvs[i] = new ParentValues(currentState, connections.get(i));
		}
		int exploredCount = 0;
		for(int iteration = 0; iteration < maxIterations; iteration++) {
			List<PartialState> nextState = new ArrayList<>(currentState.size()); 
			for(int step = 0; step < horizonTime; step++) {
				for(int stateIndex = 0; stateIndex < currentState.size(); stateIndex++) {
					double roll = r.nextDouble();
					double acc = 0;

					for(PartialState ps : Reach.allPartials(allStates)) {
						acc += ptpl.getProbability(stateIndex, pvs[stateIndex],
								partialPolicies.get(stateIndex).get(pvs[stateIndex])
								, ps);
						if (acc > roll) {
							nextState.add(ps);
							break;
						}
					}
				
				}
			}
			if (ptpl.isKnown(nextState)) {
				exploredCount++;
			}
		}
		return exploredCount / maxIterations;
	}

	/**
	 * Should we explore given some policy?
	 */
	private double chanceToExplore(
			Map<List<PartialState>, Action> explorationPolicy,
			List<PartialState> currentState) {
		Map<List<PartialState>, Double> probs = new HashMap<>();

		if (!ptpl.isKnown(currentState))
			throw new ArithmeticException();

		// Starting probabilities are 0 for known states and 1 for unknown
		for (List<PartialState> state : getAllStates()) {
			if (ptpl.isKnown(state)) {
				probs.put(state, 0.0);
			} else {
				probs.put(state, 1.0);
			}
		}

		// Find probability that we end up in an unknown state in horizonTime
		// steps
		for (long i = 0; i < horizonTime; i++) {
			for (List<PartialState> state : ptpl.getObservedStates()) {
				if (ptpl.isKnown(state)) {
					double probability = 0;

					// For all possible transitions, multiply transition
					// probability by the stored value for the target state.
					List<List<PartialState>> nextStates = ptpl
							.statesFromPartialStates(state,
									explorationPolicy.get(state));
					// System.out.println("state: " + state + " nextstates: " +
					// nextStates);
					for (List<PartialState> nextState : nextStates) {
						Double d = probs.get(nextState);
						d = d == null ? 1 : d;
						probability += d
								* ptpl.getProbability(state,
										explorationPolicy.get(state), nextState);
					}
					probs.put(state, probability);
				}
			}
		}

		// TODO: 0.05 should be epsilon / (2 * G^T_max) (see paper, section 6)
		chanceToExplore = probs.get(currentState);

		return chanceToExplore;
	}

	// }}}

	// Policy calculations {{{

	/**
	 * Performs value iteration in to find a policy that explores new states
	 * within horizonTime
	 */
	private Map<List<PartialState>, Action> findExplorationPolicy() {
		return findPolicy(true);
	}

	/**
	 * Performs value iteration in to find a policy that exploits new states
	 * within horizonTime
	 */
	private Map<List<PartialState>, Action> findExploitationPolicy() {
		return findPolicy(false);
	}

	private Map<List<PartialState>, Action> findPolicy(boolean explorationPolicy) {
		if (explorationPolicy) {
			ptpl.clearProbCache();
		}
		// Value function
		Map<List<PartialState>, Double> vf = new HashMap<>();
		Map<List<PartialState>, Double> prevVf;

		// Policy
		Map<List<PartialState>, Action> policy = new HashMap<>();

		// Initialize value function
		for (List<PartialState> state : ptpl.getObservedStates()) {
			if (ptpl.isKnown(state)) {
				vf.put(state, 0.0);
			}
		}

		vf.put(dummyState, 0.0);
		setReward(dummyState, explorationPolicy ? maxReward : 0);

		prevVf = new HashMap<>(vf);

		for (long t = 0; t < horizonTime; t++) {
			for (List<PartialState> state : vf.keySet()) {
				double bestValue = Double.NEGATIVE_INFINITY;
				Action bestAction = null;

				if (state.equals(dummyState)) {
					vf.put(state, getReward(state) + discount * prevVf.get(state));

					continue;
				}

				for (Action action : allActions.getAllActions(state)) {
					double currentValue = 0;
					double totalProb = 0;
					for (List<PartialState> nextState : allStates) {
						double thisProb = ptpl.getProbability(state, action,
								nextState);
						totalProb += thisProb;
						if (vf.keySet().contains(nextState)) {

							currentValue += thisProb * prevVf.get(nextState);
						} else {
							currentValue += thisProb * prevVf.get(dummyState);
						}
					}
					if ((totalProb > 1.1 || totalProb < 0.9) && totalProb != 0) {
						System.out.println("hej " + totalProb);
						throw new ArithmeticException();

					}
					if (currentValue > bestValue) {
						bestValue = currentValue;
						bestAction = action;
					}
				}

				vf.put(state, (explorationPolicy) ? 0 : getReward(state)
						+ discount * bestValue);
				policy.put(state, bestAction);
			}
			prevVf = vf;
		}
		return policy;
	}

	private Map<Integer, Map<ParentValues, Integer>> foundPartialPolicies = new HashMap<>();
	private Map<Integer, Map<PartialState, Map<PartialState, Double>>> 
		markovs = new HashMap<>();
	
	public Map<Integer, Map<ParentValues, Integer>> findPartialPolicies(boolean findExplorationPolicy) {
		foundPartialPolicies.clear();
		markovs.clear();
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
					for (ParentValues nextState : Reach.allParentValues(allStates, connections.get(stateIndex))) {
						double thisProb = 1.0;
						int i = 0;
						for (PartialState ps : nextState.getParents()) {
							if (ps == nextState.getSelfParent()) {
								thisProb *= ptpl.getProbability(stateIndex, pv, action, ps);
								i++;
							} else {
								Map<PartialState, Map<PartialState, Double>> map =
										markovs.get(connections.get(stateIndex).get(i));
								PartialState a = pv.getParent(nextState.getParents().indexOf(ps));
								Map<PartialState, Double> map2 = map.get(a);
								if (markovs.get(connections.get(stateIndex).get(i)).
										get(pv.
												getParent(nextState.getParents().
												indexOf(ps))) == null)  {
									//thisProb = 0;
								} else {
									thisProb *= 
										markovs.get(connections.get(stateIndex).get(i++)).
										get(pv.
												getParent(nextState.getParents().
												indexOf(ps))).
										get(ps);
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
					}
					if (currentValue > bestValue) {
						transProbs.put(pv.getSelfParent(), transProb);
						bestValue = currentValue;
						bestAction = action;

					}
				}
				vf.put(pv, (findExplorationPolicy) ? 0 : pv.getSelfParent().getCost(bestAction)
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
		currentPolicy = findExploitationPolicy();
	}

	public void unFreezePolicy() {
		System.out.println("UnFreezing");
		this.simulatorState = SimulatorState.UnFrozen;
	}

	// }}}

}
