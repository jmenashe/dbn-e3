import java.util.List;

/**
 * 
 */
public interface PartialState {
    public List<Integer> possibleActions();
    public int[] toIntarray(List<PartialState> state);
    public int getState(int i);
    public double getCost(int action);
}
