import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.*;


public class PartialTransitionProbabilityLogger {

    //From target state index to list of parent indices
    private Map<Integer, List<Integer>> connections;
    //From target state index to values of parents to count
    private Map<Integer, Map<PartialStateAction, Integer>> stateActionCounts;
    private Map<Integer, Map<PartialState, Integer>> stateCounts;
    //From target state index to set of known state/action combinations
    private Map<Integer, Set<PartialState>> known;
    //map from target state index to
    //map from values of parents to
    //map from (what action) to
    //map from (value of state) to probability
    private Map<Integer, Map<PartialState, Map<Action,
            Map<Integer, Double>>>> p;
    private int knownLimit;

    public PartialTransitionProbabilityLogger(
            Map<Integer, List<Integer>> connections,
            List<Action> actions,
            int knownLimit) {
        p = new HashMap<>();
        this.connections = connections;
        for (int i : connections.keySet()) {
            p.put(i, new HashMap<>());
        }
        this.knownLimit = knownLimit;
        known = new HashMap<>();
        stateActionCounts = new HashMap<>();
    }

    private void increaseCounts(int stateVar, PartialStateAction psa) {

        increaseStateActionCount(stateVar, psa);
        increaseStateCount(stateVar, psa.getPs());
    }

    private void increaseStateCount(int stateVar, PartialState ps) {
        Integer count;
        Map<PartialState, Integer> map2 = stateCounts.get(stateVar);
        if (map2 == null) {
            map2 = new HashMap<>();
            stateCounts.put(stateVar, map2);
        }
        count = map2.get(ps);
        if (count == null) {
            count = 0;
        }
        count++;
        map2.put(ps, count);
        if (count == knownLimit) {
            addToKnown(stateVar, ps);
        }
    }

    private void increaseStateActionCount(int stateVar, PartialStateAction psa) {
        Map<PartialStateAction, Integer> map = stateActionCounts.get(stateVar);
        if (map == null) {
            map = new HashMap<>();
            stateActionCounts.put(stateVar, map);
        }
        Integer count = map.get(psa);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(psa, count);
    }

    public int getStateActionCount(int stateVar, PartialStateAction psa) {
        Map<PartialStateAction, Integer> stateCounts = stateActionCounts.get(stateVar);
        if (stateCounts == null) {
            return 0;
        }
        Integer count = stateCounts.get(psa);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    public int getStateCount(int stateVar, PartialState ps) {
        Map<PartialState, Integer> counts = stateCounts.get(stateVar);
        if (counts == null) {
            return 0;
        }
        Integer count = counts.get(ps);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    public void record(Observation from, Action action, Observation to) {
        for (int i = 0; i < to.intArray.length; i++) {
            int stateVal = to.intArray[i];
            PartialState ps = new PartialState(from, connections.get(i));
            Map<Action, Map<Integer, Double>> m = p.get(i).get(ps);
            if (m == null) {
                m = new HashMap<>();
                p.get(i).put(ps, m);
            }
            Map<Integer, Double> m2 = m.get(action);
            if (m2 == null) {
                m2 = new HashMap<>();
                m.put(action, m2);
            }
            PartialStateAction psa = new PartialStateAction(ps, action);

            double count = getStateActionCount(i, psa);
            if (m2.get(stateVal) == null) {
                m2.put(stateVal, 0.0);
            }
            for (Map.Entry<Integer, Double> e : m2.entrySet()) {
                if (e.getKey() == stateVal) {
                    m2.put(e.getKey(), (e.getValue() * count + 1) / (count + 1));
                } else {
                    m2.put(e.getKey(), (e.getValue() * count) / (count + 1));
                }
            }
            increaseCounts(i, psa);
        }
    }

    private void addToKnown(int i, PartialState ps) {
        Set<PartialState> psSet = known.get(i);
        if (psSet == null) {
            psSet = new HashSet<>();
            known.put(i, psSet);
        }
        psSet.add(ps);
    }

    public double getProbability(int stateVar, PartialState ps, Action action, int stateVal) {
        Map<Action, Map<Integer, Double>> map = p.get(stateVar).get(ps);
        if (map == null)
            return 0.0;
        Map<Integer, Double> map2 = map.get(action);
        if (map2 == null)
            return 0.0;
        Double prob = map2.get(stateVal);
        return prob == null ? 0.0 : prob;

    }

    public boolean isKnown(Observation state) {
        for (int stateVar = 0; stateVar < connections.size(); stateVar++) {

            PartialState ps = new PartialState(state, connections.get(stateVar));
            if (!known.get(stateVar).contains(ps)) {
                return false;
            }
        }

        return true;
    }


    public Map<Observation, Double> getTransitionProbs(
            Observation state,
            Action action) {

        return null;
    }

    public Map<Integer, Set<PartialState>> getKnown() {
        return known;
    }

    public Iterator<Observation> knownStateIterator() {
        return null;
    }

    public static void main(String[] args) {
        ArrayList<Integer> apa = new ArrayList<>();
        ArrayList<Integer> bepa = new ArrayList<>();
        ArrayList<Integer> cepa = new ArrayList<>();

        apa.add(0);
        apa.add(1);

        bepa.add(1);
        bepa.add(2);

        cepa.add(2);
        cepa.add(0);

        HashMap<Integer, List<Integer>> map = new HashMap<>();
        map.put(0, apa);
        map.put(1, bepa);
        map.put(2, cepa);

        ArrayList<Action> actions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Action a = new Action(1, 0);
            a.intArray[0] = i;
            actions.add(a);
        }

        PartialTransitionProbabilityLogger ptpl =
                new PartialTransitionProbabilityLogger(map, actions, 3);
        //observation from
        Observation from = new Observation(3, 0);
        from.intArray[0] = 0;
        from.intArray[1] = 1;
        from.intArray[2] = 1;

        //action
        Action action = new Action(1, 0);
        action.intArray[0] = 1;

        //observation to
        Observation to = new Observation(3, 0);
        to.intArray[0] = 1;
        to.intArray[1] = 1;
        to.intArray[2] = 0;
        ptpl.record(from, action, to);
        to.intArray[0] = 0;
        to.intArray[1] = 0;
        to.intArray[2] = 1;
        ptpl.record(from, action, to);
        ptpl.record(from, action, to);


        PartialState ps = new PartialState(from, map.get(0));
        PartialStateAction psa = new PartialStateAction(ps, action);

        System.out.println("ptpl: " + ptpl.getStateActionCount(0, psa));
        System.out.println("probability: " + ptpl.getProbability(0, ps, action, 0));
    }
}
