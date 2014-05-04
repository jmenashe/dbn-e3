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
	
	public Reach(Reach ps) {
		
		this.empty = ps.empty;
		this.tamarisk = ps.tamarisk;
		this.plant = ps.plant;
		this.reachNumber = ps.reachNumber;
		this.habitats = ps.habitats;		
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



	private static Set<PartialState> allPartialsCache = null;
	public static Set<PartialState> allPartials(int reachSize) {
		if (allPartialsCache != null) {
			return allPartialsCache;
		}
		int reachCount = 1;
		for(int i = 0; i < reachSize; i++) {
			reachCount *= 3;
		}
		 allPartialsCache = new HashSet<>(); 
		for(int i = 0; i < reachCount; i++) {
			int iCpy = i;
			Observation o = new Observation(reachSize, 0);
			for(int j = 0; j < reachSize; j++) {
				o.intArray[j] = (iCpy % 3) + 1;
				iCpy /= 3;
			}
			
			allPartialsCache.add(new Reach(o, 0, reachSize));
		}
		
		return allPartialsCache;
	}
	
	private static Map<List<Integer>, Set<ParentValues>> allParentValuesCache = new HashMap<>();
	public static Set<ParentValues> allParentValues(int reachSize, int reachCount, List<Integer> parentIndices) {
		Set<ParentValues> pvs = new HashSet<ParentValues>();
		if (allParentValuesCache.get(parentIndices) != null) {
			return allParentValuesCache.get(parentIndices);
		}
		List<PartialState> partials = new ArrayList<>();; //= new ArrayList<PartialState>( allPartials(reachSize));
		for(PartialState ps : allPartials(reachSize)) {
			partials.add(new Reach((Reach)ps));
		}
		int pvCount = 1;
		for(int i = 0; i < parentIndices.size(); i++) {
			pvCount *= partials.size();
		}
		
		List<PartialState> tempState = new ArrayList<>(reachCount);
		for(int i = 0; i < reachCount; i++) {
			tempState.add(null);
		}
		for(int i = 0; i < pvCount; i++) {
			int iCpy = i;
			for(int j = 0; j < parentIndices.size(); j++) {
				PartialState ps = new Reach((Reach) partials.get(iCpy % partials.size()));
				ps.setReachNr(parentIndices.get(j));
				tempState.set(parentIndices.get(j),ps);
				iCpy /= partials.size();
			}
			pvs.add(new ParentValues(tempState, parentIndices));
			
		}
		allParentValuesCache.put(parentIndices, pvs);
		return pvs;
	}
	

	@Override
	public void setReachNr(int nr) {
		reachNumber = nr;
		
	}
}