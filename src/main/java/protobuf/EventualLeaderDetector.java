package protobuf;

import java.util.HashSet;
import java.util.Set;

public class EventualLeaderDetector implements Abstraction{

    private System system;
    private Set<Paxos.ProcessId> suspected;
    private Set<Paxos.ProcessId> notSuspected;
    private Paxos.ProcessId leader;

    public EventualLeaderDetector(System system) {
        this.system = system;
        suspected = new HashSet<Paxos.ProcessId>();
        this.leader = null;
    }


    @Override
    public String getId() {
        return "eld";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case EPFD_SUSPECT:
                java.lang.System.out.println("Through epfd suspect");
                epfdSuspect(message.getEpfdSuspect().getProcess());
                return true;
            case EPFD_RESTORE:
                java.lang.System.out.println("Through epfd restore");
                epfdRestore(message.getEpfdSuspect().getProcess());
                return true;
        }
        return false;
    }

    public void epfdSuspect(Paxos.ProcessId p){
        suspected.add(p);
        check();
    }

    public void epfdRestore(Paxos.ProcessId p){
        suspected.remove(p);
        check();
    }

    public void check(){
        notSuspected = system.processes;
        notSuspected.removeAll(suspected);
        if(leader.getRank() != maxRank(notSuspected).getRank()) {
            leader = maxRank(notSuspected);
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.ELD_TRUST).
                    setAbstractionId("eld").setMessageUuid("eld_trust").
                    setEldTrust(Paxos.EldTrust.newBuilder().setProcess(leader).build()).
                    build());
        }
    }

    public Paxos.ProcessId maxRank(Set<Paxos.ProcessId> processes){
        Paxos.ProcessId max = null;
        for(Paxos.ProcessId p : processes){
            if(max == null)
                max = p;
            else if(p.getRank() > max.getRank())
                max = p;
        }
        return max;
    }
}
