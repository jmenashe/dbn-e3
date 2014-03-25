
public final class MyStateAction {
  public MyState state;
  public MyAction action;
  
  public MyStateAction(MyState state, MyAction action) {
	this.state = state;
	this.action = action;
  }
  
  public int hashCode() {
	//TODO: ??
	return state.hashCode() ^ action.hashCode(); 
  }
  
  public boolean equals(Object other) {
	if (!(other instanceof MyStateAction)) {
	  return false;
	}
	MyStateAction o = (MyStateAction) other;
	return o.action.equals(this.action) && o.state.equals(this.state);
  }
}
