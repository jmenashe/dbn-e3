import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.List;


public final class ParentValues {
    private List<Integer> parents;
    private Observation state;
    private int hashCode = -1;

    public ParentValues(Observation obs, List<Integer> parents) {
        this.parents = parents;
        this.state = obs;
    }

    @Override
    public int hashCode() {
        if (hashCode != -1) {
            return hashCode;
        }
        hashCode = 1;
        for (int i : parents) {
            hashCode = hashCode * 17 + state.intArray[i];
            hashCode = hashCode * 17 + i;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ParentValues otherPs = (ParentValues) other;

        if (otherPs.state.intArray.length != this.state.intArray.length) {
            return false;
        }

        if (!this.parents.equals(otherPs.parents)) {
            return false;
        }

        for (int i : parents) {

            if (this.state.intArray[i] != otherPs.state.intArray[i]) {
                return false;
            }
        }
        return true;
    }

    public int getState(int i) {
        return state.intArray[parents.get(i)];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PState [");
        for (int i : parents) {
            sb.append(i).append("-").append(state.intArray[i]).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
}
