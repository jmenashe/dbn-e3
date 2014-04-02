import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

public class E3Agent implements AgentInterface {
    private boolean debug = false;
    private HashMap<MyStateAction, HashMap<MyState, Integer>> stateActionStateVisitCounts;
    private HashMap<MyState, HashMap<MyAction, Integer>> stateActionVisitCounts;
    private HashMap<MyState, Integer> stateVisitCounts;

    // hackish
    private MyAction[] allActions;

    private double discountFactor = 1.0;
    private int exploreCount = 0;
    private int exploitCount = 0;

    private ArrayList<HashMap<MyState, MyAction>> explorePolicy;
    private ArrayList<HashMap<MyState, MyAction>> exploitPolicy;

    final private int horizonTime = 20;

    // visit count until a state is known
    final private int knownStateLimit = 100;
    final private double explorationChanceLimit = 0.05;

    private Random randGenerator = new Random();
    private Action lastAction;
    private Observation lastObservation;
    private MDP mdp;

    private MyState previousState;
    private MyAction previousAction;

    private void log(Object o) {
        System.err.println(o);
    }

    public void agent_init(String taskSpecification) {
        TaskSpec theTaskSpec = new TaskSpec(taskSpecification);

        log("Skeleton agent parsed the task spec.");
        log("Observations have "
                + theTaskSpec.getNumDiscreteObsDims() + " integer dimensions");
        log("Actions have "
                + theTaskSpec.getNumDiscreteActionDims()
                + " integer dimensions");

        IntRange theObsRange = theTaskSpec.getDiscreteObservationRange(0);

        log("Observation (state) range is: "
                + theObsRange.getMin() + " to " + theObsRange.getMax());

        IntRange theActRange = theTaskSpec.getDiscreteActionRange(0);
        log("Action range is: " + theActRange.getMin() + " to "
                + theActRange.getMax());
        DoubleRange theRewardRange = theTaskSpec.getRewardRange();
        log("Reward range is: " + theRewardRange.getMin()
                + " to " + theRewardRange.getMax());

        mdp = new MDP();

        // Only consider one-dimensional actions
        allActions = new MyAction[theActRange.getMax() - theActRange.getMin()
                + 1];
        for (int i = theActRange.getMin(); i < theActRange.getMax() + 1; i++) {
            int[] temp = new int[1];
            temp[0] = i;
            allActions[i] = new MyAction(temp);
        }
    }

    /**
     * This should return the available actions from the given state.
     *
     * @param currentState The state from which the set of actions are sought
     * @return The possible actioins from currentState
     */
    private MyAction[] getAllActions(MyState currentState) {
        // TODO: This
        return allActions;
    }

    /**
     * Finds the so far least taken action from a given state.
     *
     * @param currentState The state from which the action is to be taken
     * @return The least taken action from currentState
     */
    private MyAction balancedWandering(MyState currentState) {
        MyAction leastTriedAction = null;
        int leastTriedCount = Integer.MAX_VALUE;
        MyAction[] actions = getAllActions(currentState);
        HashMap<MyAction, Integer> theMap = stateActionVisitCounts
                .get(currentState);
        // if no action has been taken from this state before
        if (theMap == null) {
            log("balancedWandering: no action taken from state "
                    + currentState.getState(0));

            return actions[0];
        }

        for (MyAction a : actions) {
            Integer currentCount = theMap.get(a);
            log("balancedWandering investigating: " + a
                    + " count: " + currentCount + " from " + currentState);
            // if this action has not been taken before, try it now
            if (currentCount == null) {
                log("have not tried action " + a.getAction(0));
                return a;
            } else if (currentCount < leastTriedCount) {
                leastTriedCount = currentCount;
                leastTriedAction = a;
            }
        }

        return leastTriedAction;
    }

    // TODO: This
    private int findStateKnownLimit() {
        return knownStateLimit;
    }

    // fairly untested
    private void logThis(MyState from, MyAction action, MyState to,
                         double reward) {
        // Increase state visit count
        Integer stateVisitCount = stateVisitCounts.get(to);
        stateVisitCount = stateVisitCount == null ? 1 : stateVisitCount + 1;
        stateVisitCounts.put(to, stateVisitCount);
        if (stateVisitCount > findStateKnownLimit()) {
            mdp.addToKnownStates(to);
        }

        // Increase state-action visit count
        HashMap<MyAction, Integer> theMap = stateActionVisitCounts.get(from);
        if (theMap == null) {
            theMap = new HashMap<>();
            stateActionVisitCounts.put(from, theMap);
        }
        Integer stateActionCount = theMap.get(action);
        stateActionCount = stateActionCount == null ? 1 : stateActionCount + 1;
        theMap.put(action, stateActionCount);

        // Increase state-action-state visit count

        MyStateAction sa = new MyStateAction(from, action);
        HashMap<MyState, Integer> theOtherMap = stateActionStateVisitCounts
                .get(sa);

        if (theOtherMap == null) {
            theOtherMap = new HashMap<>();
            stateActionStateVisitCounts.put(sa, theOtherMap);
        }
        Integer stateActionStatecount = theOtherMap.get(to);
        theOtherMap.put(to, stateActionStatecount == null ? 1
                : stateActionStatecount + 1);
        // Update all probabilities for (from,action)
        for (Entry<MyState, Integer> e : theOtherMap.entrySet()) {
            // System.out.println("Updating from: " + from.state[0] +
            // " action: " + action.action[0] +
            // " to: " + e.getKey().state[0] + " value: "
            // +((double)e.getValue()) / stateActionCount);
            mdp.setProbability(from, action, e.getKey(),
                    ((double) e.getValue()) / stateActionCount);
        }

        // Update reward

        Double oldReward = mdp.getActualReward(to);
        oldReward = oldReward == null ? 0 : oldReward;
        mdp.setReward(to, (oldReward * (stateVisitCount - 1) + reward)
                / stateVisitCount);

    }

