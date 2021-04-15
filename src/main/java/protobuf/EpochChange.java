package protobuf;

import java.util.Set;

public class EpochChange implements Abstraction{

    private System system;
    private Paxos.ProcessId trusted;
    private int lastts;
    private int ts;


    public EpochChange(System system) {
        this.system = system;
        this.trusted = minRank(system.processes);
        this.lastts = 0;
        this.ts = system.self.id.getRank();
    }

    public Paxos.ProcessId getFirstProcess(){
        return trusted;
    }


    @Override
    public String getId() {
        return "ec";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case ELD_TRUST:
                java.lang.System.out.println("Through eld trust");
                eldTrust(message.getEldTrust().getProcess());
            case BEB_DELIVER:
                if(message.getBebDeliver().getMessage().getType().equals(Paxos.Message.Type.EC_NEW_EPOCH_)) {
                    java.lang.System.out.println("Through beb deliver new epoch");
                    bebDeliver(message.getBebDeliver().getSender(), message.getBebDeliver().getMessage().getEcNewEpoch().getTimestamp());
                    return true;
                }
            case PL_DELIVER:
                if(message.getPlDeliver().getMessage().getType().equals(Paxos.Message.Type.EC_NACK_)){
                    java.lang.System.out.println("Through pl deliver nack");
                    plDeliver(message.getPlDeliver().getSender());
                    return true;
                }
        }
        return false;
    }

    public void eldTrust(Paxos.ProcessId p){
        trusted = p;
        if(p.getPort() == system.self.id.getPort() && p.getHost() == system.self.id.getHost()){
            ts = ts + system.processes.size();
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_BROADCAST).
                    setAbstractionId("beb").setMessageUuid("ec_eldtrust").
                    setBebBroadcast(Paxos.BebBroadcast.newBuilder().setMessage
                    (Paxos.Message.newBuilder().setType(Paxos.Message.Type.EC_NEW_EPOCH_).
                    setAbstractionId("ec").setMessageUuid("ec_eldtrust_newepoch").
                    setEcNewEpoch(Paxos.EcNewEpoch_.newBuilder().setTimestamp(ts).build()).build())
                    .build()).build());
        }
    }

    public void bebDeliver(Paxos.ProcessId l, int newTs){
        if(l.getPort() == trusted.getPort() && l.getHost() == trusted.getHost() && newTs > lastts){
            lastts = newTs;
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EC_START_EPOCH).
                    setAbstractionId("ec").setMessageUuid("ec_bebdeliver_startepoch").
                    setEcStartEpoch(Paxos.EcStartEpoch.newBuilder().setNewLeader(l).
                    setNewTimestamp(newTs).build()).build());
        }
        else{
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_SEND).
                    setAbstractionId("pl").setSystemId(system.id).setMessageUuid("ec_bebdeliver_nack").
                    setPlSend(Paxos.PlSend.newBuilder().setDestination(l).
                    setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EC_NACK_).
                    setAbstractionId("ec").setMessageUuid("ec_bebdeliver_nack_message")
                    .build()).build()).build());
        }
    }

    public void plDeliver(Paxos.ProcessId p){
        if(trusted.getPort() == system.self.id.getPort() && trusted.getHost() == system.self.id.getHost()){
            ts = ts + system.processes.size();
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.BEB_BROADCAST).
                    setAbstractionId("beb").setMessageUuid("ec_pldeliver_broadcast").
                    setBebBroadcast(Paxos.BebBroadcast.newBuilder().
                    setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EC_NEW_EPOCH_).
                    setAbstractionId("ec").setMessageUuid("ec_pldeliver_broadcast_newepoch").
                    setEcNewEpoch(Paxos.EcNewEpoch_.newBuilder().setTimestamp(ts).build()).
                    build()).build()).build());
        }
    }

    public Paxos.ProcessId minRank(Set<Paxos.ProcessId> processes){
        Paxos.ProcessId min = null;
        for(Paxos.ProcessId p : processes){
            if(min == null)
                min = p;
            else if(p.getRank() < min.getRank())
                min = p;
        }
        return min;
    }
}
