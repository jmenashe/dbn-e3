import org.rlcommunity.rlglue.codec.types.Action;


public class ParentValuesAction {
    private Action action;
    private ParentValues ps;

    public ParentValuesAction(ParentValues ps, Action action) {
        this.ps = ps;
        this.action = action;
    }

    public ParentValues getPs() {
        return ps;
    }

    public void setPs(ParentValues ps) {
        this.ps = ps;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "Psa: " + ps.toString() + " " + action.toString();
    }

    @Override
    public int hashCode() {
        return action.hashCode() * 17 + ps.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ParentValuesAction otherPsa = (ParentValuesAction) other;
        return otherPsa.action.equals(this.action) &&
                otherPsa.ps.equals(this.ps);
    }

}
