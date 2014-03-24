class MyAction {
  private int[] action;

  public MyAction(int[] theAction) {
    action = new int[theAction.length];
    for(int i = 0; i < theAction.length; i++) {
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

    MyAction other = (MyAction)obj;

    if (other.action.length != action.length) {
      return false;
    }

    for(int i = 0; i < action.length; i++) {
      if (other.action[i] != action[i]) {
        return false;
      }
    }
    return true;
  }		
}
