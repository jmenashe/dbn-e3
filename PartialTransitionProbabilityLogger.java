import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.*;

public class PartialTransitionProbabilityLogger {

    // From target state index to list of parent indices
    private Map<Integer, List<Integer>> connections;
    // From target state index to values of parents to count
    private Map<Integer, Map<ParentValuesAction, Integer>> stateActionCounts;
    private Map<Integer, Map<ParentValues, Integer>> stateCounts;
    private Map<PartialStateWithIndex, Map<ParentValues, Map<Action, Integer>>> stateActionStateCounts;
    // From target state index to set of known state/action combinations
    public Map<Integer, Map<ParentValues, List<Action>>> knownPartialStates;
    public Set<Observation> knownStates;
    private Set<Observation> observedStates;
    private AllActionsGetter allActionsGetter;
    private Map<Observation, Map<Action, Map<Observation, Double>>> probCache = new HashMap<>();


    public int knownCount = 0;

    public Set<Observation> getObservedStates() {
        return observedStates;
    }

    public Set<Observation> getKnownStates() {
        return knownStates;
    }

    // map from target state index to
    // map from parents' state to
    // map from (what action) to
    // map from (target's state) to probability
    private Map<Integer, Map<ParentValues, Map<Action, Map<Integer, Double>>>> p;
    private int knownLimit;

    public PartialTransitionProbabilityLogger(
            Map<Integer, List<Integer>> connections, List<Action> actions,
            int knownLimit, AllActionsGetter allActionsGetter) {

        p = new HashMap<>();

        this.connections = connections;
        this.allActionsGetter = allActionsGetter;

        for (int i : connections.keySet()) {
            p.put(i,
                    new HashMap<ParentValues, Map<Action, Map<Integer, Double>>>());
        }

        this.knownLimit = knownLimit;
        knownPartialStates = new HashMap<>();

        stateCounts = new HashMap<>();
        stateActionCounts = new HashMap<>();
        stateActionStateCounts = new HashMap<>();
        observedStates = new HashSet<>();
        knownStates = new HashSet<>();
    }

    private void increaseCounts(int stateIndex, int state,
                                ParentValuesAction psa) {

        increaseStateActionCount(stateIndex, psa);
        increaseStateCount(stateIndex, psa.getPs());
        increaseStateActionStateCount(stateIndex, state, psa);
    }

    private void increaseStateActionStateCount(int stateIndex, int state,
                                               ParentValuesAction psa) {
        PartialStateWithIndex pswi = new PartialStateWithIndex(state,
                stateIndex);
        Map<ParentValues, Map<Action, Integer>> theMap = stateActionStateCounts
                .get(pswi);
        if (theMap == null) {
            theMap = new HashMap<>();
            stateActionStateCounts.put(pswi, theMap);
        }

        Map<Action, Integer> theMap2 = theMap.get(psa.getPs());
        if (theMap2 == null) {
            theMap2 = new HashMap<>();
            theMap.put(psa.getPs(), theMap2);
        }

        Integer count = theMap2.get(psa.getAction());
        if (count == null) {
            count = 0;
        }
        count++;
        theMap2.put(psa.getAction(), count);

    }

    private void increaseStateCount(int stateIndex, ParentValues ps) {
        Integer count;
        Map<ParentValues, Integer> map2 = stateCounts.get(stateIndex);
        if (map2 == null) {
            map2 = new HashMap<>();
            stateCounts.put(stateIndex, map2);
        }
        count = map2.get(ps);
        if (count == null) {
            count = 0;
        }
        count++;
        map2.put(ps, count);

    }

    private void increaseStateActionCount(int stateIndex, ParentValuesAction psa) {
        Map<ParentValuesAction, Integer> map = stateActionCounts.get(stateIndex);
        if (map == null) {
            map = new HashMap<>();
            stateActionCounts.put(stateIndex, map);
        }
        Integer count = map.get(psa);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(psa, count);
        if (count == knownLimit) {
            addToKnown(stateIndex, psa.getPs(), psa.getAction());
        }
    }

