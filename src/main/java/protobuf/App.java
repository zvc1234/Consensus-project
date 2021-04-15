package protobuf;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public class App implements Abstraction {

    System system;

    public App(System system){
        this.system = system;
    }

    @Override
    public String getId() {
        return "app";
    }

    @Override
    public boolean handle(Paxos.Message message) {
        switch (message.getType()){
            case APP_PROPOSE:
                java.lang.System.out.println("Through app propose");
                appPropose(message.getAppPropose().getValue(), message.getAppPropose().getProcessesList());
                return true;
            case UC_DECIDE:
                java.lang.System.out.println("Through uc decide");
                appUcDecide(message.getUcDecide().getValue());
                return true;
        }
        return false;
    }

    public void appPropose(Paxos.Value v, List<Paxos.ProcessId> processes){
        system.setProcesses(processes);
        system.registerAbstractions();
        system.trigger(Paxos.Message.newBuilder().setType(Paxos.Message.Type.UC_PROPOSE).
                setAbstractionId("app").setMessageUuid("app_apppropose").setSystemId(system.id).
                setUcPropose(Paxos.UcPropose.newBuilder().setValue(v).build()).build());
    }

    public void appUcDecide(Paxos.Value v){
        Socket socket = null;
        OutputStream out = null;
        try {
            socket = new Socket("127.0.0.1", 5000);
            out = socket.getOutputStream();
            Paxos.Message m = Paxos.Message.newBuilder().setType(Paxos.Message.Type.NETWORK_MESSAGE).
                    setNetworkMessage(Paxos.NetworkMessage.newBuilder().setSenderHost("127.0.0.1").setSenderListeningPort(system.self.id.getPort()).setMessage(Paxos.Message.newBuilder().
                            setType(Paxos.Message.Type.APP_DECIDE).setAppDecide(Paxos.AppDecide.newBuilder().setValue(v).
                            build())).build()).build();

            byte[] buffer = m.toByteArray();
            int size = buffer.length;
            byte[] b = ByteBuffer.allocate(size).putInt(0,size).put(4,buffer).array();
            out.write(b);

        } catch(UnknownHostException u) {
            java.lang.System.out.println(u);
        } catch(IOException i) {
            java.lang.System.out.println(i);
        }
        try {
            out.close();
            socket.close();
        }
        catch(IOException i) {
            java.lang.System.out.println(i);
        }
    }
}
