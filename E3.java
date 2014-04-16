import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.rlcommunity.rlglue.codec.types.*;

/**
 * An implementation of the E3 algorithm in a discounted MDP.
 */
public class E3 {

    public double discount;
    private double eps;
    private long horizonTime;
    private double maxReward;

    // Transition probabilities in the MDP
    private TransitionProbabilities<Observation, Action> tps;

    // State, Action, State -> Visits
    private Map<Observation, Map<Action, Map<Observation, Integer>>>
            stateActionStateVisits;
    private Map<Observation, Map<Action, Integer>> stateActionVisits;
    private Map<Observation, Integer> stateVisits;

    private PartialTransitionProbabilityLogger ptpl;
    
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
    public E3(
            double discount,
            double eps,
            double maxReward,
            List<Action> actions,
            PartialTransitionProbabilityLogger ptpl,
            Observation dummyState) {
        tps = new TransitionProbabilities<>();

        stateActionStateVisits = new HashMap<>();
        stateActionVisits = new HashMap<>();
        stateVisits = new HashMap<>();
        

        rewards = new HashMap<>();

        // This is the absorbing state which represents all unvisited states
        this.dummyState = dummyState;

        this.maxReward = maxReward;

        this.discount = discount;
        this.eps = eps;
        this.ptpl = ptpl;
        horizonTime = Math.round((1 / (1 - discount)));
        possibleActions = actions;
    }

    private void l(Object obj) {
        System.out.println(obj);
    }


    // Picking the next action {{{

    public String policy = "";

    /**
     * Find the next action.
     */
    public Action nextAction(Observation state) {

        // Unknown state or expl* long enough: balanced wandering
        if (!isKnown(state) || currentTime >= horizonTime) {
            policy = "balancing";
            currentPolicy = null;
            currentTime = 0;
            return balancedWandering(state);
        }

        // Start exploitation/exploration
        if (currentPolicy == null) {
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
    private List<Action> getPossibleActions(Observation state) {
        return possibleActions;
    }

    /**
     * Is a state known?
     *
     * TODO: This :). See E3 paper.
     */
    private boolean isKnown(Observation state) {
        return getVisits(state) > 10;
    }

    /**
     * Should we explore given some policy?
     */
    private boolean shouldExplore(Map<Observation, Action> explorationPolicy, Observation currentState) {
        Map<Observation, Double> probs = new HashMap<>();

        // Starting probabilities are 0 for known states and 1 for unknown
        for (Observation state : tps.getStates()) {
            if (isKnown(state)) {
                probs.put(state, 0.0);
            } else {
                probs.put(state, 1.0);
            }
        }

        // Find probability that we end up in a unknown state in horizonTime steps
        for (long i = 0; i < horizonTime; i++) {
            for (Observation state : tps.getStates()) {
                if (isKnown(state)) {
                    double probability = 0;

                    // For all possible transitions, multiply transition
                    // probability by the stored value for the target state. 
                    for (Map.Entry<Observation, Double> sp : tps.from(state, explorationPolicy.get(state)).entrySet()) {
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
    private Map<Observation, Action> findExplorationPolicy(Observation currentState) {

        // Value function
        Map<Observation, Double> vf = new HashMap<>();

        // Policy
        Map<Observation, Action> policy = new HashMap<>();

        // Initialize value function
        for (Observation state : tps.getStates()) {
            if (isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        vf.put(dummyState, 0.0);
        setReward(dummyState, maxReward);

        for (long t = 0; t < horizonTime; t++) {
            for (Observation state : vf.keySet()) {
                double bestValue = 0;
                Action bestAction = null;

                for (Action action : getPossibleActions(state)) {
                    double currentValue = 0;

                    for (Map.Entry<Observation, Double> sp : tps.from(state, action).entrySet()) {
                        if (isKnown(sp.getKey())) {
                            currentValue += sp.getValue() * vf.get(sp.getKey());
                        } else {
                            currentValue += sp.getValue() * vf.get(dummyState) * t;
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
        for (Observation state : tps.getStates()) {
            if (isKnown(state)) {
                vf.put(state, 0.0);
            }
        }
        vf.put(dummyState, 0.0);
        setReward(dummyState, 0.0);

        for (long t = 0; t < horizonTime; t++) {
            for (Observation state : vf.keySet()) {
                double bestValue = 0;
                Action bestAction = null;

                for (Action action : getPossibleActions(state)) {
                    double currentValue = 0;

                    for (Map.Entry<Observation, Double> sp : tps.from(state, action).entrySet()) {
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
            Observation from, 
            Action action,
            Observation to,
            double reward
    ) {
        // Log the visit
        updateVisits(from, action, to);

        // Update transition probabilities
        updateTP(from, action, to);

        // Update reward table
        updateReward(to, reward);
    }

    /**
     * Update visits statistics
     */
    private void updateVisits(Observation from, Action action, Observation to) {
        // State visits
        stateVisits.put(from, stateVisits.containsKey(from) ? stateVisits.get(from) + 1 : 1);

        // State X Action visits
        Map<Action, Integer> av = stateActionVisits.get(from);
        if (av == null) {
            av = new HashMap<>();
            stateActionVisits.put(from, av);
        }
        av.put(action, av.containsKey(action) ? av.get(action) + 1 : 1);

        // State X Action X State visits
        Map<Action, Map<Observation, Integer>> asv = stateActionStateVisits.get(from);
        if (asv == null) {
            asv = new HashMap<>();
            stateActionStateVisits.put(from, asv);
        }

        Map<Observation, Integer> sv = asv.get(action);
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
    private void updateTP(Observation from, Action action, Observation to) {
        int sav = getVisits(from, action);

        for (Map.Entry<Observation, Integer> sv : stateActionStateVisits.get(from).get(action).entrySet()) {
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
    private int getVisits(Observation from, Action action) {
        try {
            return stateActionVisits.get(from).get(action);
        } catch (NullPointerException e) {}

        return 0;
    }
    private int getVisits(Observation from, Action action, Observation to) {
        try {
            return stateActionStateVisits.get(from).get(action).get(to);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    private double getReward(Observation state) {
        return rewards.get(state);
    }

    // }}}

}
