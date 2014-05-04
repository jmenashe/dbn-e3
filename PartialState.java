import java.util.List;

/**
 * 
 */
public interface PartialState {
    public List<Integer> possibleActions();
    public int[] toIntarray(List<PartialState> state);
    public int getState(int i);
    public double getReward(int action);
    public int getIndex();
	public void setStateNr(int nr);
}
