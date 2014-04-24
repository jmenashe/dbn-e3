import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains a representation of a reach. Nice!
 */
public class Reach implements PartialState {
    private int empty;
    private int tamarisk;
    private int plant;

    private int reachNumber;

    private int habitats;

    public Reach(Observation state, int reachNumber, int habitatsPerReach) {
        this.reachNumber = reachNumber;
        this.habitats = habitatsPerReach;

        for (int i = reachNumber * habitatsPerReach; i < (reachNumber + 1) * habitatsPerReach; i++) {
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

    @Override
    public double getCost(int action) {
        double cost = 0;

        if (tamarisk != 0) {
            cost += 10;
        }

        cost += 0.1 * plant;
        cost += 0.05 * empty;

        switch (action) {
            case 2: // Eradicate
                cost += tamarisk * 0.4 + 0.5;
                break;
            case 3: // Restore
                cost += empty * 0.4 + 0.9;
                break;
            case 4: // Eradicate + Restore
                cost += tamarisk * 0.4 + 0.5;
                cost += empty * 0.4 + 0.9;
                break;
        }

        return cost;
    }

    @Override
    public List<Integer> possibleActions() {
        List<Integer> actions = new ArrayList<>();

        actions.add(1);

        if (plant == habitats) {
            // only nothing action!
        } else if (tamarisk == 0) {
            actions.add(3);

        } else if (tamarisk == habitats) {
            actions.add(2);
            actions.add(4);

        } else if (empty == habitats) {
            actions.add(3);

        } else if (empty == 0) {
            actions.add(2);
            actions.add(4);

        } else {
            actions.add(2);
            actions.add(3);
            actions.add(4);
        }

        return actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reach reach = (Reach) o;

        return empty == reach.empty &&
                plant == reach.plant &&
                reachNumber == reach.reachNumber &&
                tamarisk == reach.tamarisk;

    }

    @Override
    public int hashCode() {
        int result = empty;
        result = 31 * result + tamarisk;
        result = 31 * result + plant;
        result = 31 * result + reachNumber;
        return result;
    }

    public static List<PartialState> allReaches(Observation state, int nReaches, int habitatsPerReach) {
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
}