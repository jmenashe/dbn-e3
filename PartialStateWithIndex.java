public class PartialStateWithIndex {
    final public int state;
    final public int index;

    public PartialStateWithIndex(int state, int index) {
        this.state = state;
        this.index = index;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + state;
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
        PartialStateWithIndex other = (PartialStateWithIndex) obj;
        return index == other.index &&
               state == other.state;
    }

    public String toString() {
        return new StringBuilder("pswi: state: ").append(state).
                append(" index: ").append(index).toString();
    }
}
