import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;

public class E3Agent implements AgentInterface {
  private boolean debug = false;
  private HashMap<MyState, HashMap<MyAction,Integer>> StateActionVisitCounts;
  //hackish
  private MyAction[] allActions;

  private double discountFactor = 1.0;
  private int exploreCount = 0;
  private int exploitCount = 0;

  private ArrayList<HashMap<MyState, MyAction>> explorePolicy;
  private ArrayList<HashMap<MyState, MyAction>> exploitPolicy;

  private int mixingTime = 1;

  private Random randGenerator = new Random();
  private Action lastAction;
  private Observation lastObservation;
  private MDP mdp;

  int obsRangeMin;
  int obsRangeMax;
  int actRangeMin;
  int actRangeMax;

  public void agent_init(String taskSpecification) {
    TaskSpec theTaskSpec = new TaskSpec(taskSpecification);

    System.out.println("Skeleton agent parsed the task spec.");
    System.out.println("Observation have "+theTaskSpec.getNumDiscreteObsDims()+" integer dimensions");
    System.out.println("Actions have "+theTaskSpec.getNumDiscreteActionDims()+" integer dimensions");

    IntRange theObsRange = theTaskSpec.getDiscreteObservationRange(0);

    System.out.println("Observation (state) range is: "+theObsRange.getMin()+" to "+theObsRange.getMax());

    IntRange theActRange = theTaskSpec.getDiscreteActionRange(0);
    System.out.println("Action range is: "+theActRange.getMin()+" to "+theActRange.getMax());
    DoubleRange theRewardRange = theTaskSpec.getRewardRange();
    System.out.println("Reward range is: "+theRewardRange.getMin()+" to "+theRewardRange.getMax());

    actRangeMax = theActRange.getMax();
    actRangeMin = theActRange.getMin();
    obsRangeMax = theObsRange.getMax();
    obsRangeMin = theObsRange.getMin();

    mdp = new MDP();

    allActions = new MyAction[4];
    for(int i = 0; i < 4; i++) {
      int[] temp = new int[1];
      temp[0] = i;
      allActions[i] = new MyAction(temp);
    }
  }

  private MyAction[] getAllActions(MyState currentState) {
    return allActions;
  }


  private MyAction balancedWandering(MyState currentState) {
    MyAction leastTriedAction = null;
    int leastTriedCount = Integer.MAX_VALUE;
    MyAction[] actions = getAllActions(currentState);
    HashMap<MyAction, Integer> theMap = StateActionVisitCounts.get(currentState);

    if (theMap == null) {
      return actions[0];
    }

    for (MyAction a : actions) {
      Integer currentCount = theMap.get(a);
      if (currentCount == null) {
        return a;
      } else if (currentCount < leastTriedCount) {
        leastTriedCount = currentCount;
        leastTriedAction = a;
      }
    }

    return leastTriedAction;
  }

  private void logThis(MyState from, MyAction action, MyState to, double reward) {
    HashMap<MyAction, Integer> theMap = StateActionVisitCounts.get(from);
  }

  private double chanceToExplore(
      ArrayList<HashMap<MyState, MyAction>> policy, 
      MyState startingState
  ) {
    HashMap<MyState, Double> probabilities = new HashMap<MyState, Double>();
    HashMap<MyState, Double> newProbabilities = new HashMap<MyState, Double>();

    for (MyState state : mdp.transitionProbabilities.keySet()) {
      if (!mdp.isKnown(state)) {
        probabilities.put(state, 1.0);
      } else {
        probabilities.put(state, 0.0);
      }
    }

    for (int i = 0; i < mixingTime; i++) {
      for (MyState from : mdp.transitionProbabilities.keySet()) {
        if (!mdp.isKnown(from)) {
          newProbabilities.put(from, 1.0);
        } else {
          double newProb = 0;
          //TODO: null pointers? (Should not happen!)
          for (MyStateProbability msp : 
              mdp.getActualProbabilities(from, policy.get(i).get(from))) {
            newProb += probabilities.get(msp.state) * msp.value;
          }
          newProbabilities.put(from,newProb);
        }
      }

      for (MyState state : newProbabilities.keySet()) {
        probabilities.put(state, newProbabilities.get(state));
      }
    }

    return probabilities.get(startingState);
  }

