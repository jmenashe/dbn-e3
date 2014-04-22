import org.rlcommunity.rlglue.codec.*;
import org.rlcommunity.rlglue.codec.taskspec.*;
import org.rlcommunity.rlglue.codec.taskspec.ranges.*;
import org.rlcommunity.rlglue.codec.types.*;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class E3Agent implements AgentInterface {

    private E3DBN e3;
    
    private Observation lastState;
    private Action lastAction;
    private int stepCount = 1;
    
    public void agent_init(String taskSpecification) {

        TaskSpec taskspec = new TaskSpec(taskSpecification);

        List<Action> allActions = createAllActionsList(taskspec);

        DoubleRange rewardRange = taskspec.getRewardRange();
        
    	e3 = new E3DBN(0.95, // Discount
            0.90, // epsilon
            rewardRange.getMax(), // max reward
            allActions,
            taskspec,
            new Observation(0, 0, 0));
            
    }


	private List<Action> createAllActionsList(TaskSpec taskspec) {
		int actionDims = taskspec.getNumDiscreteActionDims();
        IntRange[] actionRanges = new IntRange[actionDims];
        int actionCount = 1;
        for(int i = 0; i < actionDims; i++) {
        	actionRanges[i] = taskspec.getDiscreteActionRange(i);
        	actionCount *= actionRanges[i].getMax() - actionRanges[i].getMin() + 1;
        }
        List<Action> allActions = new ArrayList<>(actionCount);
        for(int i = 0; i < actionCount; i++) {
        	Action action = new Action(actionDims, 0, 0);
        	allActions.add(action);
        }
        
        int n = actionCount;
        int m;
        for(int i = 0; i < actionRanges.length; i++) {
        	int howMany = (actionRanges[i].getMax() - actionRanges[i].getMin() + 1);
        	n = n / howMany;
        	m = howMany;
	        for(int action = 0; action < actionCount; action++) {
	        	allActions.get(action).intArray[i] =
	        			actionRanges[i].getMin() + (action / n) % m; 
	        }
        }
		return allActions;
	}




    private void l(Object obj) {
        System.out.println(obj);
    }

    public Action agent_start(Observation state) {
        lastState = state;

        lastAction = e3.nextAction(state);
        return lastAction;
    }
    
        
    public Action agent_step(double reward, Observation state) {
    	e3.observe(lastState, lastAction, state, reward);
		lastAction = e3.nextAction(state);
		
		if (stepCount == 1200)
			throw new ArithmeticException();
		lastState = state;
		l(stepCount++ +") State: " + 
		Arrays.toString(lastState.intArray) + 
		" Action: " +
		Arrays.toString(lastAction
				.intArray) + ", " + 
		e3.policy + ", " +
		reward + ", "); //+ 
		//e3.chanceToExplore + ", " +
		//e3.balancingCount + ", " +
		//e3.getKnownCount());

		
		return lastAction;
    }

    public void agent_end(double reward) {
    }

    public void agent_cleanup() {
        lastState = null;
        lastAction = null;
    }

    public String agent_message(String message) {
        return "E3(dbn) agent, win edition.";
    }

    public static void main(String[] args) {
         AgentLoader loader = new AgentLoader(new E3Agent());
         loader.run();
    }

}
