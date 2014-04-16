/*
 * Copyright 2008 Brian Tanner
 * http://rl-glue-ext.googlecode.com/
 * brian@tannerpages.com
 * http://brian.tannerpages.com
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 *  $Revision: 676 $
 *  $Date: 2009-02-08 18:15:04 -0700 (Sun, 08 Feb 2009) $
 *  $Author: brian@tannerpages.com $
 *  $HeadURL: http://rl-glue-ext.googlecode.com/svn/trunk/projects/codecs/Java/examples/skeleton-sample/SkeletonEnvironment.java $
 * 
 */

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 *
 */
public class SysOpEnvironment implements EnvironmentInterface {

    private Random rand = new Random();

    private ArrayList<SysOpCpu> cpus;
    private ArrayList<SysOpCpu> nextCpuList;

    private HashMap<Integer, SysOpCpu> cpuMap;
    private HashMap<Integer, SysOpCpu> nextCpuMap;

    private double runningReward = 1.0;

    public final double oddsDownAffectsUp = 0.5;
    public final double oddsStayUp = 0.95;
    public final double oddsComeUp = 0.05;

    public SysOpEnvironment() {

        cpus = new ArrayList<>(5);

        SysOpCpu centerNode = new SysOpCpu(true, null, 4);
        SysOpCpu bottomNode = new SysOpCpu(true, null, 1);
        bottomNode.addToLinked(centerNode);
        SysOpCpu leftNode = new SysOpCpu(true, null, 2);
        leftNode.addToLinked(centerNode);
        SysOpCpu rightNode = new SysOpCpu(true, null, 3);
        rightNode.addToLinked(centerNode);
        SysOpCpu topNode = new SysOpCpu(true, null, 0);
        topNode.addToLinked(centerNode);
        centerNode.addToLinked(topNode);
        centerNode.addToLinked(leftNode);
        centerNode.addToLinked(rightNode);
        centerNode.addToLinked(bottomNode);
        cpus.add(centerNode);
        cpus.add(leftNode);
        cpus.add(rightNode);
        cpus.add(topNode);
        cpus.add(bottomNode);

        cpuMap = new HashMap<>();

        for (SysOpCpu c : cpus) {
            cpuMap.put(c.getId(), c);
        }

        // for nextCpuList TODO: fix a method for copying the above
        nextCpuList = new ArrayList<>();
        SysOpCpu centerNode2 = new SysOpCpu(true, null, 4);
        SysOpCpu bottomNode2 = new SysOpCpu(true, null, 1);
        bottomNode2.addToLinked(centerNode2);
        SysOpCpu leftNode2 = new SysOpCpu(true, null, 2);
        leftNode2.addToLinked(centerNode2);
        SysOpCpu rightNode2 = new SysOpCpu(true, null, 3);
        rightNode2.addToLinked(centerNode2);
        SysOpCpu topNode2 = new SysOpCpu(true, null, 0);
        topNode2.addToLinked(centerNode2);
        centerNode2.addToLinked(topNode2);
        centerNode2.addToLinked(leftNode2);
        centerNode2.addToLinked(rightNode2);
        centerNode2.addToLinked(bottomNode2);
        nextCpuList.add(centerNode);
        nextCpuList.add(leftNode);
        nextCpuList.add(rightNode);
        nextCpuList.add(topNode);
        nextCpuList.add(bottomNode);

        nextCpuMap = new HashMap<>();

        for (SysOpCpu c : nextCpuList) {
            nextCpuMap.put(c.getId(), c);
        }

    }