    /**
     * Finds the probability that a certain policy will put the agent in an
     * unknown state, when starting in a certain state, within the horizon time
     * of the MDP
     *
     * @param policy        The policy to execute
     * @param startingState The state to start from
     * @return The probability of ending up in an unknown state
     */
    @SuppressWarnings("unchecked")
    private double chanceToExplore(
            ArrayList<HashMap<MyState, MyAction>> policy, MyState startingState) {
        HashMap<MyState, Double> probabilities = new HashMap<>();
        HashMap<MyState, Double> newProbabilities = new HashMap<>();

        // The starting probabilities are 1 for unknown states and 0 for known
        // states
        for (MyState state : mdp.getTransitionProbabilities().keySet()) {
            if (!mdp.isKnown(state)) {
                probabilities.put(state, 1.0);
            } else {
                probabilities.put(state, 0.0);
            }
        }

        // Only look ahead one horizon time (be careful when considering the
        // direction
        // the index i changes - it may not seem intuitive to start at 0 and end
        // at
        // the horizon time.)
        for (int i = 0; i < horizonTime; i++) {
            for (MyState from : mdp.getTransitionProbabilities().keySet()) {
                // if this is an unknown state, the probability of reaching an unknown
                // state is 1
                if (!mdp.isKnown(from)) {
                    newProbabilities.put(from, 1.0);
                }
                // otherwise calculate the probability of ending up in a known
                // state, using dynamic programming (memoization)
                else {
                    double newProb = 0;
                    // TODO: null pointers? (Should not happen!)
                    if (debug) {
                        log("inner loop, state: "
                                + from.getState(0) + " action: "
                                + policy.get(i).get(from).getAction(0));
                    }
                    // For all possible transitions, multiply transition 
                    //probability by the stored value for the target state. 
                    for (MyStateProbability msp : mdp.getActualProbabilities(
                            from, policy.get(policy.size() - 1).get(from))) {
                        newProb += probabilities.get(msp.getState()) * msp.getValue();
                        if (debug) {
                            log("dp prob: "
                                    + probabilities.get(msp.getState())
                                    + " transition prob " + msp.getValue());
                        }
                    }
                    newProbabilities.put(from, newProb);
                }
            }

            // Set up for next iteration
            probabilities = (HashMap<MyState, Double>) newProbabilities.clone();

            if (debug) {
                log(i);
                for (Entry<MyState, Double> e : probabilities.entrySet()) {
                    log("State: " + e.getKey().getState(0)
                            + " value: " + e.getValue());
                }
                //TODO Adam, you forgot something?
                //log();
            }
        }

        return probabilities.get(startingState);
    }

    /**
     * This should return the limit for the probability of finding an unknown
     * state within the horizon time, above which it is preferable to explore
     * rather than exploit.
     *
     * @return A limit for the probability when it is more preferable to explore
     * rather than exploit.
     */
    public double getExplorationChanceLimit() {

        // TODO: This should be a formula. Look it up!
        return explorationChanceLimit;
    }

    /**
     * Finds the action to take from the given state.
     *
     * @param currentState The state from which an action is to be taken
     * @return The action to take
     */
    public MyAction findAction(MyState currentState) {
        // Balanced wandering if unknown state!
        if (!mdp.isKnown(currentState)) {
            log("findAction: starting balanced wandering");
            exploreCount = 0;
            exploitCount = 0;
            return balancedWandering(currentState);
        }

        // If known state and not currently exploring or exploiting
        if (exploreCount == 0 && exploitCount == 0) {
            exploitPolicy = mdp.valueIterate(horizonTime, discountFactor, false);
            explorePolicy = mdp.valueIterate(horizonTime, discountFactor, true);

            // if exploration seems beneficial
            double chanceToExplore = chanceToExplore(explorePolicy,
                    currentState);

            log("Chance to explore: " + chanceToExplore);

            if (chanceToExplore > getExplorationChanceLimit()) {
                log("Starting exploration");
                exploreCount = horizonTime;
            } else {
                // if exploitation seems beneficial
                log("Starting exploitation");
                exploitCount = horizonTime;
            }
        }

        // If we are currently exploring, or have just decided to start
        // exploring
        if (exploreCount > 0) {
            exploreCount--;
            log("exploring");

            return explorePolicy.get(explorePolicy.size() - 1)
                    .get(currentState);
        }

        // If we are currently exploiting, or have just decided to start
        // exploiting
        if (exploitCount > 0) {
            exploitCount--;
            log("exploiting");

            return exploitPolicy.get(exploitPolicy.size() - 1)
                    .get(currentState);
        }

        // at this point, we should have checked all possibilities - balanced
        // wandering, exploration, and exploitation.
        return null;
    }

