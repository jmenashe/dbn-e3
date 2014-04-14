import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.Arrays;
import java.util.List;


public final class PartialState {
    private int[] state;

    public PartialState(Observation obs, List<Integer> parents) {
        state = new int[parents.size()];
        int j = 0;
        for (int i : parents) {
            state[j++] = obs.intArray[i];
        }
    }

    public int hashCode() {
        return Arrays.hashCode(state);
    }

    public boolean equals(Object other) {
        if (!(other instanceof PartialState)) {
            return false;
        }
        PartialState otherPs = (PartialState) other;
        return Arrays.equals(otherPs.state, state);
    }

    public int getState(int i) {
        return state[i];
    }

    public String toString() {
        return "Partial state: " + Arrays.toString(state);
    }
}
