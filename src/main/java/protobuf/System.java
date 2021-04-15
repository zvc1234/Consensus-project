package protobuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class System{

    public String id;
    public Process self;
    public Set<Paxos.ProcessId> processes;
    public Map<String, Abstraction> abstractions;
    public ConcurrentLinkedQueue<Paxos.Message> events;
    public Paxos.Message message;

    public System(){}

    public System(String id, Process self) {
        this.id = id;
        this.self = self;
        this.abstractions = new HashMap<String, Abstraction>();
        this.events = new ConcurrentLinkedQueue<Paxos.Message>();
        this.processes = new HashSet<Paxos.ProcessId>();
    }

    public int getPort(){
        return self.id.getPort();
    }

    public void registerEp(EpochConsensus oldEp, EpochConsensus newEp){
        abstractions.remove(oldEp.getId());
        abstractions.put(newEp.getId(), newEp);
    }

    public void registerAbstractions(){
        BestEffortBroadcast beb = new BestEffortBroadcast(this);
        EventualyPerfectFailureDetector epfd = new EventualyPerfectFailureDetector(this);
        EventualLeaderDetector eld = new EventualLeaderDetector(this);
        EpochChange ec = new EpochChange(this);
        EpochConsensus ep = new EpochConsensus(this,0, new State(0, Paxos.Value.newBuilder().setDefined(false).build()), ec.getFirstProcess());
        UniformConsensus uc = new UniformConsensus(this, ec, ep);
        PerfectLink pl = new PerfectLink(this);
        abstractions.put(beb.getId(), beb);
        abstractions.put(epfd.getId(), epfd);
        abstractions.put(eld.getId(), eld);
        abstractions.put(ec.getId(), ec);
        abstractions.put(uc.getId(), uc);
        abstractions.put(pl.getId(), pl);
    }

    public void trigger(Paxos.Message message){
        events.add(message);
    }

    public void setProcesses(List<Paxos.ProcessId> processes){
        this.processes.addAll(processes);
    }

//    public void network(String host, int port){
//        ServerSocket serverSocket = null;
//        InputStream input = null;
//        try{
//            serverSocket = new ServerSocket(port);
//            Socket socket = serverSocket.accept();
//            java.lang.System.out.println("Port " + socket.getLocalPort() + " "+ socket.getInetAddress());
//            input = socket.getInputStream();
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            byte buffer[] = new byte[1024];
//            baos.write(buffer, 0 , input.read(buffer));
//            byte result[] = baos.toByteArray();
//
//            ByteBuffer bBuf = ByteBuffer.wrap(result);
//            byte[] b = new byte[4];
//            bBuf.get(b,0,b.length);
//            ByteBuffer newBBuf  = ByteBuffer.wrap(b);
//            int size = newBBuf.getInt();
//            java.lang.System.out.println(size);
//           // int size = result[0] + result [1] + result[2] + result[3];
//            byte[] buf = ByteBuffer.allocate(size).put(buffer, 4, size).array();
//
//            Paxos.Message m = Paxos.Message.parseFrom(buf);
//            this.message = m;
//        } catch(UnknownHostException u) {
//            java.lang.System.out.println(u);
//        } catch(IOException i) {
//            java.lang.System.out.println(i);
//        }
//        try {
//            input.close();
//            serverSocket.close();
//        } catch(IOException i) {
//            java.lang.System.out.println(i);
//        }
//    }

    public void start(){
        App app = new App(this);
        abstractions.put(app.getId(), app);
        while(true) {
            eventloop();
           // java.lang.System.out.println("Event loop thread");
//            Paxos.Message m = this.message.getNetworkMessage().getMessage();
//            if(m.getType().equals(Paxos.Message.Type.APP_PROPOSE))
//                trigger(m);
//            else {
//                trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_DELIVER).
//                        setAbstractionId(m.getAbstractionId()).setPlDeliver(Paxos.PlDeliver.newBuilder().
//                        setMessage(m.getPlDeliver().getMessage()).
//                        setSender(m.getPlDeliver().getSender()).build()).build());
//            }
        }
    }

    public void eventloop(){
        boolean handled;
        for(Paxos.Message m : events){
           // java.lang.System.out.println("Event: " + m.getType());
            handled = false;
            for(Map.Entry<String, Abstraction> a : abstractions.entrySet()){
                handled = a.getValue().handle(m);
               // java.lang.System.out.println(handled);
            }
            if(handled){
                events.remove(m);
            }
        }
    }

}
