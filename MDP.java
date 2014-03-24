import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

class MDP {
  private final double rewardMax = 100000;

  //For each state
  //For each action from that state
  //Store a list of transition probabilities
  HashMap<
    MyState, 
    HashMap<
      MyAction, 
      ArrayList<MyStateProbability>>> transitionProbabilities;

  HashMap<MyState, Double> rewards;
  HashSet<MyState> knownStates;

  public MDP() {
    transitionProbabilities = 
      new HashMap<MyState, HashMap<MyAction, ArrayList<MyStateProbability>>>();

    rewards = new HashMap<MyState, Double>();
    knownStates = new HashSet<MyState>();
  }


  public void addToKnownStates(MyState state) {
    assert(transitionProbabilities.get(state) != null);

    knownStates.add(state);
  }

  public void setReward(MyState state, double value) {
    rewards.put(state,  value);
  }

  //This could be used when updating a reward - should not be used in
  //value iteration 
  public double getActualReward(MyState state) {
    Double val = rewards.get(state);
    if (val != null) {
      return rewards.get(state);
    } else { 

      return 0;
    }
  }

  //This should be used in value iteration - not when updating a reward
  public double getReward(MyState state, boolean rewardExploration) {
    Double val = rewards.get(state);

    if (knownStates.contains(state)) {
      assert(val != null);
      return val;
    }

    if (rewardExploration) {
      return rewardMax;
    } else {
      return 0.0;
    }

  }

  public void setProbability(MyState from, MyAction action, MyState to, double probability) {
    HashMap<MyAction, ArrayList<MyStateProbability>> theMap = 
      transitionProbabilities.get(from);
    if (theMap == null) {
      theMap = new HashMap<MyAction, ArrayList<MyStateProbability>>();
      transitionProbabilities.put(from, theMap);
    }
    ArrayList<MyStateProbability> theList = theMap.get(action);
    if (theList == null) {
      theList = new ArrayList<MyStateProbability>();
      theMap.put(action, theList);
    }
    theList.add(new MyStateProbability(to, probability));
  }

  public ArrayList<MyStateProbability> getActualProbabilities(MyState from, MyAction action) {
    HashMap<MyAction, ArrayList<MyStateProbability>> theMap = 
      transitionProbabilities.get(from);
    if (theMap == null) {
      return null;
    }
    return theMap.get(action);
  }

  public ArrayList<MyStateProbability> getProbabilities(MyState from, MyAction action) {

    if (knownStates.contains(from)) {
      return getActualProbabilities(from, action);
    } else {
      MyState endState = new MyState(null, true);
      ArrayList<MyStateProbability> returnList = new ArrayList<MyStateProbability>();
      returnList.add(new MyStateProbability(endState, 1));
      return returnList;
    }
  }

  public Set<MyAction> getActions(MyState from) {
    if (from.isLastState) {
      HashSet<MyAction> hs = new HashSet<MyAction>();
      hs.add(new MyAction(new int[1]));
      return hs;
    }

    HashMap<MyAction, ArrayList<MyStateProbability>> theMap =
      transitionProbabilities.get(from);

    if (theMap == null)
      return null;

    return theMap.keySet();
  }

  public boolean isKnown(MyState state) {
    return knownStates.contains(state); 
  }

  public ArrayList<HashMap<MyState, MyAction>> exploitationPolicy() {
    return null;
  }
  public ArrayList<HashMap<MyState, MyAction>> explorationPolicy() {
    return null;
  }

  public ArrayList<HashMap<MyState, MyAction>> policyIterate(
      int T,
      double discount,
      boolean rewardExploration, 
      MyState startingState) {

      HashMap<MyState, Double> PreviousU = new HashMap<MyState, Double>(); 
      HashMap<MyState, Double> CurrentU = new HashMap<MyState, Double>(); 
      ArrayList<HashMap<MyState, MyAction>> returnList = 
        new ArrayList<HashMap<MyState,MyAction>>();

      // Initialize all state values to 0 PreviousU contains the values
      // from the previous iteration of the large loop below
      for (MyState s : knownStates) {
        PreviousU.put(s, 0.0);
      }

      PreviousU.put(new MyState(null, true), 0.0);

      HashSet<MyState> knownAndS0 = (HashSet<MyState>) knownStates.clone();
      knownAndS0.add(new MyState(null, true));

      for (int i = 0; i < T; i++) {
        HashMap<MyState, MyAction> policy = new HashMap<MyState, MyAction>();

        for (MyState s : knownAndS0) {
          double reward = getReward(s, rewardExploration);
          double bestValue = Double.MAX_VALUE * -1.0;
          MyAction bestAction = null;

          for (MyAction a : getActions(s)) {
            double currentValue = 0;

            for (MyStateProbability msp : getProbabilities(s, a)) {
              if (knownStates.contains(msp.state)) {
                currentValue += msp.value * PreviousU.get(msp.state);
              } else if (rewardExploration) {
                // a bit hacky; T - i - 1 is the number of remaining 
                //time steps up to T
                currentValue += rewardMax * (T - i - 1);
              }
            }

            if (currentValue > bestValue) {
              bestValue = currentValue;
              bestAction = a;
            }
          }

          CurrentU.put(s,reward + discount * bestValue);
          policy.put(s,bestAction);
        }

        PreviousU = CurrentU;
        CurrentU = new HashMap<MyState, Double>();
        returnList.add(policy);
      }

      return returnList;
    }
}
