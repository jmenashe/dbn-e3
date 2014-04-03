import java.util.Map;
import java.util.HashMap;

import org.rlcommunity.rlglue.codec.types.*;

/**
 * An implementation of the E3 algorithm.
 */
public class E3 {

    // Transition probabilities in the MDP
    private TransitionProbabilities tps;

    // State, Action, State -> Visits
    private Map<Observation, Map<Action, Map<Observation, Integer>>>
        sasv;

    // State -> Total reward
    private Map<Observation, Integer> sr;

    public E3() {
        tps = new TransitionProbabilities();
        sasv = new HashMap<>();
        sr = new HashMap<>();
    }

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

}
