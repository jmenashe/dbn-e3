import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class E3Agent implements AgentInterface {

    public static final int NBR_REACHES = 4;
    public static final int HABITATS_PER_REACHES = 3;

    private E3DBN e3;

    private List<PartialState> lastState;
    private Action lastAction;
    private int stepCount = 1;

    private double getMaxReward() {
    	return 11.6*NBR_REACHES + 0.9*NBR_REACHES*HABITATS_PER_REACHES;
    }

    public void agent_init(String taskSpecification) {

        TaskSpec taskspec = new TaskSpec(taskSpecification);

        //List<Action> allActions = createAllActionsList(taskspec);

        //DoubleRange rewardRange = taskspec.getRewardRange();
        
        e3 = new E3DBN(0.95, // Discount
                0.90, // epsilon
                getMaxReward(), // max reward
                taskspec);

    }


    private void l(Object obj) {
        System.out.println(obj);
    }

    public Action agent_start(Observation state) {
        List<PartialState> stateList = Reach.allReaches(state, NBR_REACHES,
                HABITATS_PER_REACHES);

        lastState = stateList;

        lastAction = new Action(NBR_REACHES, 0, 0);
        for (int i = 0; i < NBR_REACHES; i++) {
            lastAction.intArray[i] = 1;
        }

        return lastAction;
    }


    public Action agent_step(double reward, Observation state) {
        reward += getMaxReward();
    	List<PartialState> stateList = Reach.allReaches(state, NBR_REACHES,
                HABITATS_PER_REACHES);

        l(stepCount++ + ") STEP State: " +
                lastState +
                " Action: " +
                Arrays.toString(lastAction.intArray) + ", " +
                e3.policy + ", " +
                reward + ", " +
                e3.chanceToExplore + ", " +
                //e3.balancingCount + ", " +
                e3.getKnownPartialsCount() + ", " + 
                e3.getKnownFullCount());

        e3.observe(lastState, lastAction, stateList, reward);
        lastAction = e3.nextAction(stateList);
        lastState = stateList;

        return lastAction;
    }

    public void agent_end(double reward) {
    }

    public void agent_cleanup() {
        lastState = null;
        lastAction = null;
    }

    public String agent_message(String message) {
        if (message.equals("freeze learning")) {
            e3.freezePolicy();
        } else if (message.equals("unfreeze learning")) {
            e3.unFreezePolicy();
        }
        return "E3(dbn) agent, win edition.";

    }

    public static void main(String[] args) {
        AgentLoader loader = new AgentLoader(new E3Agent());
        loader.run();
    }

}