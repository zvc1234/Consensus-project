package protobuf;

import java.util.HashMap;
import java.util.Map;

class State{
    int valts;
    Paxos.Value val;

    public State(int valts, Paxos.Value val) {
        this.valts = valts;
        this.val = val;
    }

    public int getValts() {
        return valts;
    }

    public void setValts(int valts) {
        this.valts = valts;
    }

    public Paxos.Value getVal() {
        return val;
    }

    public void setVal(Paxos.Value val) {
        this.val = val;
    }
}

public class EpochConsensus implements Abstraction{

    System system;
    int ets;
    State state;
    Paxos.Value tmpval;
    Paxos.ProcessId l;
    Map<Paxos.ProcessId, State> states;
    int accepted;
    boolean abort;

    public EpochConsensus(System system, int ets, State state, Paxos.ProcessId l) {
        this.system = system;
        this.ets = ets;
        this.state = state;
        this.l = l;
        this.states = new HashMap<Paxos.ProcessId, State>();
        this.tmpval = Paxos.Value.newBuilder().setDefined(false).build();
        this.accepted = 0;
        this.abort = false;
    }

    @Override
    public String getId() {
        return "ep." + ets;
    }

    @Override
    public boolean handle(Paxos.Message message) {
        if(!abort)
            return false;
        switch (message.getType()){
            case EP_PROPOSE:
                java.lang.System.out.println("Through ep propose");
                epPropose(message.getEpPropose().getValue());
                return true;
            case BEB_DELIVER:
                switch (message.getBebDeliver().getMessage().getType()){
                    case EP_READ_:
                        java.lang.System.out.println("Through beb ep read");
                        epBebDeliverRead(message.getBebDeliver().getSender());
                        return true;
                    case EP_WRITE_:
                        java.lang.System.out.println("Through beb ep write");
                        epBebDeliverWrite(message.getBebDeliver().getSender(), message.getBebDeliver().getMessage().getEpWrite().getValue());
                        return true;
                    case EP_DECIDED_:
                        java.lang.System.out.println("Through beb ep decide");
                        epBebDeliverDecided(message.getBebDeliver().getSender(), message.getBebDeliver().getMessage().getEpDecided().getValue());
                        return true;
                }
            case PL_DELIVER:
                switch (message.getPlDeliver().getMessage().getType()){
                    case EP_STATE_:
                        java.lang.System.out.println("Through pl ep state");
                        epPlDeliverState(message.getPlDeliver().getSender(), message.getPlDeliver().getMessage().getEpState().getValueTimestamp(), message.getPlDeliver().getMessage().getEpState().getValue());
                        checkState();
                        return true;
                    case EP_ACCEPT_:
                        java.lang.System.out.println("Through pl ep accept");
                        epPlDeliverAccept(message.getPlDeliver().getSender());
                        checkAccepted();
                        return true;
                }
            case EP_ABORT:
                java.lang.System.out.println("Through ep abort");
                epAbort();
                return true;
        }
        return false;
    }

    public void epPropose(Paxos.Value v){
        tmpval = v;
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_BROADCAST).
                setAbstractionId("beb").setMessageUuid("ep_propose_broadcast").
                setBebBroadcast(Paxos.BebBroadcast.newBuilder().
                setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_READ_).
                setAbstractionId(getId()).setMessageUuid("ep_propose_broadcast_read").
                setEpRead(Paxos.EpRead_.newBuilder().build()).
                build()).build()).build());
    }

    public void epBebDeliverRead(Paxos.ProcessId l){
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_SEND).
                setAbstractionId("pl").setMessageUuid("ep_bebdeliver_read").
                setPlSend(Paxos.PlSend.newBuilder().setDestination(l).
                setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_STATE_).
                setAbstractionId(getId()).setMessageUuid("ep_bebdeliver_read_message").
                setEpState(Paxos.EpState_.newBuilder().setValue(state.val).
                setValueTimestamp(state.valts).build()).build()).build()).build());
    }

    public void epPlDeliverState(Paxos.ProcessId q, int ts, Paxos.Value v){
        states.put(q, new State(ts,v));
    }

    public void epBebDeliverWrite(Paxos.ProcessId l, Paxos.Value v){
        state.valts = ets;
        state.val = v;
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_SEND).
                setAbstractionId("pl").setSystemId(system.id).setMessageUuid("ep_bebdeliver_write").
                setPlSend(Paxos.PlSend.newBuilder().setDestination(l).
                setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_ACCEPT_).
                setAbstractionId(getId()).setMessageUuid("ep_bebdeliver_write_message").build()).
                build()).build());
    }

    public void epPlDeliverAccept(Paxos.ProcessId q){
        accepted++;
    }

    public void epBebDeliverDecided(Paxos.ProcessId l, Paxos.Value v){
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_DECIDE).
                setAbstractionId(getId()).setMessageUuid("ep_bebdeliver_decide").
                setEpDecide(Paxos.EpDecide.newBuilder().setValue(v).build()).build());
    }

    public void epAbort(){
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_ABORTED).
                setAbstractionId(getId()).setMessageUuid("ep_abort").
                setEpAborted(Paxos.EpAborted.newBuilder().setValueTimestamp(state.valts).setValue(state.val).
                build()).build());
        abort = true;
    }

    public State highestState(Map<Paxos.ProcessId, State> states){
        State s1 = new State(0, Paxos.Value.newBuilder().setDefined(false).build());
        for(Map.Entry<Paxos.ProcessId, State> s : states.entrySet()){
            if(s.getValue().getValts() > s1.getValts())
                s1 = s.getValue();
        }
        return s1;
    }

    public void checkState(){
        if(states.size() > system.processes.size()/2){
            State st = highestState(states);
            if(st.getVal().getDefined())
                tmpval = st.val;
        }
        states.clear();
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_BROADCAST).
                setAbstractionId("beb").setMessageUuid("ep_checkstate_bebbroadcast").
                setBebBroadcast(Paxos.BebBroadcast.newBuilder().
                setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_WRITE_).
                setAbstractionId(getId()).setMessageUuid("ep_checkstate_bebbroadcast_message").
                setEpWrite(Paxos.EpWrite_.newBuilder().setValue(tmpval).build()).build()).build()).build());
    }

    public void checkAccepted(){
        if(accepted > system.processes.size()/2){
            accepted = 0;
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_BROADCAST).
                    setAbstractionId("beb").setMessageUuid("ep_checkaccepted_bebbroadcast").
                    setBebBroadcast(Paxos.BebBroadcast.newBuilder().
                    setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EP_DECIDED_).
                    setAbstractionId(getId()).setMessageUuid("ep_checkaccepted_bebbroadcast_message").
                    setEpDecided(Paxos.EpDecided_.newBuilder().setValue(tmpval).build()).build()).build()).build());
        }
    }
}
