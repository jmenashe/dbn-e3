import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * An implementation of the E3 algorithm in a discounted MDP.
 *
 */
public class E3<State, Action> {

    public double discount;
    private long horizonTime;
    private int maxReward;


    // Transition probabilities in the MDP
    private TransitionProbabilities<State, Action> tps;

    // State, Action, State -> Visits
    private Map<State, Map<Action, Map<State, Integer>>>
        sasv;

    // State -> Reward
    private Map<State, Integer> rewards;

    // Current policy
    private Map<State, Action> currentPolicy;

    // Current time we have expl*
    private long currentTime;

    // All possible actions
    private List<Action> possibleActions;

    // This is the absorbing state which represents all unvisited states
    private State dummyState;

    /**
     * @param discount discount factor
     * @param actions list of all possible actions
     * @param dummyState representation of the absorbing dummystate
     */
    public E3(double discount, int maxReward, List<Action> actions, State dummyState) {
        tps = new TransitionProbabilities<>();
        sasv = new HashMap<>();
        rewards = new HashMap<>();

        // This is the absorbing state which represents all unvisited states
        this.dummyState = dummyState;

        this.maxReward = maxReward;

        this.discount = discount;
        horizonTime = Math.round(1 / (1 - discount));
        possibleActions = actions;
    }

    // Picking the next action {{{

    /**
     * Find the next action.
     */
    public Action nextAction(State state) {

        // Unknown state or expl* long enough: balanced wandering
        if (!isKnown(state) || currentTime >= horizonTime) {
            currentPolicy = null;
            currentTime = 0;
            return balancedWandering(state);
        }

        // Start exploitation/exploration
        if (currentPolicy == null) {
            currentPolicy = findExplorationPolicy(state);

            // Should we really explore?
            if (!shouldExplore(currentPolicy, state)) {
                currentPolicy = findExploitationPolicy(state);
            }
        }

        currentTime++;
        return currentPolicy.get(state);
    }

    /**
     * Find the balanced wandering action
     */
    private Action balancedWandering(State state) {
        Action balancingAction = null;

        for (Action action : getPossibleActions(state)) {
            if (balancingAction == null) { balancingAction = action; continue; }

            if (getVisits(state, action) < getVisits(state, balancingAction)) {
                balancingAction = action;
            }
        }

        return balancingAction;
    }

    /**
     * All possible actions in a state
     * TODO: Return an iterator over all possible actions from state
     */
    private List<Action> getPossibleActions(State state) {
        return possibleActions;
    }

    /**
     * Is a state known?
     *
     * TODO: This :). See E3 paper.
     */
    private boolean isKnown(State state) {
        return getVisits(state) > 100;
    }

    /**
     * Should we explore given some policy?
     */
    private boolean shouldExplore(Map<State, Action> explorationPolicy, State currentState) {
        Map<State, Double> probs = new HashMap<>();

        // Starting probabilities are 0 for known states and 1 for unknown
        for (State state : tps.getStates()) {
            if (isKnown(state)) {
                probs.put(state, 0.0);
            } else {
                probs.put(state, 1.0);
            }
        }

        // Find probability that we end up in a unknown state in horizonTime steps
        for (long i = 0; i < horizonTime; i++) {
            for (State state : tps.getStates()) {
                if (isKnown(state)) {
                    double probability = 0;

                    // For all possible transitions, multiply transition
                    // probability by the stored value for the target state. 
                    for (Map.Entry<State, Double> sp : tps.from(state, explorationPolicy.get(state)).entrySet()) {
                        probability += probs.get(sp.getKey()) * sp.getValue();
                    }
                    probs.put(state, probability);
                }
            }
        }

        // TODO: 0.05 should be epsilon / (2 * G^T_max) (see paper, section 6)
        return probs.get(currentState) > 0.05;
    }

    // }}}

    // Policy calculations {{{

