import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.*;

public class PartialTransitionProbabilityLogger {

	// From target state index to list of parent indices
	private Map<Integer, List<Integer>> connections;
	// From target state index to values of parents to count
	private Map<Integer, Map<PartialStateAction, Integer>> stateActionCounts;
	private Map<Integer, Map<PartialState, Integer>> stateCounts;
	private Map<PartialStateWithIndex, Map<PartialState, Map<Action, Integer>>> stateActionStateCounts;
	// From target state index to set of known state/action combinations
	private Map<Integer, Map<PartialState, List<Integer>>> known;

	// From state index to possible states
	private Map<Integer, List<Integer>> possibleStates;

	// map from target state index to
	// map from parents' state to
	// map from (what action) to
	// map from (target's state) to probability
	private Map<Integer, Map<PartialState, Map<Action, Map<Integer, Double>>>> p;
	private int knownLimit;

	public PartialTransitionProbabilityLogger(
			Map<Integer, List<Integer>> connections, List<Action> actions,
			Map<Integer, List<Integer>> possibleStates, int knownLimit) {
		p = new HashMap<>();
		this.connections = connections;
		this.possibleStates = possibleStates;
		for (int i : connections.keySet()) {
			p.put(i,
					new HashMap<PartialState, Map<Action, Map<Integer, Double>>>());
		}
		this.knownLimit = knownLimit;
		known = new HashMap<>();
		
		stateCounts = new HashMap<>();
		stateActionCounts = new HashMap<>();
		stateActionStateCounts = new HashMap<>();
	}

	private void increaseCounts(int stateIndex, int state,
			PartialStateAction psa) {

		increaseStateActionCount(stateIndex, psa);
		increaseStateCount(stateIndex, psa.getPs());
		increaseStateActionStateCount(stateIndex, state, psa);
	}

	private void increaseStateActionStateCount(int stateIndex, int state,
			PartialStateAction psa) {
		PartialStateWithIndex pswi = new PartialStateWithIndex(stateIndex,
				state);
		Map<PartialState, Map<Action, Integer>> theMap = stateActionStateCounts
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
		if (count == knownLimit) {
			addToKnown(stateIndex, state, psa.getPs());
		}
	}

	private void increaseStateCount(int stateIndex, PartialState ps) {
		Integer count;
		Map<PartialState, Integer> map2 = stateCounts.get(stateIndex);
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
		Map<PartialStateAction, Integer> stateCounts = stateActionCounts
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
		for (int stateIndex = 0; stateIndex < to.intArray.length; stateIndex++) {
			int state = to.intArray[stateIndex];
			PartialState ps = new PartialState(from,
					connections.get(stateIndex));
			Map<Action, Map<Integer, Double>> m = p.get(stateIndex).get(ps);
			if (m == null) {
				m = new HashMap<>();
				p.get(stateIndex).put(ps, m);
			}
			Map<Integer, Double> m2 = m.get(action);
			if (m2 == null) {
				m2 = new HashMap<>();
				m.put(action, m2);
			}
			PartialStateAction psa = new PartialStateAction(ps, action);

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
		}
	}

	private void addToKnown(int sourceIndex, int targetState, PartialState ps) {
		Map<PartialState, List<Integer>> psMap = known.get(sourceIndex);
		if (psMap == null) {
			psMap = new HashMap<>();
			known.put(sourceIndex, psMap);
		}

		List<Integer> targetList = psMap.get(ps);

		if (targetList == null) {
			targetList = new LinkedList<>();
			psMap.put(ps, targetList);
		}

		targetList.add(targetState);
	}

	public double getProbability(int stateIndex, PartialState ps,
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
		for (int stateIndex = 0; stateIndex < connections.size(); stateIndex++) {

			PartialState ps = new PartialState(observedState,
					connections.get(stateIndex));
			for (int state : possibleStates.get(stateIndex)) {
				try {
					if (!known.get(stateIndex).get(ps).contains(state)) {
						return false;
					}
				} catch (NullPointerException e) {
					return false;
				}
			}
		}

		return true;
	}

	public Map<Observation, Double> getTransitionProbs(Observation state,
			Action action) {

		return null;
	}

	public Map<Integer, Map<PartialState, List<Integer>>> getKnown() {
		return known;
	}

	public Iterator<Observation> knownStateIterator() {
		return null;
	}
	
	public List<Observation> statesFromPartialStates(Observation wholeState, Action action) {
		List<List<Integer>> seenStates = new LinkedList<>();
		for (int stateIndex = 0; stateIndex < wholeState.intArray.length; stateIndex++) {
			Map<PartialState, Map<Action, Map<Integer, Double>>> map = 
					p.get(stateIndex);
			PartialState ps = new PartialState(wholeState, connections.get(stateIndex));
			
			Map<Action, Map<Integer, Double>> map2 = map.get(ps);
			seenStates.add(new ArrayList<Integer>(map2.get(action).keySet()));
		}
		List<Observation> returnList = new ArrayList<>(); 
		for (List<Integer> l :  nextStates(seenStates)) {
			Observation o = new Observation(l.size(), 0);
			for(int i =0; i < l.size(); i++) {
				o.setInt(i, l.get(i));
			}
			returnList.add(o);
		}
		return returnList;
	}
	
	public static List<List<Integer>>nextStates(List<List<Integer>> input) {
		int max = 1;
		int[] sizes = new int[input.size()];
		int k = 0;
		for(List<Integer> l : input) {
			max *= l.size();
			sizes[k++] = l.size();
		}
		List<List<Integer>> returnList = new LinkedList<>();
		for(int i = 0; i < max; i++) {
			int iCpy = i;
			List<Integer> subList = new LinkedList<>(); 
			for (int j = 0; j < input.size(); j++) {
				subList.add(input.get(j).get(iCpy % sizes[j]));
				iCpy = iCpy / sizes[j];
			}
			returnList.add(subList);
		}
		return returnList;
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

		Map<Integer, List<Integer>> possible = new HashMap<>();

		List<Integer> possibleVals = new LinkedList<>();
		possibleVals.add(0);
		possibleVals.add(1);
		possible.put(0, possibleVals);
		possible.put(1, possibleVals);
		possible.put(2, possibleVals);

		PartialTransitionProbabilityLogger ptpl = new PartialTransitionProbabilityLogger(
				map, actions, possible, 3);
		// observation from
		Observation from = new Observation(3, 0);
		from.intArray[0] = 0;
		from.intArray[1] = 1;
		from.intArray[2] = 1;

		// action
		Action action = new Action(1, 0);
		action.intArray[0] = 1;

		// observation to
		Observation to = new Observation(3, 0);
		to.intArray[0] = 1;
		to.intArray[1] = 1;
		to.intArray[2] = 0;
		ptpl.record(from, action, to);
		to.intArray[0] = 1;
		to.intArray[1] = 0;
		to.intArray[2] = 1;
		ptpl.record(from, action, to);
		ptpl.record(from, action, to);
		ptpl.record(from, action, to);

		PartialState ps = new PartialState(from, map.get(0));
		PartialStateAction psa = new PartialStateAction(ps, action);

		System.out.println("ptpl: " + ptpl.getStateActionCount(0, psa));
		System.out.println("probability: "
				+ ptpl.getProbability(0, ps, action, 0));

		//System.out.println(ptpl.getKnown());
		System.out.println(ptpl.statesFromPartialStates(from, action));
		
	}
}