  public MyAction findAction(MyState currentState) {
    MyAction theAction = null;

    // Balanced wandering if unknown state!
    if (!mdp.isKnown(currentState)) {
      theAction = balancedWandering(currentState);

    } else if (exploreCount == 0 && exploitCount == 0) {
      ArrayList<HashMap<MyState, MyAction>> exploitPolicy = 
        mdp.policyIterate(mixingTime, discountFactor, false, currentState);
      ArrayList<HashMap<MyState, MyAction>> explorePolicy = 
        mdp.policyIterate(mixingTime, discountFactor, true, currentState);

      //if exploration seems beneficial
      double chanceToExplore = chanceToExplore(explorePolicy, currentState);
      System.out.println("Chance to explore: " + chanceToExplore);
      if (true) {
        System.out.println("Starting exploration");
      } else {
        // if exploitation seems beneficial
        System.out.println("Starting expliotation");
      }    		
    } else {
      if (exploreCount > 0) {
        // Already exploring
      } else {
        // Already exploiting
      }

    }

    return theAction;
  }

  public Action agent_start(Observation observation) {
    return null;
  }

  public Action agent_step(double reward, Observation observation) {
    return null;
  }

  public void agent_end(double reward) {

  }

  public void agent_cleanup() {
    lastAction=null;
    lastObservation=null;

  }

  public String agent_message(String message) {
    if(message.equals("what is your name?"))
      return "my name is E3 agent, Java edition!";

    return "I don't know how to respond to your message";
  }

  /**
   * This is a trick we can use to make the agent easily loadable.
   * @param args
   */

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

    mdp.setProbability(state1, action1, state2, 0.4);
    mdp.setProbability(state1, action1, state1, 0.6);
    mdp.setProbability(state1, action2, state1, 0.3);
    mdp.setProbability(state1, action2, state2, 0.7);
    mdp.setProbability(state2, action1, state1, 0.6);
    mdp.setProbability(state2, action1, state3, 0.4);
    mdp.setProbability(state2, action2, state1, 0);
    mdp.setProbability(state2, action2, state3, 1);
    mdp.setProbability(state3, action1, state4, 0.6);
    mdp.setProbability(state3, action1, state2, 0.4);
    mdp.setProbability(state3, action2, state2, 0.6);
    mdp.setProbability(state3, action2, state4, 0.4);
    mdp.setProbability(state4, action1, state4, 0.6);
    mdp.setProbability(state4, action1, state3, 0.4);
    mdp.setProbability(state4, action2, state3, 0.6);
    mdp.setProbability(state4, action2, state4, 0.4);
    mdp.setReward(state1, 1.0);
    mdp.setReward(state2, 2.0);
    mdp.setReward(state3, 3.0);
    mdp.setReward(state4, 4.0);
    //mdp.addToKnownStates(state1);
    mdp.addToKnownStates(state2);
    mdp.addToKnownStates(state3);
    mdp.addToKnownStates(state4);
    ArrayList<HashMap<MyState,MyAction>> policy = 
      mdp.policyIterate(3, 1, true, state4);
    int i = 0;
    for(HashMap<MyState,MyAction> hm : policy) {
      System.out.println("i: " + i++);
      for(Entry<MyState,MyAction> entry : hm.entrySet()) {
        System.out.println("\tstate: " + (entry.getKey().state == null ? "null" : entry.getKey().state[0]) + " action : " + 
            (entry.getValue().action == null ? "null" : entry.getValue().action[0]));
      }
    }
    this.mdp = mdp;
    mixingTime = 2;
    findAction(state1);
  }

  public static void main(String[] args){
    //AgentLoader theLoader=new AgentLoader(new E3Agent());
    //theLoader.run();

    new E3Agent().test();
  }

}
