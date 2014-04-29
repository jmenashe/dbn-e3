import org.rlcommunity.rlglue.codec.types.Action;

import java.util.*;

public class PartialTransitionProbabilityLogger {

	// From target state index to list of parent indices
	private Map<Integer, List<Integer>> connections;
	// From target state index to values of parents to count
	private Map<Integer, Map<ParentValuesAction, Integer>> stateActionCounts;

	// Target State Index -> ParentValues -> "PartialAction" -> Count
	private Map<Integer, Map<ParentValues, Map<Integer, Integer>>> parentSettingPartialActionCounts;
	// private Map<Integer, Map<ParentValues, Integer>> stateCounts;
	// private Map<PartialState, Map<ParentValues, Map<Action, Integer>>>
	// stateActionStateCounts;
	// From target state index to set of known state/action combinations
	public Map<Integer, Map<ParentValues, List<Integer>>> knownPartialStates;
	public Set<List<PartialState>> knownStates;
	private Set<List<PartialState>> observedStates;
	private AllActionsGetter allActionsGetter;

	public int knownCount = 0;

	public Set<List<PartialState>> getObservedStates() {
		return observedStates;
	}

    // map from target state index to
	// map from parents' state to
	// map from partial action to
	// map from partial target state
	// to probability
	private Map<Integer, Map<ParentValues, Map<Integer, Map<PartialState, Double>>>> p;
	private int knownLimit;

	public PartialTransitionProbabilityLogger(
			Map<Integer, List<Integer>> connections, int knownLimit,
			AllActionsGetter allActionsGetter) {

		p = new HashMap<>();

		this.connections = connections;
		this.allActionsGetter = allActionsGetter;

		for (int i : connections.keySet()) {
			p.put(i,
					new HashMap<ParentValues, Map<Integer, Map<PartialState, Double>>>());
		}

		this.knownLimit = knownLimit;
		knownPartialStates = new HashMap<>();

		/* stateCounts = new HashMap<>(); */
		stateActionCounts = new HashMap<>();
		parentSettingPartialActionCounts = new HashMap<>();
		// stateActionStateCounts = new HashMap<>();
		observedStates = new HashSet<>();
		knownStates = new HashSet<>();
	}

	private void increaseCounts(int targetStateIndex, ParentValuesAction pva) {

		increaseStateActionCount(targetStateIndex, pva);
		increaseParentSettingPartialActionCounts(targetStateIndex, pva);
		// increaseStateCount(pva.getPv());
		// increaseStateActionStateCount(ps, pva);
	}

	private void increaseParentSettingPartialActionCounts(int targetStateIndex,
			ParentValuesAction pva) {
        int index = connections.get(targetStateIndex).size();
		Map<ParentValues, Map<Integer, Integer>> map1 = parentSettingPartialActionCounts
				.get(index);
		if (map1 == null) {
			map1 = new HashMap<>();
			parentSettingPartialActionCounts.put(index, map1);
		}
		Map<Integer, Integer> map2 = map1.get(pva.getPv());
		if (map2 == null) {
			map2 = new HashMap<>();
			map1.put(pva.getPv(), map2);
		}
		int partialAction = pva.getAction().intArray[targetStateIndex];
		Integer count = map2.get(partialAction);
		count = count == null ? 1 : count + 1;
		map2.put(partialAction, count);
	}

	public int getParentSettingPartialActionCount(int targetStateIndex,
			ParentValues pv, int partialAction) {
		try {
			return parentSettingPartialActionCounts.get(connections.get(targetStateIndex).size())
					.get(pv).get(partialAction);
		} catch (NullPointerException e) {
			return 0;
		}
	}

	private void increaseStateActionCount(int stateIndex, ParentValuesAction psa) {
		int index = connections.get(stateIndex).size();
        Map<ParentValuesAction, Integer> map = stateActionCounts
				.get(index);
		if (map == null) {
			map = new HashMap<>();
			stateActionCounts.put(index, map);
		}
		Integer count = map.get(psa);
		if (count == null) {
			count = 0;
		}
		count++;
		map.put(psa, count);
		if (count == knownLimit) {
			addToKnown(stateIndex, psa.getPv(), psa.getAction());
		}
	}

	public void record(List<PartialState> from, Action action,
			List<PartialState> to) {
		for (int stateIndex = 0; stateIndex < to.size(); stateIndex++) {
			PartialState state = to.get(stateIndex);

			ParentValues pv = new ParentValues(from,
					connections.get(stateIndex));

			Map<Integer, Map<PartialState, Double>> m = p.get(stateIndex).get(
					pv);

			if (m == null) {
				m = new HashMap<>();
				p.get(stateIndex).put(pv, m);
			}
			Map<PartialState, Double> m2 = m.get(action.getInt(stateIndex));
			if (m2 == null) {
				m2 = new HashMap<>();
				m.put(action.getInt(stateIndex), m2);
			}
			ParentValuesAction psa = new ParentValuesAction(pv, action);

			double count = getParentSettingPartialActionCount(stateIndex, pv,
					action.getInt(stateIndex));
			if (m2.get(state) == null) {
				m2.put(state, 0.0);
			}
			for (Map.Entry<PartialState, Double> e : m2.entrySet()) {
				if (e.getKey().equals(state)) {
					m2.put(e.getKey(), (e.getValue() * count + 1) / (count + 1));
				} else {
					m2.put(e.getKey(), (e.getValue() * count) / (count + 1));
				}
			}
			increaseCounts(stateIndex, psa);
		}
		observedStates.add(to);
	}

