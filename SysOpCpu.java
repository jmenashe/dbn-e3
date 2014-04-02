import java.util.LinkedList;
import java.util.List;

public final class SysOpCpu {
    private boolean running;
    private int id;

    private LinkedList<SysOpCpu> linkedCpus;

    public SysOpCpu(boolean running, List<SysOpCpu> linkedCpus, int id) {
        if (linkedCpus != null) {
            this.linkedCpus = new LinkedList<>(linkedCpus);
        } else {
            this.linkedCpus = new LinkedList<>();
        }
        this.id = id;
        this.running = running;
    }

    public SysOpCpu(int id) {
        this(true, new LinkedList<SysOpCpu>(), id);
    }

    public int getId() {
        return id;
    }

    public boolean isRunning() {
        return running;
    }

    public void setIsRunning(boolean running) {
        this.running = running;
    }

    public void addToLinked(SysOpCpu cpu) {
        linkedCpus.add(cpu);
    }

    public void addToLinked(List<SysOpCpu> cpus) {
        linkedCpus.addAll(cpus);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Cpu id: " + id + "; connected to");
        for (SysOpCpu c : linkedCpus) {
            sb.append(" ").append(c.id);
        }

        return sb.toString();
    }

    public List<SysOpCpu> getLinked() {
        return linkedCpus;
    }

    public boolean equals(Object other) {
        if (!(other instanceof SysOpCpu)) {
            return false;
        }
        SysOpCpu otherCpu = (SysOpCpu) other;
        return id == otherCpu.id;
    }

    public int hashCode() {
        return id * 13;
    }
}