    /**
     * Performs value iteration in to find a policy that explores new states within horizonTime
     */
    private Map<State, Action> findExplorationPolicy(State currentState) {

        // Value function
        Map<State, Double> vf = new HashMap<>();

        // Policy
        Map<State, Action> policy = new HashMap<>();

        // Initialize value function
        for (State state : tps.getStates()) {
            if (isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        vf.put(dummyState, (double)maxReward); // TODO: What is the value of the absorbing state

        for (long t = horizonTime; t >= 1; t--) {
            for (State state : vf.keySet()) {
                double bestValue = 0;
                Action bestAction = null;

                for (Action action : getPossibleActions(state)) {
                    double currentValue = 0;

                    for (Map.Entry<State, Double> sp : tps.from(state, action).entrySet()) {
                        if (isKnown(sp.getKey())) {
                            currentValue += sp.getValue() * vf.get(sp.getKey());
                        } else {
                            currentValue += sp.getValue() * vf.get(dummyState);
                        }
                    }

                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestAction = action;
                    }
                }

                vf.put(state, getReward(state)  + discount * bestValue);
                policy.put(state, bestAction);
            }
        }

        return policy;
    }

    private Map<State, Action> findExploitationPolicy(State currentState) {

        // Value function
        Map<State, Double> vf = new HashMap<>();

        // Policy
        Map<State, Action> policy = new HashMap<>();

        // Initialize value function
        for (State state : tps.getStates()) {
            if (isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        vf.put(dummyState, 0.0);

        for (long t = horizonTime; t >= 1; t--) {
            for (State state : vf.keySet()) {
                double bestValue = 0;
                Action bestAction = null;

                for (Action action : getPossibleActions(state)) {
                    double currentValue = 0;

                    for (Map.Entry<State, Double> sp : tps.from(state, action).entrySet()) {
                        if (isKnown(sp.getKey())) {
                            currentValue += sp.getValue() * vf.get(sp.getKey());
                        } else {
                            // Unknown states get no reward
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
            State from, 
            Action action,
            State to,
            int reward
    ) {
        // Log the visit
        updateVisits(from, action, to);

        // Update transition probabilities
        updateTP(from, action, to);

        // Update reward table
        updateReward(from, action, to, reward);
    }

    /**
     * Update visits statistics
     */
    private void updateVisits(State from, Action action, State to) {
        Map<Action, Map<State, Integer>> asv = sasv.get(from);
        if (asv == null) {
            asv = new HashMap<>();
            sasv.put(from, asv);
        }

        Map<State, Integer> sv = asv.get(action);
        if (sv == null) {
            sv = new HashMap<>();
            asv.put(action, sv);
        }

        Integer v = sv.get(to);
        if (v == null) {
            v = 0;
        }

        v += 1;
        sv.put(to, v);
    }

    /**
     * Update the transition probabilities
     */
    private void updateTP(State from, Action action, State to) {
        int sav = getVisits(from, action);

        for (Map.Entry<State, Integer> sv : sasv.get(from).get(action).entrySet()) {
            tps.setTP(
                    from,
                    action,
                    sv.getKey(),
                    (double) sv.getValue() / sav);
        }
    }

    /**
     * Update reward table.
     */
    private void updateReward(State from, Action action, State to, int reward) {
        int r = rewards.get(to);

        //if (r == null) {
            //r = 0;
        //}

        r += reward;
        rewards.put(to, r);
    }

    // }}}

    // Get state, action, state visit statistics {{{

    private Integer getVisits(State from) {
        Integer visits = 0;
        for (Action action : sasv.get(from).keySet()) {
            visits += getVisits(from, action);
        }
        return visits;
    }
    private Integer getVisits(State from, Action action) {
        Integer visits = 0;
        for (Integer count : sasv.get(from).get(action).values()) {
            visits += count;
        }
        return visits;
    }
    private Integer getVisits(State from, Action action, State to) {
        return sasv.get(from).get(action).get(to);
    }

    private int getReward(State state) {
        return rewards.get(state);
    }

    // }}}

}
