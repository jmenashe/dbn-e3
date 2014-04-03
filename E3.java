import java.util.Map;
import java.util.HashMap;

import org.rlcommunity.rlglue.codec.types.*;

/**
 * An implementation of the E3 algorithm.
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

    public E3() {
        tps = new TransitionProbabilities();
        sasv = new HashMap<>();
        sr = new HashMap<>();
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

            if (shouldExploit(currentPolicy, state)) {
                currentPolicy = findExploitationPolicy(state);
            }
        }

        return currentPolicy.get(state);
    }

    /**
     * Find the balanced wandering action
     */
    private Action balancedWandering(Observation state) {
        return null;
    }

    /**
     * Is a state known?
     * TODO: this :)
     */
    private boolean isKnown(Observation state) {
        return getVisits(state) > 100;
    }

    /**
     * Should we exploit? Or is exploration beneficial?
     * TODO: this :)
     */
    private boolean shouldExploit(Map<Observation, Action> policy, Observation state) {
        return false;
    }

    // Functions for policy calculations {{{

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
