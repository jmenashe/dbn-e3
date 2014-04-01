import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;
import org.rlcommunity.rlglue.codec.util.AgentLoader;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

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