	private void addToKnown(int targetIndex, ParentValues ps, Action action) {
        int index = connections.get(targetIndex).size();
        Map<ParentValues, List<Integer>> psMap = knownPartialStates
				.get(index);
		if (psMap == null) {
			psMap = new HashMap<>();
			knownPartialStates.put(index, psMap);
		}

		List<Integer> actionList = psMap.get(ps);

		if (actionList == null) {
			actionList = new LinkedList<>();
			psMap.put(ps, actionList);
		}

		actionList.add(action.intArray[targetIndex]);
		knownCount++;
	}

    public boolean isKnown(List<PartialState> observedState) {
		if (knownStates.contains(observedState)) {
			return true;
		}
		for (int stateIndex = 0; stateIndex < connections.size(); stateIndex++) {

			ParentValues ps = new ParentValues(observedState,
					connections.get(stateIndex));
			for (Action a : allActionsGetter.getAllActions(observedState)) {
				Map<ParentValues, List<Integer>> map = knownPartialStates
						.get(connections.get(stateIndex).size());
				if (map == null) {
					return false;
				}
				List<Integer> list = map.get(ps);
				if (list == null) {
					return false;
				}
				if (!list.contains(a.intArray[stateIndex])) {
					return false;
				}
			}
		}
		knownStates.add(observedState);
		return true;
	}

    public List<List<PartialState>> statesFromPartialStates(
			List<PartialState> wholeState, Action action) {

		List<List<PartialState>> seenStates = new ArrayList<>(wholeState.size());
		for (int stateIndex = 0; stateIndex < wholeState.size(); stateIndex++) {
			Map<ParentValues, Map<Integer, Map<PartialState, Double>>> map = p
					.get(stateIndex);
			ParentValues ps = new ParentValues(wholeState,
					connections.get(stateIndex));

			Map<Integer, Map<PartialState, Double>> map2 = map.get(ps);
			seenStates.add(new ArrayList<>(map2.get(action.getInt(stateIndex))
					.keySet()));
		}

		return nextStates(seenStates);

	}

	public List<List<PartialState>> nextStates(List<List<PartialState>> input) {
		List<List<PartialState>> returnList;

		int max = 1;
		int[] sizes = new int[input.size()];
		int k = 0;
		for (List<PartialState> l : input) {
			max *= l.size();
			sizes[k++] = l.size();
		}
		returnList = new ArrayList<>(max);
		for (int i = 0; i < max; i++) {
			int iCpy = i;
			List<PartialState> subList = new ArrayList<>(input.size());
			for (int j = 0; j < input.size(); j++) {
				subList.add(input.get(j).get(iCpy % sizes[j]));
				iCpy = iCpy / sizes[j];
			}
			returnList.add(subList);
		}
		return returnList;
	}

	public int getSmallestPartialStateActionVisitCount(Action action,
			List<PartialState> state) {
		int smallest = Integer.MAX_VALUE;
		for (int stateIndex = 0; stateIndex < state.size(); stateIndex++) {
			ParentValues ps = new ParentValues(state,
					connections.get(stateIndex));

			try {
				smallest = Math.min(smallest, stateActionCounts.get(stateIndex)
						.get(new ParentValuesAction(ps, action)));
			} catch (NullPointerException e) {
				return 0;
			}
		}
		return smallest;
	}

	Map<List<PartialState>,Map<Action,Map<List<PartialState>,Double>>> probCache = new HashMap<>();
	public double getProbability(List<PartialState> currentState,
			Action action, List<PartialState> nextState) {
		Map<Action,Map<List<PartialState>,Double>> m1 = probCache.get(currentState);
		if (m1 != null) {
			Map<List<PartialState>,Double> m2 = m1.get(action);
			if (m2 != null) {
				Double d = m2.get(nextState);
				if (d != null) {
					return d;
				}
			}
		}

		double product = 1;
		// Dummy state
		if (currentState.size() == 0) {
			if (nextState.size() == 0) {
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
		addToProbCache(currentState, action, nextState, product);
		return product;
	}
	  
	private void addToProbCache(List<PartialState> currentState, Action action,
			List<PartialState> nextState, double product) {
		Map<Action, Map<List<PartialState>, Double>> m1 = probCache.get(currentState);
		if (m1 == null) {
			m1 = new HashMap<>();
			probCache.put(currentState, m1);
		}
		Map<List<PartialState>, Double> m2 = m1.get(action);
		if (m2 == null) {
			m2 = new HashMap<>();
			m1.put(action, m2);
		}
		m2.put(nextState,product);		
	}
	public void clearProbCache() {
		probCache.clear();
	}
	private double getPartialProbability(List<PartialState> nextState,
			Action action, int stateIndex, ParentValues ps) {
		Map<Integer, Map<PartialState, Double>> map = p.get(stateIndex).get(ps);
		if (map == null) {
			return 0.0;
		}
		Map<PartialState, Double> map2 = map.get(action.getInt(stateIndex));
		if (map2 == null) {
			return 0.0;
		}
		Double d = map2.get(nextState.get(stateIndex));
		return d == null ? 0.0 : d;

	}

	public Map<Integer, List<Integer>> getConnections() {
		return connections;
	}

}
