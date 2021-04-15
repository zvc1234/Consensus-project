package protobuf;

class Pair{
    int timestamp;
    Paxos.ProcessId process;

    public Pair(int timestamp, Paxos.ProcessId process) {
        this.timestamp = timestamp;
        this.process = process;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Paxos.ProcessId getProcess() {
        return process;
    }

    public void setProcess(Paxos.ProcessId process) {
        this.process = process;
    }
}

public class UniformConsensus implements Abstraction {

    System system;
    Paxos.Value val;
    boolean proposed;
    boolean decided;
    Paxos.ProcessId l;
    Pair newState;
    Pair initialState;
    EpochConsensus ep;

    public UniformConsensus(System system, EpochChange ec, EpochConsensus ep) {
        this.system = system;
        this.val = Paxos.Value.newBuilder().setDefined(false).build();
        this.proposed = false;
        this.decided = false;
        this.ep = ep;
        this.l = ec.getFirstProcess();
    }


    @Override
    public String getId() {
        return "uc";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case UC_PROPOSE:
                java.lang.System.out.println("Through uc propose");
                ucPropose(message.getUcPropose().getValue());
                check();
                return true;
            case EC_START_EPOCH:
                java.lang.System.out.println("Through ec start epoch");
                ucEcStartEpoch(new Pair(message.getEcStartEpoch().getNewTimestamp(), message.getEcStartEpoch().getNewLeader()));
                return true;
            case EP_DECIDE:
                java.lang.System.out.println("Through ep decide");
                if(message.getEpDecide().getEts() == initialState.getTimestamp())
                {
                    ucEpDecide(message.getEpDecide().getValue());
                    return true;
                }
            case EP_ABORTED:
                java.lang.System.out.println("Through ep aborted");
                if(message.getEpAborted().getEts() == initialState.getTimestamp()){
                    ucEpAborted(new State(message.getEpAborted().getValueTimestamp(), message.getEpAborted().getValue()));
                    return true;
                }
        }
        return false;
    }

    public void ucPropose(Paxos.Value v) {
        val = v;
    }

    public void ucEcStartEpoch(Pair p) {
        newState.setTimestamp(p.getTimestamp());
        newState.setProcess(p.getProcess());
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_ABORT).
                setAbstractionId(ep.getId()).setMessageUuid("uc_ecstartepoch").
                setEpAbort(Paxos.EpAbort.newBuilder().build()).build());
    }

    public void check() {
        if(l.getPort() == system.self.id.getPort() && l.getHost() == system.self.id.getHost() && val.getDefined() && !proposed){
            proposed = true;
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_PROPOSE).
                    setAbstractionId(ep.getId()).setMessageUuid("check_eppropose").
                    setEpPropose(Paxos.EpPropose.newBuilder().setValue(val).build()).build());
        }
    }

    public void ucEpDecide(Paxos.Value v) {
        if(!decided){
            decided = true;
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.UC_DECIDE).
                    setAbstractionId("uc").setMessageUuid("uc_epdecide").
                    setUcDecide(Paxos.UcDecide.newBuilder().setValue(v).build()).build());
        }
    }

    public void ucEpAborted(State state) {
        initialState.setProcess(newState.getProcess());
        initialState.setTimestamp(newState.getTimestamp());
        proposed = false;
        EpochConsensus e = new EpochConsensus(system, initialState.getTimestamp(), state, initialState.getProcess());
        system.registerEp(ep,e);
        this.ep = e;
    }
}
