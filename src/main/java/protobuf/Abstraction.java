package protobuf;

public interface Abstraction {
    public String getId();
    public boolean handle(Paxos.Message message);
}
