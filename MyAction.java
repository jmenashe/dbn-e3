public class MyAction {

    private int[] actions;

    public MyAction(int[] theAction) {
        actions = new int[theAction.length];
        System.arraycopy(theAction, 0, actions, 0, theAction.length);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (int anAction : actions) {
            hash = hash * 17 + anAction;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MyAction)) {
            return false;
        }

        MyAction other = (MyAction) obj;

        if (other.actions.length != actions.length) {
            return false;
        }

        for (int i = 0; i < actions.length; i++) {
            if (other.actions[i] != actions[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Action: [ ");
        for (int anAction : actions) {
            sb.append(anAction).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public int getAction(int index) {
        if (index >= actions.length || index < 0) {
            throw new IllegalArgumentException("Nonexistent index");
        }
        return actions[index];
    }

    public int[] getActions() {
        return actions;
    }
}