    public String env_init() {

        // Create a task spec programatically. This task spec encodes that
        // state, action, and reward space for the problem.
        // You could forgo the task spec if your agent and environment have been
        // created specifically to work with each other
        // ie, there is no need to share this information at run time. You could
        // also use your own ad-hoc task specification language,
        // or use the official one but just hard code the string instead of
        // constructing it this way.
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setContinuing();
        theTaskSpecObject.setDiscountFactor(1.0d);
        // Specify that there will be couple of boolean observations for the
        // cpus.
        // 1 = the corresponding cpu is running
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));
        // Specify that there will be an integer action [0,4]
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 4));
        // Specify the reward range [-1,1]
        theTaskSpecObject.setRewardRange(new DoubleRange(-1, 5));

        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);

        // This actual string this makes is:
        // VERSION RL-Glue-3.0 PROBLEMTYPE episodic DISCOUNTFACTOR 1.0
        // OBSERVATIONS INTS (1 0 20) ACTIONS INTS (1 0 1) REWARDS (1 -1.0 1.0)
        // EXTRA

        // This could be simplified a bit if you made it manually to
        // VERSION RL-Glue-3.0 PROBLEMTYPE episodic DISCOUNTFACTOR 1.0
        // OBSERVATIONS INTS (0 20) ACTIONS INTS (0 1) REWARDS (-1.0 1.0) EXTRA
        return taskSpecString;
    }

    public Observation env_start() {

        Observation returnObservation = new Observation(5, 0, 0);
        for (int i = 0; i < 5; i++) {
            returnObservation.intArray[i] = 1;
        }

        return returnObservation;
    }

    public Reward_observation_terminal env_step(Action thisAction) {
        boolean episodeOver = false;
        double probRunning;

        int rebooted = thisAction.intArray[0];

        // Calculate next state ("cpus" contains the previous state,
        // "nextCpuList"
        // contains the new state)
        for (SysOpCpu c : nextCpuList) {
            if (c.getId() == rebooted) {
                probRunning = 1.0;
            } else {
                SysOpCpu cOld = cpuMap.get(c.getId());
                // Previous state of the cpu affects its current status
                if (cOld.isRunning()) {
                    probRunning = oddsStayUp;
                } else {
                    probRunning = oddsComeUp;
                }
                // All cpus that are down can affect neighboring cpus to come
                // down
                for (SysOpCpu connected : c.getLinked()) {
                    SysOpCpu connectedOld = cpuMap.get(connected.getId());
                    if (!connectedOld.isRunning()) {
                        probRunning *= oddsDownAffectsUp;
                    }
                }
            }
            c.setRunningStatus(rand.nextDouble() < probRunning);
        }

        // Switch old and new cpu lists and maps
        HashMap<Integer, SysOpCpu> temp = cpuMap;
        cpuMap = nextCpuMap;
        nextCpuMap = temp;

        ArrayList<SysOpCpu> temp2 = cpus;
        cpus = nextCpuList;
        nextCpuList = temp2;

        // Calculate reward, build observation
        Observation obs = new Observation(cpus.size(), 0);
        double reward = 0;
        for (SysOpCpu c : cpus) {
            boolean running = c.isRunning();
            obs.intArray[c.getId()] = running ? 1 : 0;
            if (running) {
                reward += runningReward;
            }
        }

        return new Reward_observation_terminal(reward, obs, episodeOver);
    }

    public void env_cleanup() {
    }

    private String getConnectionsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cpus.size(); i++) {
            SysOpCpu cpu = cpuMap.get(i);
            sb.append(cpu.getId()).append(" ");

            for (SysOpCpu connectedCpu : cpu.getLinked()) {
                sb.append(connectedCpu.getId()).append(" ");
            }
            //remove last space
            sb.deleteCharAt(sb.length() - 1);
            sb.append(":");
        }
        // remove last colon
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public String env_message(String message) {
        if (message.equals("what is your name?"))
            return "my name is SysOp Environment, Java edition!";
        if (message.equals("tell me your connections"))
            return getConnectionsString();
        return "I don't know how to respond to your message";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Environment contains:\n");
        for (SysOpCpu cpu : cpus) {
            sb.append("\t").append(cpu.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * This is a trick we can use to make the agent easily loadable.
     */
    public static void main(String[] args) {
        EnvironmentLoader theLoader = new EnvironmentLoader(
                new SysOpEnvironment());
        theLoader.run();
    }

}
