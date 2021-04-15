package protobuf;
import java.util.Map;

public class Process {
    Paxos.ProcessId id;

    public Process(Paxos.ProcessId id) {
        this.id = id;
    }
}