    public int getStateActionCount(int stateVar, ParentValuesAction psa) {
        Map<ParentValuesAction, Integer> stateCounts = stateActionCounts
                .get(stateVar);
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

    public int getStateCount(int stateVar, ParentValues ps) {
        Map<ParentValues, Integer> counts = stateCounts.get(stateVar);
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
        for (int stateIndex = 0; stateIndex < to.intArray.length; stateIndex++) {
            int state = to.intArray[stateIndex];

            ParentValues ps = new ParentValues(from,
                    connections.get(stateIndex));

            Map<Action, Map<Integer, Double>> m = p
                    .get(stateIndex)
                    .get(ps);

            if (m == null) {
                m = new HashMap<>();
                p.get(stateIndex).put(ps, m);
            }
            Map<Integer, Double> m2 = m.get(action);
            if (m2 == null) {
                m2 = new HashMap<>();
                m.put(action, m2);
            }
            ParentValuesAction psa = new ParentValuesAction(ps, action);

            double count = getStateActionCount(stateIndex, psa);
            if (m2.get(state) == null) {
                m2.put(state, 0.0);
            }
            for (Map.Entry<Integer, Double> e : m2.entrySet()) {
                if (e.getKey() == state) {
                    m2.put(e.getKey(), (e.getValue() * count + 1) / (count + 1));
                } else {
                    m2.put(e.getKey(), (e.getValue() * count) / (count + 1));
                }
            }
            increaseCounts(stateIndex, state, psa);
            observedStates.add(to);
        }
    }

    private void addToKnown(int targetIndex, ParentValues ps, Action action) {
        Map<ParentValues, List<Action>> psMap = knownPartialStates.get(targetIndex);
        if (psMap == null) {
            psMap = new HashMap<>();
            knownPartialStates.put(targetIndex, psMap);
        }

        List<Action> actionList = psMap.get(ps);

        if (actionList == null) {
            actionList = new LinkedList<>();
            psMap.put(ps, actionList);
        }

        actionList.add(action);
        knownCount++;
    }

    public double getProbability(int stateIndex, ParentValues ps,
                                 Action action, int state) {
        Map<Action, Map<Integer, Double>> map = p.get(stateIndex).get(ps);
        if (map == null)
            return 0.0;
        Map<Integer, Double> map2 = map.get(action);
        if (map2 == null)
            return 0.0;
        Double prob = map2.get(state);
        return prob == null ? 0.0 : prob;

    }

    public boolean isKnown(Observation observedState) {
        if (knownStates.contains(observedState)) {
            return true;
        }
        for (int stateIndex = 0; stateIndex < connections.size(); stateIndex++) {

            ParentValues ps = new ParentValues(observedState,
                    connections.get(stateIndex));
            for (Action a : allActionsGetter.getAllActions(observedState)) {
                Map<ParentValues, List<Action>> map = knownPartialStates.get(stateIndex);
                if (map == null) {
                    return false;
                }
                List<Action> list = map.get(ps);
                if (list == null) {
                    return false;
                }
                if (!list.contains(a)) {
                    return false;
                }
            }
        }
        knownStates.add(observedState);
        return true;
    }

    public Map<Integer, Map<ParentValues, List<Action>>> getKnown() {
        return knownPartialStates;
    }

    private Map<Observation, Map<Action, List<Observation>>> nextStateCache = new HashMap<>();

    public List<Observation> statesFromPartialStates(Observation wholeState,
                                                     Action action) {
        if (nextStateCache.get(action) != null && nextStateCache.get(action).get(wholeState) != null) {
            return nextStateCache.get(action).get(wholeState);
        }

        List<List<Integer>> seenStates = new ArrayList<>(wholeState.intArray.length);
        for (int stateIndex = 0; stateIndex < wholeState.intArray.length; stateIndex++) {
            Map<ParentValues, Map<Action, Map<Integer, Double>>> map = p
                    .get(stateIndex);
            ParentValues ps = new ParentValues(wholeState,
                    connections.get(stateIndex));

            Map<Action, Map<Integer, Double>> map2 = map.get(ps);
            seenStates.add(new ArrayList<Integer>(map2.get(action).keySet()));
        }

        List<List<Integer>> list = nextStates(seenStates);
        List<Observation> returnList = new ArrayList<>(list.size());
        for (List<Integer> stateList : list) {
            Observation state = new Observation(stateList.size(), 0);
            for (int i = 0; i < stateList.size(); i++) {
                state.setInt(i, stateList.get(i));
            }
            returnList.add(state);
        }

        addToCache(action, wholeState, returnList);
        return returnList;
    }

    private void addToCache(Action action, Observation wholeState,
                            List<Observation> returnList) {
        Map<Action, List<Observation>> m1 = nextStateCache.get(wholeState);
        if (m1 == null) {
            m1 = new HashMap<>();
            nextStateCache.put(wholeState, m1);
        }

        m1.put(action, returnList);

    }

    private Map<List<List<Integer>>, List<List<Integer>>> nsCache = new HashMap<>();

    public List<List<Integer>> nextStates(List<List<Integer>> input) {
        List<List<Integer>> returnList = nsCache.get(input);
        if (returnList != null) {
            return returnList;
        }
        int max = 1;
        int[] sizes = new int[input.size()];
        int k = 0;
        for (List<Integer> l : input) {
            max *= l.size();
            sizes[k++] = l.size();
        }
        returnList = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            int iCpy = i;
            List<Integer> subList = new ArrayList<>(input.size());
            for (int j = 0; j < input.size(); j++) {
                subList.add(input.get(j).get(iCpy % sizes[j]));
                iCpy = iCpy / sizes[j];
            }
            returnList.add(subList);
        }
        nsCache.put(input, returnList);
        return returnList;
    }