    private void initvars() {
        mdp = new MDP();
        stateActionStateVisitCounts = new HashMap<>();
        stateActionVisitCounts = new HashMap<>();
        stateVisitCounts = new HashMap<>();
    }

    public Action agent_start(Observation observation) {
        initvars();
        MyState startingState = new MyState(observation.intArray, false);
        stateVisitCounts.put(startingState, 1);
        MyAction action = findAction(startingState);
        Action returnAction = new Action(action.getActions().length, 0);
        returnAction.intArray = action.getActions();
        previousAction = action;
        previousState = startingState;
        return returnAction;
    }

    public Action agent_step(double reward, Observation observation) {
        MyState currentState = new MyState(observation.intArray, false);

        log("logging:  previousState : " + previousState
                + " previousAction: " + previousAction + " currentState: "
                + currentState + " reward:" + reward);

        MyAction newAction = findAction(currentState);

        // Log the necessary statistics
        logThis(previousState, previousAction, currentState, reward);

        previousAction = newAction;
        previousState = currentState;

        if (previousAction == null) {
            // This should never happen
            int a = 2 / 0;
        }

        Action returnAction = new Action(previousAction.getActions().length, 0);
        returnAction.intArray = previousAction.getActions();
        return returnAction;
    }

    public void agent_end(double reward) {

    }

    public void agent_cleanup() {
        lastAction = null;
        lastObservation = null;

    }

    public String agent_message(String message) {
        if (message.equals("what is your name?"))
            return "my name is E3 agent, Java edition!";

        return "I don't know how to respond to your message";
    }

    public void test() {
        MDP mdp = new MDP();
        int[] apa = {1};
        int[] bepa = {2};
        int[] cepa = {3};
        int[] depa = {4};

        int[] foo = {1};
        int[] bar = {2};
        MyState state1 = new MyState(apa, false);
        MyState state2 = new MyState(bepa, false);
        MyState state3 = new MyState(cepa, false);
        MyState state4 = new MyState(depa, false);

        MyAction action1 = new MyAction(foo);
        MyAction action2 = new MyAction(bar);

        /*
         * mdp.setProbability(state1, action1, state2, 0.4);
         * mdp.setProbability(state1, action1, state1, 0.6);
         * mdp.setProbability(state1, action2, state1, 0.3);
         * mdp.setProbability(state1, action2, state2, 0.7);
         * mdp.setProbability(state2, action1, state1, 0.6);
         * mdp.setProbability(state2, action1, state3, 0.4);
         * mdp.setProbability(state2, action2, state1, 0);
         * mdp.setProbability(state2, action2, state3, 1);
         * mdp.setProbability(state3, action1, state4, 0.6);
         * mdp.setProbability(state3, action1, state2, 0.4);
         * mdp.setProbability(state3, action2, state2, 0.6);
         * mdp.setProbability(state3, action2, state4, 0.4);
         * mdp.setProbability(state4, action1, state4, 0.6);
         * mdp.setProbability(state4, action1, state3, 0.4);
         * mdp.setProbability(state4, action2, state3, 0.6);
         * mdp.setProbability(state4, action2, state4, 0.4);
         */

        mdp.setProbability(state1, action1, state1, 1);
        mdp.setProbability(state1, action2, state2, 1);
        mdp.setProbability(state2, action1, state1, 1);
        mdp.setProbability(state2, action2, state3, 1);
        mdp.setProbability(state3, action1, state2, 1);
        mdp.setProbability(state3, action2, state4, 1);
        mdp.setProbability(state4, action1, state3, 1);
        mdp.setProbability(state4, action2, state4, 1);

        mdp.setReward(state1, 1.0);
        mdp.setReward(state2, 2.0);
        mdp.setReward(state3, 7.0);
        mdp.setReward(state4, 4.0);
        mdp.addToKnownStates(state1);
        mdp.addToKnownStates(state2);
        mdp.addToKnownStates(state3);
        mdp.addToKnownStates(state4);
        ArrayList<HashMap<MyState, MyAction>> policy = mdp.valueIterate(6, 1,
                false);
        int i = 0;
        for (HashMap<MyState, MyAction> hm : policy) {
            log("i: " + i++);
            for (Entry<MyState, MyAction> entry : hm.entrySet()) {
                log("\tstate: "
                        + (entry.getKey().getStates() == null ? "null" : entry
                        .getKey().getState(0))
                        + " action : "
                        + (entry.getValue().getActions() == null ? "null" : entry
                        .getValue().getAction(0)));
            }
        }
        this.mdp = mdp;

        // horizonTime = 3;

        // findAction(state2);
    }

    public static void main(String[] args) {
        // AgentLoader theLoader=new AgentLoader(new E3Agent());
        // theLoader.run();

        new E3Agent().test();
    }

}
