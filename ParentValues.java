
import java.util.*;


public final class ParentValues {
    private List<PartialState> parents;
  

    public ParentValues(List<PartialState> fullState, List<Integer> parentIndices) {
    	parents = new ArrayList<PartialState>(parentIndices.size());
        for(int parent : parentIndices) {
        	parents.add(
        			fullState.get(parent));
        }
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parents == null) ? 0 : parents.hashCode());
		return result;
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