    public int getSmallestPartialStateActionVisitCount(Action action, Observation state) {
        int smallest = Integer.MAX_VALUE;
        for (int stateIndex = 0; stateIndex < state.intArray.length; stateIndex++) {
            ParentValues ps = new ParentValues(state, connections.get(stateIndex));

            try {
                smallest = Math.min(smallest, stateActionCounts.get(stateIndex).get(
                        new ParentValuesAction(ps, action)));
            } catch (NullPointerException e) {
                return 0;
            }
        }
        return smallest;
    }

    public double getProbability(Observation currentState, Action action, Observation nextState) {

        //Try the cache
        Map<Action, Map<Observation, Double>> m = probCache.get(currentState);
        if (m != null) {
            Map<Observation, Double> m2 = m.get(action);
            if (m2 != null) {
                Double prob = m2.get(nextState);
                if (prob != null) {
                    return prob;
                }
            }
        }


        double product = 1;
        //Dummy state
        if (currentState.intArray.length == 0) {
            if (nextState.intArray.length == 0) {
                return 1.0;
            } else {
                return 0.0;
            }
        }

        for (int i = 0; i < connections.size(); i++) {
            ParentValues ps = new ParentValues(currentState, connections.get(i));
            Double d = getPartialProbability(nextState, action, i, ps);
            if (d == null) {
                return 0;
            }
            product *= d;

        }
        addToCache(currentState, action, nextState, product);
        return product;
    }

    private void addToCache(Observation currentState, Action action,
                            Observation nextState, double product) {
        Map<Action, Map<Observation, Double>> m = probCache.get(currentState);
        if (m == null) {
            m = new HashMap<>();
            probCache.put(currentState, m);
        }
        Map<Observation, Double> m2 = m.get(action);
        if (m2 == null) {
            m2 = new HashMap<>();
            m.put(action, m2);
        }
        m2.put(nextState, product);
    }

    public void clearProbabilityCache() {
        probCache.clear();
        nextStateCache.clear();
    }

    private double getPartialProbability(Observation nextState, Action action, int i, ParentValues ps) {
        Map<Action, Map<Integer, Double>> map = p.get(i).get(ps);
        if (map == null) {
            return 0.0;
        }
        Map<Integer, Double> map2 = map.get(action);
        if (map2 == null) {
            return 0.0;
        }
        Double d = map2.get(nextState.intArray[i]);
        return d == null ? 0.0 : d;

    }


}
