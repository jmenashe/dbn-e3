import java.util.List;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;


public interface AllActionsGetter {
	List<Action> getAllActions(List<PartialState> state);
}
