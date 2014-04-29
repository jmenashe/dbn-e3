
import java.util.*;


public final class ParentValues {
    private List<PartialState> parents;
    int hashCode = -1;

    public ParentValues(List<PartialState> fullState, List<Integer> parentIndices) {
    	parents = new ArrayList<>(parentIndices.size());
        for(int parent : parentIndices) {
        	parents.add(
        			fullState.get(parent));
        }
        calcHashCode();
    }
    
    private void calcHashCode() {
    	final int prime = 31;
		int result = 1;
		result = prime * result + ((parents == null) ? 0 : parents.hashCode());
		hashCode = result;
    }

    @Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParentValues other = (ParentValues) obj;
		if (parents == null) {
			if (other.parents != null)
				return false;
		} else if (!parents.equals(other.parents))
			return false;
		return true;
	}

	public String toString() {
        StringBuilder sb = new StringBuilder("Parent values [" );
        for (PartialState ps : parents) {
            sb.append(ps).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
}
