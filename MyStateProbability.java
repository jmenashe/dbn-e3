public final class MyStateProbability {
    private MyState state;
    private double value;

    public MyStateProbability(MyState state, double value) {
        this.state = state;
        this.value = value;
    }

    public int hashCode() {
        return state.hashCode() * 13;
    }

    public boolean equals(Object other) {
        if (!(other instanceof MyStateProbability)) {
            return false;
        }
        MyStateProbability msp = (MyStateProbability) other;
        return state.equals(msp.state);
    }

    public MyState getState() {
        return state;
    }

    public double getValue() {
        return value;
    }
}
