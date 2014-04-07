import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.rlcommunity.rlglue.codec.types.*;

/**
 * An implementation of the E3 algorithm.
 *
 * - A state is of type Observation.
 * - An action is of type Action.
 */
public class E3 {

    public double discount;

    // Transition probabilities in the MDP
    private TransitionProbabilities tps;

    // State, Action, State -> Visits
    private Map<Observation, Map<Action, Map<Observation, Integer>>>
        sasv;

    // State -> Total reward
    private Map<Observation, Integer> sr;

    // Current policy
    private Map<Observation, Action> currentPolicy;

    // All possible actions
    private List<Action> possibleActions;

    public E3(double discount, List<Action> actions) {
        tps = new TransitionProbabilities();
        sasv = new HashMap<>();
        sr = new HashMap<>();

        this.discount = discount;
        possibleActions = actions;
    }

    /**
     * Find the next action.
     */
    public Action nextAction(Observation state) {
        if (!isKnown(state)) {
            // Unknown state, resume balanced wandering
            currentPolicy = null;
            return balancedWandering(state);
        }

        if (currentPolicy == null) {
            // Start exploitation/exploration
            currentPolicy = findExplorationPolicy(state);

            if (!shouldExplore(currentPolicy, state)) {
                // Should we use an exploitation policy instead?
                currentPolicy = findExploitationPolicy(state);
            }
        }

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
        return getVisits(state) > 100;
    }

    /**
     * Should we explore given some policy?
     * 
     *
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

        long horizonTime = Math.round(1 / (1 - discount));

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

    // Policy calulations {{{

    private Map<Observation, Action> findExplorationPolicy(Observation state) {
        return null;
    }

    private Map<Observation, Action> findExploitationPolicy(Observation state) {
        return null;
    }

    // }}}

    // Functions for updating statistics {{{

    /**
     * Observe a state transition.
     */
    public void observe(
            Observation from, 
            Action action,
            Observation to,
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
    private void updateVisits(Observation from, Action action, Observation to) {
        Map<Action, Map<Observation, Integer>> asv = sasv.get(from);
        if (asv == null) {
            asv = new HashMap<>();
            sasv.put(from, asv);
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

        for (Map.Entry<Observation, Integer> sv : sasv.get(from).get(action).entrySet()) {
            tps.setTP(
                    from,
                    action,
                    sv.getKey(),
                    (double) sv.getValue() / sav);
        }
    }

    /**
     * Update reward table.
     * Reward depends only on current state.
     */
    private void updateReward(Observation from, Action action, Observation to, int reward) {
        Integer r = sr.get(to);
        if (r == null) {
            r = 0;
        }

        r += reward;
        sr.put(to, r);
    }

    // }}}

    // Functions for getting visit statistics {{{

    private Integer getVisits(Observation from) {
        Integer visits = 0;
        for (Action action : sasv.get(from).keySet()) {
            visits += getVisits(from, action);
        }
        return visits;
    }
    private Integer getVisits(Observation from, Action action) {
        Integer visits = 0;
        for (Integer count : sasv.get(from).get(action).values()) {
            visits += count;
        }
        return visits;
    }
    private Integer getVisits(Observation from, Action action, Observation to) {
        return sasv.get(from).get(action).get(to);
    }

    // }}}

}
