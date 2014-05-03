import org.rlcommunity.rlglue.codec.types.Action;


public class ParentValuesAction {
    private int action;
    private ParentValues pv;

    public ParentValuesAction(ParentValues pv, int targetState, Action action) {
        this.pv = pv;
        this.action = action.intArray[targetState];
    }

    public ParentValues getPv() {
        return pv;
    }

    public void setPs(ParentValues pv) {
        this.pv = pv;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "Psa: " + pv.toString() + " " + action;
    }

    @Override
    public int hashCode() {
        return action * 17 + pv.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ParentValuesAction otherPsa = (ParentValuesAction) other;
        return otherPsa.action == (this.action) &&
                otherPsa.pv.equals(this.pv);
    }

}
