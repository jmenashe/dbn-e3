public class MyState {
    private int[] states;
    private boolean isLastState;

    public MyState(int[] theState, boolean isLastState) {
        this.isLastState = isLastState;
        if (theState != null) {
            states = new int[theState.length];
            System.arraycopy(theState, 0, states, 0, theState.length);
        }
    }

    @Override
    public int hashCode() {
        if (isLastState) {
            return 0;
        }
        int hash = 1;
        for (int aState : states) {
            hash = hash * 17 + aState;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MyState)) {
            return false;
        }

        MyState other = (MyState) obj;

        if (other.isLastState && isLastState)
            return true;

        if (other.states.length != states.length) {
            return false;
        }

        for (int i = 0; i < states.length; i++) {
            if (other.states[i] != states[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("State: [ ");
        for (int aState : states) {
            sb.append(aState).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isLastState() {
        return isLastState;
    }

    public int[] getStates() {
        return states;
    }

    public int getState(int index) {
        if (index >= states.length || index < 0) {
            throw new IllegalArgumentException("Nonexistent index");
        }

        return states[index];
    }
}
