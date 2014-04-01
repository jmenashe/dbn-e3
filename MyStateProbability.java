final class MyStateProbability {
    public MyState state;
    public double value;

    public MyStateProbability(MyState state, double value) {
        this.state = state;
        this.value = value;

    }

    public int hashCode() {
        return state.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof MyStateProbability)) {
            return false;
        }
        MyStateProbability msp = (MyStateProbability) other;
        return state.equals(msp.state);
    }
}
