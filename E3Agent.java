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
	final private class MyState {
		public int[] state;
		public boolean isLastState;
		
		public MyState(int[] theState, boolean isLastState) {
			this.isLastState = isLastState;
			if (theState == null) return;
			state = new int[theState.length];
			for(int i = 0; i < theState.length; i++) {
				state[i] = theState[i];
			}
		}
		
		@Override
		public int hashCode() {
			if (isLastState)
				return 0;
			int hash = 1;
			for (int i = 0; i < state.length; i++) {
				hash = hash * 17 + state[i];
			}
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof MyState)) { 
				return false;
			}
			
			MyState other = (MyState)obj;
			
			if (other.isLastState && isLastState)
				return true;
			
			if (other.state.length != state.length) {
				return false;
			}
			
			for(int i = 0; i < state.length; i++) {
				if (other.state[i] != state[i]) {
					return false;
				}
			}
			return true;
		}
	}
	
	private final class MyAction {
		private int[] action;
		
		public MyAction(int[] theAction) {
			action = new int[theAction.length];
			for(int i = 0; i < theAction.length; i++) {
				action[i] = theAction[i];
			}
		}
		
		@Override
		public int hashCode() {
			int hash = 1;
			for (int i = 0; i < action.length; i++) {
				hash = hash * 17 + action[i];
			}
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof MyAction)) { 
				return false;
			}
			
			MyAction other = (MyAction)obj;
			
			if (other.action.length != action.length) {
				return false;
			}
			
			for(int i = 0; i < action.length; i++) {
				if (other.action[i] != action[i]) {
					return false;
				}
			}
			return true;
		}		
	}
	
	private class MyStateProbability {
		public MyState state;
		public double value;
		
		public MyStateProbability(MyState state, double value) {
			this.state = state;
			this.value = value;
		}
	}
	
	private class MDP {
		private final double rewardMax = 100000;
		
		//For each state
		//For each action from that state
		//Store a list of transition probabilities
		private HashMap<MyState, HashMap<MyAction,ArrayList<MyStateProbability>>> transitionProbabilities;
		private HashMap<MyState, Double> rewards;
		private HashSet<MyState> knownStates;
		
		public MDP() {
			transitionProbabilities = new HashMap<MyState, HashMap<MyAction, ArrayList<MyStateProbability>>>();
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
		
		public ArrayList<HashMap<MyState, MyAction>> 
				policyIterate(int T, double discount, boolean rewardExploration, 
						MyState startingState) {
			HashMap<MyState, Double> PreviousU = new HashMap<MyState, Double>(); 
			HashMap<MyState, Double> CurrentU = new HashMap<MyState, Double>(); 
			ArrayList<HashMap<MyState, MyAction>> returnList = 
					new ArrayList<HashMap<MyState,MyAction>>();
			
			//Initialize all state values to 0 PreviousU contains the values
			//from the previous iteration of the large loop below
			for (MyState s : knownStates) {
				PreviousU.put(s, 0.0);
			}
			PreviousU.put(new MyState(null, true), 0.0);
			
			HashSet<MyState> knownAndS0 = (HashSet<MyState>) knownStates.clone();
			knownAndS0.add(new MyState(null, true));
			
			for(int i = 0; i < T; i++) {
				HashMap<MyState, MyAction> policy = new HashMap<MyState, MyAction>();
				for (MyState s : knownAndS0) {
					double reward = getReward(s, rewardExploration);
					double bestValue = Double.MAX_VALUE * -1.0;
					MyAction bestAction = null;
					for(MyAction a : getActions(s)) {
						double currentValue = 0;
						for(MyStateProbability msp : getProbabilities(s, a)) {
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
    	TaskSpec theTaskSpec=new TaskSpec(taskSpecification);
		System.out.println("Skeleton agent parsed the task spec.");
		System.out.println("Observation have "+theTaskSpec.getNumDiscreteObsDims()+" integer dimensions");
		System.out.println("Actions have "+theTaskSpec.getNumDiscreteActionDims()+" integer dimensions");
		IntRange theObsRange=theTaskSpec.getDiscreteObservationRange(0);
		System.out.println("Observation (state) range is: "+theObsRange.getMin()+" to "+theObsRange.getMax());
		IntRange theActRange=theTaskSpec.getDiscreteActionRange(0);
		System.out.println("Action range is: "+theActRange.getMin()+" to "+theActRange.getMax());
		DoubleRange theRewardRange=theTaskSpec.getRewardRange();
		System.out.println("Reward range is: "+theRewardRange.getMin()+" to "+theRewardRange.getMax());
		
		actRangeMax = theActRange.getMax();
		actRangeMin= theActRange.getMin();
		obsRangeMax = theObsRange.getMax();
		obsRangeMin= theObsRange.getMin();
		mdp = new MDP();
		
		allActions = new MyAction[4];
		for(int i = 0; i < 4; i++) {
			int[] temp = new int[1];
			temp[0] = i;
			allActions[i] = new MyAction(temp);
		}
		//In more complex agents, you would also check for continuous observations and actions, discount factors, etc.
		//Also, these ranges can have special values like "NEGINF, POSINF, UNSPEC (unspecified)".  There is no guarantee
		//that they are all specified and that they are all nice numbers.
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
    	for(MyAction a : actions) {
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
    
    private double chanceToExplore(ArrayList<HashMap<MyState, MyAction>> policy, 
    		MyState startingState) {
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
	    			for(MyStateProbability msp : 
	    				mdp.getActualProbabilities(from, policy.get(i).get(from))) {
	    				newProb += probabilities.get(msp.state) * msp.value;
	    			}
	    			newProbabilities.put(from,newProb);
	    		}
	    	}
	    	for(MyState state : newProbabilities.keySet()) {
	    		probabilities.put(state, newProbabilities.get(state));
	    	}
    	}
    	
    	return probabilities.get(startingState);
    }
    
    public MyAction findAction(MyState currentState) {
    	MyAction theAction = null;
    	//if unknown state, balanced wandering
    	if (! mdp.isKnown(currentState)) {
    		theAction = balancedWandering(currentState);
    	} else if (exploreCount == 0 && exploitCount == 0) {
    		ArrayList<HashMap<MyState, MyAction>>  exploitPolicy = 
    				mdp.policyIterate(mixingTime, discountFactor, false, currentState);
    		ArrayList<HashMap<MyState, MyAction>>  explorePolicy = 
    				mdp.policyIterate(mixingTime, discountFactor, true, currentState);
    		//if exploration seems beneficial
    		double chanceToExplore = chanceToExplore(explorePolicy, currentState);
    		System.out.println("Chance to explore: " + chanceToExplore);
    		if (true) {
    			System.out.println("Starting exploration");
    		} 
    		//if exploitation seems beneficial
    		else {
    			System.out.println("Starting expliotation");
    		}    		
    	} else {
    		//Already exploring
    		if (exploreCount > 0) {
    			
    		} //Already exploiting
    		else {
    			
    		}
    		
    	}
    	//else if exploration beneficial
    		//attempted exploration
    	//else
    		//attempted exploitation
    	return theAction;
    }
    
    public Action agent_start(Observation observation) {
        /**
         * Choose a random action (0 or 1)
         */
        int theIntAction = randGenerator.nextInt(4);
        /**
         * Create a structure to hold 1 integer action
         * and set the value
         */
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = theIntAction;

        lastAction = returnAction.duplicate();
        lastObservation = observation.duplicate();

        return returnAction;
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
