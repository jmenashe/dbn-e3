class MyAction {
    public int[] action;

    public MyAction(int[] theAction) {
        action = new int[theAction.length];
        for (int i = 0; i < theAction.length; i++) {
            action[i] = theAction[i];
        }
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < action.length; i++) {
            hash = hash * 17 + action[i];
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MyAction)) {
            return false;
        }

        MyAction other = (MyAction) obj;

        if (other.action.length != action.length) {
            return false;
        }

        for (int i = 0; i < action.length; i++) {
            if (other.action[i] != action[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Action: [ ");
        for (int i = 0; i < action.length; i++) {
            sb.append(action[i] + " ");
        }
        sb.append("]");
        return sb.toString();
    }
}
