import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;

import sysopenvironment.SysOpEnvironment;

public class RunAll {
    public static void main(String[] args) {
        AgentInterface theAgent = new E3Agent();
        EnvironmentInterface theEnvironment = new SysOpEnvironment();

        LocalGlue localGlueImplementation = new LocalGlue(
                theEnvironment,
                theAgent
        );

        RLGlue.setGlue(localGlueImplementation);

        SkeletonExperiment.main(args);
        System.out.println("RunAllSkeletonNoSockets Complete");
    }
}
