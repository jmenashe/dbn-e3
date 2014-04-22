import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.ArrayList;
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
                case 1: tamarisk++; break;
                case 2: plant++; break;
                case 3: empty++; break;
            }
        }
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

        if (empty != reach.empty) return false;
        if (plant != reach.plant) return false;
        if (reachNumber != reach.reachNumber) return false;
        if (tamarisk != reach.tamarisk) return false;

        return true;
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
}