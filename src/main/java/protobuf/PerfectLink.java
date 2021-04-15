package protobuf;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class PerfectLink implements Abstraction{

    Socket socket;
    System system;

    public PerfectLink(System system){
        this.socket = null;
        this.system = system;
    }

    @Override
    public String getId() {
        return "pl";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case PL_SEND:
                java.lang.System.out.println("Through pl send");
                plSend(message, message.getPlSend().getDestination().getPort());
                return true;
            case PL_DELIVER:
                java.lang.System.out.println("Through pl deliver");
                plDeliver(message.getPlDeliver().getMessage());
                return true;
        }
        return false;
    }

    public void plSend(Paxos.Message message, int port){
        OutputStream out = null;
        try {
            socket = new Socket("127.0.0.1", port);
            Paxos.Message m = Paxos.Message.newBuilder().setType(Paxos.Message.Type.NETWORK_MESSAGE).
                    setAbstractionId(message.getAbstractionId()).setSystemId(message.getSystemId()).
                    setNetworkMessage(Paxos.NetworkMessage.newBuilder().setMessage(message.getPlSend().getMessage()).
                            setSenderListeningPort(system.self.id.getPort()).build()).build();
            byte[] buffer = m.toByteArray();
            int size = buffer.length;
            byte[] b = ByteBuffer.allocate(1024).putInt(0,size).put(4,buffer).array();
            out = socket.getOutputStream();
            out.write(b);
            //out = socket.getOutputStream();
        }
        catch(UnknownHostException u)
        {
            java.lang.System.out.println(u);
        }
        catch(IOException i)
        {
            java.lang.System.out.println(i);
        }
        try
        {
            out.close();
            socket.close();
        }
        catch(IOException i)
        {
            java.lang.System.out.println(i);
        }
    }

    public void plDeliver(Paxos.Message message) {
        system.trigger(message);
    }
//        InputStream input = null;
//        try{
//            socket = new Socket(host, port);
//            input = socket.getInputStream();
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            byte buffer[] = new byte[1024];
//            baos.write(buffer, 0 , input.read(buffer));
//            byte result[] = baos.toByteArray();
//
//            Paxos.Message message;
//
//        } catch(UnknownHostException u) {
//            java.lang.System.out.println(u);
//        } catch(IOException i) {
//            java.lang.System.out.println(i);
//        }
//        try {
//            input.close();
//            socket.close();
//        } catch(IOException i) {
//            java.lang.System.out.println(i);
//        }
//        Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_DELIVER).
//                setAbstractionId(message.getAbstractionId()).setPlDeliver(Paxos.PlDeliver.newBuilder().
//                setMessage(message.getNetworkMessage().getMessage()).build()).build();
//    }
}
