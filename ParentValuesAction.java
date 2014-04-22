import org.rlcommunity.rlglue.codec.types.Action;


public class ParentValuesAction {
    private Action action;
    private ParentValues pv;

    public ParentValuesAction(ParentValues pv, Action action) {
        this.pv = pv;
        this.action = action;
    }

    public ParentValues getPv() {
        return pv;
    }

    public void setPs(ParentValues pv) {
        this.pv = pv;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "Psa: " + pv.toString() + " " + action.toString();
    }

    @Override
    public int hashCode() {
        return action.hashCode() * 17 + pv.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ParentValuesAction otherPsa = (ParentValuesAction) other;
        return otherPsa.action.equals(this.action) &&
                otherPsa.pv.equals(this.pv);
    }

}
