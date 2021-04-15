package protobuf;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.MILLIS;

public class EventualyPerfectFailureDetector implements Abstraction {

    private System system;
    private Set<Paxos.ProcessId> alive;
    private Set<Paxos.ProcessId> suspected;
    private Duration delay;
    private Duration epfdDelay = Duration.of(60, MILLIS);


    public EventualyPerfectFailureDetector(System system) {
        this.system = system;
        this.alive = system.processes;
        this.suspected = new HashSet<Paxos.ProcessId>();
        this.delay = epfdDelay;
        starttimer();
    }

    class T extends TimerTask{
        Timer t;
        T(Timer t){this.t = t;}

        public void run(){
            t.cancel();
            system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EPFD_TIMEOUT).
                    setAbstractionId(system.id).setMessageUuid("epfd_timer_timeout").
                    setEpfdTimeout(Paxos.EpfdTimeout.newBuilder().build()).build());
        }
    }

    public void starttimer(){
        Timer timer = new Timer();
        timer.schedule(new T(timer), delay.toMillis());
        //timer.schedule(new T(timer), 2*1000);
    }

    @Override
    public String getId() {
        return "epfd";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case EPFD_TIMEOUT:
                java.lang.System.out.println("Through epfd timeout");
                timeout();
                return true;
            case EPFD_HEARTBEAT_REQUEST:
                switch (message.getPlDeliver().getMessage().getType()){
                    case EPFD_HEARTBEAT_REQUEST:
                        java.lang.System.out.println("Through epfd hrequest");
                        plDeliverHeartbeatRequest(message.getPlDeliver().getSender());
                        return true;
                    case EPFD_HEARTBEAT_REPLY:
                        java.lang.System.out.println("Through epfd hreply");
                        plDeliverHeartbeatReply(message.getPlDeliver().getSender());
                        return true;
                }
        }
        return false;
    }

    public void timeout(){
        Set<Paxos.ProcessId> intersection = new HashSet<Paxos.ProcessId>(alive);
        intersection.retainAll(suspected);
        if(intersection.isEmpty()){
            delay = delay.plus(epfdDelay);
        }
        for(Paxos.ProcessId p : system.processes){
            if(!alive.contains(p) && !(suspected.contains(p))){
                suspected.add(p);
                Paxos.Message message = Paxos.Message.newBuilder().
                        setType(Paxos.Message.Type.EPFD_SUSPECT).
                        setAbstractionId("epfd").setMessageUuid("epfd_timeout_suspect").
                        setEpfdSuspect(Paxos.EpfdSuspect.newBuilder().setProcess(p).build()).build();
                system.trigger(message);
            }
            else if(alive.contains(p) && suspected.contains(p)){
                suspected.remove(p);
                Paxos.Message message = Paxos.Message.newBuilder().
                        setType(Paxos.Message.Type.EPFD_RESTORE).
                        setAbstractionId("epfd").setMessageUuid("epfd_timeout_restore").
                        setEpfdRestore(Paxos.EpfdRestore.newBuilder().setProcess(p).build()).build();
                system.trigger(message);
            }
            system.trigger(Paxos.Message.newBuilder().
                    setType(Paxos.Message.Type.PL_SEND).
                    setAbstractionId("pl").setMessageUuid("epfd_timeout_plsend").setSystemId(system.id).
                    setPlSend(Paxos.PlSend.newBuilder().setDestination(p).
                    setMessage(Paxos.Message.newBuilder().
                    setType(Paxos.Message.Type.EPFD_HEARTBEAT_REQUEST).
                    setAbstractionId("epfd").setMessageUuid("epfd_timeout_plsend_message").build()).
                    build()).build());
        }
        alive.clear();
        starttimer();
    }

    public void plDeliverHeartbeatRequest(Paxos.ProcessId sender){
        system.trigger(Paxos.Message.newBuilder().
                setType(Paxos.Message.Type.PL_SEND).
                setAbstractionId("pl").setSystemId(system.id).setMessageUuid("epfd_pldeliver_heartbeatrequest").
                setPlSend(Paxos.PlSend.newBuilder().setDestination(sender).
                setMessage(Paxos.Message.newBuilder().setType(Paxos.Message.Type.EPFD_HEARTBEAT_REPLY).
                setAbstractionId("epfd").setMessageUuid("epfd_pldeliver_plsend_message").build()).
                build()).build());
    }

    public void plDeliverHeartbeatReply(Paxos.ProcessId sender){
        alive.add(sender);
    }
}
