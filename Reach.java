import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a representation of a reach. Nice!
 */
public class Reach {
    private int empty;
    private int tamarisk;
    private int plant;

    private int reachNumber;

    public Reach(Observation state, int reachNumber, int habitatsPerReach) {
        this.reachNumber = reachNumber;

        for (int i = reachNumber * habitatsPerReach; i < (reachNumber + 1) * habitatsPerReach; i++) {
            switch (state.intArray[i]) {
                case 1: tamarisk++; break;
                case 2: plant++; break;
                case 3: empty++; break;
            }
        }
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

    public static List<Reach> allReaches(Observation state, int nReaches, int habitatsPerReach) {
        List<Reach> reaches = new ArrayList<>();

        for (int reach = 0; reach < nReaches; reach++) {
            reaches.add(new Reach(state, reach, habitatsPerReach));
        }

        return reaches;
    }
}