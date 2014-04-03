import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import org.rlcommunity.rlglue.codec.types.*;

/**
 * Models transition probabilities.
 * State -> Action -> State -> Double
 */
public class TransitionProbabilities {
    private Map<Observation, Map<Action, Map<Observation, Double>>> tp;

    public TransitionProbabilities() {
        tp = new HashMap<>();
    }

    /**
     * Transition probabilities from a state
     */
    public Map<Action, Map<Observation, Double>> from(Observation state) {
        Map<Action, Map<Observation, Double>> actionStateProb = tp.get(state);

        if (actionStateProb == null) {
            actionStateProb = new HashMap<>();
            tp.put(state, actionStateProb);
        }

        return actionStateProb;
    }

    /**
     * Transition probabilities from a state and action
     */
    public Map<Observation, Double> from(Observation state, Action action) {
        Map<Action, Map<Observation, Double>> actionStateProb = from(state);

        Map<Observation, Double> stateProb = actionStateProb.get(action);
        
        if (stateProb == null) {
            stateProb = new HashMap<>();
            actionStateProb.put(action, stateProb);
        }

        return stateProb;
    }

    /**
     * All actions taken from a state
     */
    public Set<Action> actionsFromState(Observation state) {
        return from(state).keySet();
    }

    /**
     * Set probability
     */
    public void setTP(Observation fromState, Action action, Observation toState, Double prob) {
        from(fromState, action).put(toState, prob);
    }
}
