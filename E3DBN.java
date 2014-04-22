import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.*;

/**
 * An implementation of the E3 algorithm in a discounted MDP.
 */
public class E3DBN {
	
	private class AllActions implements AllActionsGetter {

		@Override
		public List<Action> getAllActions(Observation state) {
            List<Action> actions = new ArrayList<>();
            for (List<Integer> actints : Utilities.getActions(state.intArray, 7, 4)) {
                Action act = new Action(7, 0, 0);

                for (int i = 0; i < 7; i++) {
                    act.setInt(i, actints.get(i));
                }

                actions.add(act);
            }

			return actions;
		}
		
	}

	private AllActions allActions;
    private double discount;
    private long horizonTime;
    private double maxReward;
    private final int partialStateKnownLimit = 10;
    
    //mostly for debugging
    private int balancingCount = 0;
    private double chanceToExplore ;

    // State, Action, State -> Visits
    private Map<Observation, Integer> stateVisits;

    public PartialTransitionProbabilityLogger ptpl;
    
    // State -> Reward
    private Map<Observation, Double> rewards;

    // Current policy
    private Map<Observation, Action> currentPolicy;

    // Current time we have expl*
    private long currentTime;

    // All possible actions
    private List<Action> possibleActions;

    // This is the absorbing state which represents all unvisited states
    private Observation dummyState;

    /**
     * @param discount discount factor
     * @param eps the epsilon parameter
     * @param maxReward the maximum possible reward
     * @param actions list of all possible actions
     * @param dummyState representation of the absorbing dummystate
     */
    public E3DBN(
            double discount,
            double eps,
            double maxReward,
            List<Action> actions,
            TaskSpec taskspec,
            Observation dummyState) {

        stateVisits = new HashMap<>();

        String edges = taskspec.getExtraString();

        DiGraph graph = DiGraph.graphFromString(edges);
        System.out.println(edges);
        System.out.println(graph.edges());

        int habitats = taskspec.getNumDiscreteObsDims();

        graph = new DiGraph();
        for (int i = 0; i < habitats; i++) {
            graph.addEdge(i, i);
        }

        allActions = new AllActions();
        ptpl = new PartialTransitionProbabilityLogger(
                        graph.edges(), actions, partialStateKnownLimit, 
                        allActions);


        rewards = new HashMap<>();

        // This is the absorbing state which represents all unvisited states
        this.dummyState = dummyState;

        this.maxReward = maxReward;

        this.discount = discount;
        horizonTime = Math.round((1 / (1 - discount)));
        possibleActions = actions;
    }    
    private Map<Integer, List<Integer>> parseConnectionsMessage(
            String responseMessage) {
        Map<Integer, List<Integer>> returnMap = new HashMap<>();
        String[] connections = responseMessage.split(":");
        for (int i = 0; i < connections.length; i++) {
            List<Integer> list = new ArrayList<Integer>();
            for (String nr : connections[i].split("\\s+")) {
                int a = Integer.parseInt(nr);
                list.add(a);
            }
            returnMap.put(i, list);
        }
        return returnMap;
    }
    
    @SuppressWarnings("unused")
	private void l(Object obj) {
        System.out.println(obj);
    }


    // Picking the next action {{{

    public String policy = "";

    /**
     * Find the next action.
     */
    public Action nextAction(Observation state) {

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
        if (currentPolicy == null || currentPolicy.get(state) == null) {
            currentPolicy = findExplorationPolicy(state);
            policy = "exploration";

            // Should we really explore?
            if (!shouldExplore(currentPolicy, state)) {
                currentPolicy = findExploitationPolicy(state);
                policy = "exploitation";
            }
        }

        currentTime++;
        return currentPolicy.get(state);
    }

    /**
     * Find the balanced wandering action
     */
    private Action balancedWandering(Observation state) {
        Action balancingAction = null;

        int lowest = Integer.MAX_VALUE;
        for (Action action : allActions.getAllActions(state)) {
            if (balancingAction == null) { 
            	lowest = ptpl.getSmallestPartialStateActionVisitCount(action, state);
            	balancingAction = action; continue; 
            	}
            int temp = ptpl.getSmallestPartialStateActionVisitCount(action, state);
            if (temp < lowest) {
            	lowest = temp;
                balancingAction = action;
            }
        }

        return balancingAction;
    }


    /**
     * Should we explore given some policy?
     */
    private boolean shouldExplore(Map<Observation, Action> explorationPolicy, 
            Observation currentState) {
        Map<Observation, Double> probs = new HashMap<>();
      
        // Starting probabilities are 0 for known states and 1 for unknown
        for (Observation state : ptpl.getObservedStates()) {
            if (ptpl.isKnown(state)) {
                probs.put(state, 0.0);
            } else {
                probs.put(state, 1.0);
            }
        }

        // Find probability that we end up in an unknown state in horizonTime steps
        for (long i = 0; i < horizonTime; i++) {
            for (Observation state : ptpl.getObservedStates()) {
                if (ptpl.isKnown(state)) {
                    double probability = 0;

                    // For all possible transitions, multiply transition
                    // probability by the stored value for the target state. 
                    List<Observation> nextStates = 
                    		ptpl.statesFromPartialStates(state, explorationPolicy.get(state));
                    for (Observation nextState : nextStates) {
                    	Double d = probs.get(nextState);
                    	d = d == null ? 1 : d;
                    	probability += d * ptpl.getProbability(state, 
                    			explorationPolicy.get(state), nextState);
                    }
                    probs.put(state, probability);
                }
            }
        }

        // TODO: 0.05 should be epsilon / (2 * G^T_max) (see paper, section 6)
        chanceToExplore = probs.get(currentState);
        return probs.get(currentState) > 0.05;
    }

