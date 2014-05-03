import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.*;

/**
 * Contains a representation of a reach. Nice!
 */
public class Reach implements PartialState {

	/*
	 * State Actions
	 */
	public static final int ACTION_DO_NOTHING = 1;
	public static final int ACTION_ERADICATE = 2;
	public static final int ACTION_RESTORE = 3;
	public static final int ACTION_ERADICATE_AND_RESTORE = 4;

	private int empty;
	private int tamarisk;
	private int plant;

	private int reachNumber;

	private int habitats;

	public Reach(Observation state, int reachNumber, int habitatsPerReach) {
		this.reachNumber = reachNumber;
		this.habitats = habitatsPerReach;

		for (int i = reachNumber * habitatsPerReach; i < (reachNumber + 1)
				* habitatsPerReach; i++) {
			switch (state.intArray[i]) {
			case 1:
				tamarisk++;
				break;
			case 2:
				plant++;
				break;
			case 3:
				empty++;
				break;
			}
		}
	}
	
	private double maxReward() {
		return 11.6 + 0.9*habitats;
	}

	@Override
	public double getReward(int action) {
		double cost = 0;

		if (tamarisk != 0) {
			cost += 10;
		}

		cost += 0.1 * plant;
		cost += 0.05 * empty;

		switch (action) {
		case ACTION_ERADICATE: // Eradicate
			cost += tamarisk * 0.4 + 0.5;
			return cost;
		case ACTION_RESTORE: // Restore
			cost += empty * 0.4 + 0.9;
			return cost;
		case ACTION_ERADICATE_AND_RESTORE: // Eradicate + Restore
			cost += tamarisk * 0.4 + 0.5;
			cost += empty * 0.4 + 0.9;
			return cost;
		}

		return maxReward() - cost;
	}

	@Override
	public List<Integer> possibleActions() {
		List<Integer> actions = new ArrayList<>();

		actions.add(ACTION_DO_NOTHING);

		if (plant == habitats) {
			// only nothing action!
		} else if (tamarisk == 0) {
			actions.add(ACTION_RESTORE);

		} else if (tamarisk == habitats) {
			actions.add(ACTION_ERADICATE);
			actions.add(ACTION_ERADICATE_AND_RESTORE);

		} else if (empty == habitats) {
			actions.add(ACTION_RESTORE);

		} else if (empty == 0) {
			actions.add(ACTION_ERADICATE);
			actions.add(ACTION_ERADICATE_AND_RESTORE);

		} else {
			actions.add(ACTION_ERADICATE);
			actions.add(ACTION_RESTORE);
			actions.add(ACTION_ERADICATE_AND_RESTORE);
		}

		return actions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Reach reach = (Reach) o;

		return empty == reach.empty && plant == reach.plant
				//&& reachNumber == reach.reachNumber
				&& tamarisk == reach.tamarisk;

	}

	@Override
	public int hashCode() {
		int result = empty;
		result = 31 * result + tamarisk;
		result = 31 * result + plant;
		//result = 31 * result + reachNumber;
		return result;
	}

	public static List<PartialState> allReaches(Observation state,
			int nReaches, int habitatsPerReach) {
		List<PartialState> partialStates = new ArrayList<>();

		for (int reach = 0; reach < nReaches; reach++) {
			partialStates.add(new Reach(state, reach, habitatsPerReach));
		}

		return partialStates;
	}

	@Override
	public int[] toIntarray(List<PartialState> state) {
		List<Integer> returnList = new LinkedList<>();
		for (PartialState partialState : state) {
			for (int i = 1; i < 4; i++) {
				for (int j = 0; j < partialState.getState(i); j++) {
					returnList.add(i);
				}
			}
		}
		// java sucks some times
		int[] returnArray = new int[returnList.size()];
		for (int i = 0; i < returnList.size(); i++) {
			returnArray[i] = returnList.get(i);
		}
		return returnArray;
	}

	@Override
	public int getState(int i) {
		switch (i) {
		case 1:
			return tamarisk;
		case 2:
			return plant;
		case 3:
			return empty;
		default:
			throw new IllegalArgumentException("Nonexistent index");
		}
	}

	@Override
	public String toString() {
		return "[" + tamarisk + " " + plant + " " + empty + "]";
	}

	@Override
	public int getIndex() {
		return reachNumber;
	}

	private static Set<PartialState> allPartials = null;

	public static Set<PartialState> allPartials(
			Set<List<PartialState>> allStates) {
		if (allPartials != null) {
			return allPartials;
		}

		allPartials = new HashSet<PartialState>();
		// This is ridiculous, but who gives a shit
		for (List<PartialState> fullState : allStates) {
			for (PartialState partial : fullState) {
				allPartials.add(partial);
			}
		}

		return allPartials;
	}

	private static Map<Integer, Set<ParentValues>> allParentValuesCache = new HashMap<>();

	public static Set<ParentValues> allParentValues(
			Set<List<PartialState>> allStates, List<Integer> parentIndices) {

		int parentCount = parentIndices.size();
		if (allParentValuesCache.get(parentCount) != null) {
//			return allParentValuesCache.get(parentCount);
		}

		Set<ParentValues> pvs = new HashSet<ParentValues>();
		for (List<PartialState> state : allStates) {
			ParentValues pv = new ParentValues(state, parentIndices);
			pvs.add(pv);
		}
		//allParentValuesCache.put(parentCount,pvs);
		return pvs;
	}
}