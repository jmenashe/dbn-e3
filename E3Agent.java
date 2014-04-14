import org.rlcommunity.rlglue.codec.*;
import org.rlcommunity.rlglue.codec.taskspec.*;
import org.rlcommunity.rlglue.codec.taskspec.ranges.*;
import org.rlcommunity.rlglue.codec.types.*;

import java.util.ArrayList;
import java.util.List;

public class E3Agent implements AgentInterface {

    private E3 e3;

    private Observation lastState;
    private Action lastAction;

    public void agent_init(String taskSpecification) {

        TaskSpec taskspec = new TaskSpec(taskSpecification);

        IntRange actionRange = taskspec.getDiscreteActionRange(0);
        DoubleRange rewardRange = taskspec.getRewardRange();

        // Only consider one-dimensional actions
        List<Action> allActions = new ArrayList<>();
        for (int i = actionRange.getMin(); i <= actionRange.getMax(); i++) {
            Action act = new Action(1,0,0);
            act.setInt(0, i);
            allActions.add(act);
        }

        lastAction = allActions.get(0);

        e3 = new E3(
                0.95, // Discount
                0.90, // epsilon
                rewardRange.getMax(), // max reward
                allActions,
                new Observation(0, 0, 0)
                );
    }

    private void l(Object obj) {
        System.out.println(obj);
    }

    public Action agent_start(Observation state) {
        lastState = state;


        return lastAction;
    }

    public Action agent_step(double reward, Observation state) {
        e3.observe(lastState, lastAction, state, reward);

        lastState = state;
        lastAction = e3.nextAction(state);

        //l("State: " + Arrays.toString(lastState.intArray) + " Action: " + Arrays.toString(lastAction.intArray) + ", " + e3.policy + ", " + reward);

        return lastAction;
    }

    public void agent_end(double reward) {

    }

    public void agent_cleanup() {
        lastState = null;
        lastAction = null;
    }

    public String agent_message(String message) {
        return "E3 agent, win edition.";
    }

    public static void main(String[] args) {
        //AgentLoader loader = new AgentLoader(new E3Agent());
        //loader.run();
    }

}
