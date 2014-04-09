import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * Models transition probabilities.
 * State -> Action -> State -> Double
 */
public class TransitionProbabilities<State, Action> {
    private Map<State, Map<Action, Map<State, Double>>> tp;

    public TransitionProbabilities() {
        tp = new HashMap<>();
    }

    public Set<State> getStates() {
        return tp.keySet();
    }

    /**
     * Transition probabilities from a state
     */
    public Map<Action, Map<State, Double>> from(State state) {
        Map<Action, Map<State, Double>> actionStateProb = tp.get(state);

        if (actionStateProb == null) {
            actionStateProb = new HashMap<>();
            tp.put(state, actionStateProb);
        }

        return actionStateProb;
    }

    /**
     * Transition probabilities from a state and action
     */
    public Map<State, Double> from(State state, Action action) {
        Map<Action, Map<State, Double>> actionStateProb = from(state);

        Map<State, Double> stateProb = actionStateProb.get(action);
        
        if (stateProb == null) {
            stateProb = new HashMap<>();
            actionStateProb.put(action, stateProb);
        }

        return stateProb;
    }

    /**
     * All actions taken from a state
     */
    public Set<Action> actionsFromState(State state) {
        return from(state).keySet();
    }

    /**
     * Set probability
     */
    public void setTP(State fromState, Action action, State toState, Double prob) {
        from(fromState, action).put(toState, prob);
    }
}
