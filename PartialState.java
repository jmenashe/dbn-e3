import org.rlcommunity.rlglue.codec.types.Observation;

import java.util.Arrays;
import java.util.List;


public final class PartialState {
    private List<Integer> parents;
    private Observation state;
    private Integer hashCode = null;

    public PartialState(Observation obs, List<Integer> parents) {
    	this.parents = parents;
    	this.state = obs;
    }

    
    public int hashCode() {
    	if (hashCode != null) {
    		return hashCode;
    	}
    	int code = 1;
    	for (Integer i : parents) {
    		code = code*17 + state.intArray[i];
    		code = code*17 + i;
    	}
    	hashCode = code;
        return code;
    }

    public boolean equals(Object other) {
        if (!(other instanceof PartialState)) {
            return false;
        }
        PartialState otherPs = (PartialState) other;
        if (otherPs.state.intArray.length != this.state.intArray.length) {
        	return false;
        }
        if (!this.parents.equals(otherPs.parents)) {
        	return false;
        }
        
        for(Integer i : parents) {
        	if (this.state.intArray[i] != otherPs.state.intArray[i]) {
        		return false;
        	}
        }
        return true;
    }

    public int getState(int i) {
        return state.intArray[parents.get(i)];
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder("PState [");
    	for(int i : parents) {
    		sb.append(i).append("-").append(state.intArray[i]).append(" ");
    	}
    	sb.append("]");
        return sb.toString();
    }
}
