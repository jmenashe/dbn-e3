import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//TODO: return 0 or -inf for an unknown state when not rewarding exploration?
public class MDP {
    private boolean debug = false;
    private final double rewardMax = 100000;

    // For each state
    // For each action from that state
    // Store a list of transition probabilities
    private HashMap<MyState, HashMap<MyAction, HashSet<MyStateProbability>>> transitionProbabilities;

    private HashMap<MyState, Double> rewards;
    private HashSet<MyState> knownStates;

    /**
     * Create a new MDP
     */
    public MDP() {
        transitionProbabilities = new HashMap<>();

        rewards = new HashMap<>();
        knownStates = new HashSet<>();
    }

    /**
     * Adds a state to the set of known states.
     *
     * @param state The state to be added
     */
    public void addToKnownStates(MyState state) {

        knownStates.add(state);
    }

    /**
     * Sets the reward for a given state.
     *
     * @param state The state to set the reward for
     * @param value The reward to set for the given state
     */
    public void setReward(MyState state, double value) {
        rewards.put(state, value);
    }

    /**
     * Gets the reward stored by the MDP for a given state. This does not take
     * into consideration whether a state is known or not. If this consideration
     * should be taken - use getReward() instead.
     *
     * @param state The state for which the reward is sought
     * @return The reward for the given state
     */
    public double getActualReward(MyState state) {
        Double val = rewards.get(state);
        if (val != null) {
            return val;
        } else {
            // TODO: what should this be?
            return 0;
        }
    }

    /**
     * Returns the reward for a given state. This method considers whether a
     * state is known or not. If it is known, the recorded reward is returned,
     * otherwise the return value depends on the parameter rewardExploration.
     *
     * @param state             The state whose reward is sought
     * @param rewardExploration If this is set to true and the given state is unknown, the
     *                          maximum reward is returned; if this is false and the given
     *                          state is unknown, the minimum reward is returned
     * @return The reward associated with the given state, considering whether
     * it is known or not.
     */
    public double getReward(MyState state, boolean rewardExploration) {
        Double val = rewards.get(state);

        if (knownStates.contains(state)) {
            if(val != null) {
                return val;
            }
            throw new IllegalArgumentException("Non valid state reward pair");
        }

        if (rewardExploration) {
            return rewardMax;
        } else {
            // TODO: What should this be?
            return 0.0;
        }

    }

    /**
     * Sets the probability of going from a state, to another state when
     * performing an action.
     *
     * @param from        The state from which the action is taken
     * @param action      The action associated with the probability
     * @param to          The state to which
     * @param probability The probability of ending up in the state <code>to</code> when
     *                    taking the action <code>action</code> and starting in state
     *                    <code>
     *                    from</code>
     */
    public void setProbability(MyState from, MyAction action, MyState to,
                               double probability) {
        // get everything to do with "from"
        HashMap<MyAction, HashSet<MyStateProbability>> theMap = transitionProbabilities
                .get(from);
        if (theMap == null) {
            theMap = new HashMap<>();
            transitionProbabilities.put(from, theMap);
        }
        // get everything to do with "action" and "from"
        HashSet<MyStateProbability> theSet = theMap.get(action);
        if (theSet == null) {
            theSet = new HashSet<>();
            theMap.put(action, theSet);

        }
        // to, probability
        // TODO: dunno how to do this. This looks ugly; probably not right
        theSet.remove(new MyStateProbability(to, probability));
        theSet.add(new MyStateProbability(to, probability));
    }

    /**
     * Gets the recorded probabilities associated with all target states from a
     * given state when a given action is performed. This method does not
     * consider whether a state is known or not.
     *
     * @param from   The state from which probabilities are sought
     * @param action The action for which probabilities are sought
     * @return A list of pairs of states and probabilities signifying the
     * probability of ending up in these states when taking the given
     * action from the given state
     */
    public HashSet<MyStateProbability> getActualProbabilities(MyState from,
                                                              MyAction action) {
        HashMap<MyAction, HashSet<MyStateProbability>> theMap = transitionProbabilities
                .get(from);
        if (theMap == null) {
            return null;
        }
        return theMap.get(action);
    }

