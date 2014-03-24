class MyState {
  public int[] state;
  public boolean isLastState;

  public MyState(int[] theState, boolean isLastState) {
    this.isLastState = isLastState;
    if (theState == null) return;
    state = new int[theState.length];
    for(int i = 0; i < theState.length; i++) {
      state[i] = theState[i];
    }
  }

  @Override
  public int hashCode() {
    if (isLastState)
      return 0;
    int hash = 1;
    for (int i = 0; i < state.length; i++) {
      hash = hash * 17 + state[i];
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MyState)) { 
      return false;
    }

    MyState other = (MyState)obj;

    if (other.isLastState && isLastState)
      return true;

    if (other.state.length != state.length) {
      return false;
    }

    for(int i = 0; i < state.length; i++) {
      if (other.state[i] != state[i]) {
        return false;
      }
    }
    return true;
  }
}