    // }}}

    // Policy calculations {{{

    /**
     * Performs value iteration in to find a policy that explores new states within horizonTime
     */
    private Map<Observation, Action> findExplorationPolicy(Observation currentState) {

        // Value function
        Map<Observation, Double> vf = new HashMap<>();

        // Policy
        Map<Observation, Action> policy = new HashMap<>();

        // Initialize value function
        for (Observation state : ptpl.getObservedStates()) {
            if (ptpl.isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        ptpl.clearProbabilityCache();
                
        vf.put(dummyState, 0.0);
        setReward(dummyState, maxReward);

        for (long t = 0; t < horizonTime; t++) {
            for (Observation state : vf.keySet()) {
                double bestValue = Double.NEGATIVE_INFINITY;
                Action bestAction = null;

                if (state.equals(dummyState)) {
                    vf.put(state, getReward(state) + discount * vf.get(state));

                    continue;
                }

                for (Action action : allActions.getAllActions(state)) {
                    double currentValue = 0;
                    //List<Observation> nextStates = 
                    //		ptpl.statesFromPartialStates(state, action);
                    for (Observation o : ptpl.getObservedStates()) {
                    	//(if known)
                        if (vf.keySet().contains(o) && !dummyState.equals(o)) {
                            currentValue += 
                            		ptpl.getProbability(state, action, o) * vf.get(o);
                        } else {
                            currentValue += 
                            		ptpl.getProbability(state, action, o) * vf.get(dummyState);
                        }
                    }
                    
                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestAction = action;
                    }
                }

                vf.put(state, getReward(state) + discount * bestValue);
                policy.put(state, bestAction);
            }
        }
        return policy;
    }

    private Map<Observation, Action> findExploitationPolicy(Observation currentState) {

        // Value function
        Map<Observation, Double> vf = new HashMap<>();

        // Policy
        Map<Observation, Action> policy = new HashMap<>();
        
        // Initialize value function
        for (Observation state : ptpl.getObservedStates()) {
            if (ptpl.isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        vf.put(dummyState, 0.0);
        setReward(dummyState, 0.0);

        for (long t = 0; t < horizonTime; t++) {
            for (Observation state : vf.keySet()) {
                double bestValue = Double.NEGATIVE_INFINITY;
                Action bestAction = null;

                if (state.equals(dummyState)) {
                    vf.put(state, getReward(state) + discount * vf.get(state));

                    continue;
                }

                for (Action action : allActions.getAllActions(state)) {
                    double currentValue = 0;

                    List<Observation> nextStates = 
                    		ptpl.statesFromPartialStates(state, action);
                    for (Observation o : nextStates) {
                    	//(if known)
                    	if (vf.keySet().contains(o) && !dummyState.equals(o)) {
                            currentValue += 
                            		ptpl.getProbability(state, action, o) * vf.get(o);
                        } else {
                            // Do nothing if state is unknown
                        }
                    }

                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestAction = action;
                    }
                }

                vf.put(state, getReward(state) + discount * bestValue);
                policy.put(state, bestAction);
            }
        }

        return policy;
    }

    // }}}

    // Updating necessary statistics {{{

    /**
     * Observe a state transition.
     */
    public void observe(
            Observation from, 
            Action action,
            Observation to,
            double reward
    ) {

        // Log the visit
        updateVisits(from, action, to);

        // Update reward table
        updateReward(to, reward);
        
        ptpl.record(from, action, to);
    }

    /**
     * Update visits statistics
     */
    private void updateVisits(Observation from, Action action, Observation to) {
        // State visits
        stateVisits.put(from, stateVisits.containsKey(from) ? stateVisits.get(from) + 1 : 1);

    }


    /**
     * Update reward table.
     */
    private void updateReward(Observation to, double reward) {
        int visits = getVisits(to);

        double oldReward = 0;

        if (rewards.containsKey(to)) {
            oldReward = rewards.get(to);
        }

        rewards.put(to, (reward + oldReward * visits) / (visits + 1));
    }

    /**
     * Update reward table.
     */
    private void setReward(Observation to, double reward) {
        rewards.put(to, reward);
    }

    // }}}

    // Get state, action, state visit statistics {{{

    private int getVisits(Observation state) {
        return stateVisits.containsKey(state) ? stateVisits.get(state) : 0;
    }

    private double getReward(Observation state) {
        return rewards.get(state);
    }

    public int getKnownCount() {
    	return ptpl.knownCount;
    }

    public int getBalancingActionCount() {
    	return balancingCount;
    }

    public double getChanceToExplore() {
    	return chanceToExplore;
    }
    
    // }}}

}