    /**
     * Gets the recorded probabilities associated with all target states from a
     * given state when a given action is performed. This method considers
     * whether a state is known or not; if it is not known, all probabilities of
     * transitioning to other states are 0, and the probability of ending up in
     * the gathering state is 1.
     *
     * @param from   The state from which probabilities are sought
     * @param action The action for which probabilities are sought
     * @return A list of pairs of states and probabilities signifying the
     * probability of ending up in these states when taking the given
     * action from the given state
     */
    public HashSet<MyStateProbability> getProbabilities(MyState from,
                                                        MyAction action) {

        if (knownStates.contains(from)) {
            return getActualProbabilities(from, action);
        } else {
            // If state unknown, create a new list with only one entry - the
            // gathering state
            MyState endState = new MyState(null, true);
            HashSet<MyStateProbability> returnList = new HashSet<>();
            returnList.add(new MyStateProbability(endState, 1));
            return returnList;
        }
    }

    /**
     * Gets all actions that have recorded transition probabilities from a given
     * state
     *
     * @param from The state from which a set of actions is sought
     * @return A set of actions with recorded transition probabilities from the
     * given state
     */
    public Set<MyAction> getActions(MyState from) {
        // if in gathering state, return an array of just one dummy action
        if (from.isLastState()) {
            HashSet<MyAction> hs = new HashSet<>();
            hs.add(new MyAction(new int[1]));
            return hs;
        }

        HashMap<MyAction, HashSet<MyStateProbability>> theMap = transitionProbabilities
                .get(from);

        if (theMap == null)
            return null;

        return theMap.keySet();
    }

    /**
     * Returns true if the given state is considered known.
     *
     * @param state The state
     * @return True if the state is known
     */
    public boolean isKnown(MyState state) {
        return knownStates.contains(state);
    }

    /**
     * Performs T-step value iteration.
     *
     * @param T                 The mixing time of the MDP
     * @param discount          The discount rate
     * @param rewardExploration If set to true, unknown states are given the maximum value,
     *                          otherwise they are given the minimum value
     * @return A list, denoting the policy found by the value iteration //TODO:
     * Maybe this should not be a list, but rather just a HashMap -
     * maybe the partial results are useless outside of this method
     */
    public ArrayList<HashMap<MyState, MyAction>> valueIterate(int T,
                                                              double discount, boolean rewardExploration) {

        // No need to store any more state values than from the preceding
        // iteration
        HashMap<MyState, Double> previousU = new HashMap<>();
        HashMap<MyState, Double> currentU = new HashMap<>();
        ArrayList<HashMap<MyState, MyAction>> returnList = new ArrayList<>();

        // Initialize all state values to 0. previousU will contain the values
        // from the previous iteration of the large loop below
        for (MyState s : knownStates) {
            previousU.put(s, 0.0);
        }

        previousU.put(new MyState(null, true), 0.0);

        // Go to T, since we're finding the T-step policy
        for (int i = 0; i < T; i++) {
            HashMap<MyState, MyAction> policy = new HashMap<>();

            // for every origin state
            for (MyState s : knownStates) {
                double reward = getReward(s, rewardExploration);
                double bestValue = Double.MAX_VALUE * -1.0;
                MyAction bestAction = null;

                // for every action from the origin
                for (MyAction a : getActions(s)) {
                    double currentValue = 0;

                    // calculate the value of the action by weighting the target
                    // states
                    // by their probabilities
                    for (MyStateProbability msp : getProbabilities(s, a)) {
                        if (knownStates.contains(msp.getState())) {
                            currentValue += msp.getValue()
                                    * previousU.get(msp.getState());
                        } else if (rewardExploration) {

                            currentValue += msp.getValue() * rewardMax * i;
                        }
                    }

                    // if this is the best action seen so far
                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestAction = a;
                    }
                }

                if (debug) {
                    System.out.println("State: "
                            + (s.getStates() == null ? "null" : s.getState(0))
                            + " value: " + bestValue);
                }
                // Store the best action found in the policy hashmap
                currentU.put(s, reward + discount * bestValue);
                policy.put(s, bestAction);
            }

            // Prepare for the next iteration
            previousU = currentU;
            currentU = new HashMap<>();

            returnList.add(policy);
        }

        return returnList;
    }

    public HashMap<MyState, HashMap<MyAction, HashSet<MyStateProbability>>> getTransitionProbabilities() {
        return transitionProbabilities;
    }
}
