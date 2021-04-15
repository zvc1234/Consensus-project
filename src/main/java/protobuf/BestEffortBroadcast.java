package protobuf;

import java.util.Map;

public class BestEffortBroadcast implements Abstraction {

    private System system;


    public BestEffortBroadcast(System system) {
        this.system = system;
    }

    @Override
    public String getId() {
        return "beb";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch(message.getType()){
            case BEB_BROADCAST:
                java.lang.System.out.println("Through broadcast");
                broadcast(message.getBebBroadcast().getMessage());
                return true;
            case PL_DELIVER:
                if(!message.getAbstractionId().equals(getId()))
                    return false;
                java.lang.System.out.println("Through pl deliver");
                plDeliver(message.getPlDeliver().getSender(), message.getPlDeliver().getMessage());
                return true;
        }
        return false;
    }

    public void broadcast(Paxos.Message m){
        for(Paxos.ProcessId p: system.processes){
            Paxos.Message message= Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_SEND).
                    setAbstractionId("pl").setSystemId(system.id).setMessageUuid("beb_broadcast").setPlSend
                    (Paxos.PlSend.newBuilder().setDestination(p).setMessage(m).build()).build();
            system.trigger(message);
        }
    }

    public void plDeliver(Paxos.ProcessId p, Paxos.Message m){
        Paxos.Message message = Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_DELIVER).
                setAbstractionId("beb").setMessageUuid("beb_pl_deliver").
                setBebDeliver(Paxos.BebDeliver.newBuilder().setMessage(m).setSender(p).build()).build();
        system.trigger(message);
    }


}
