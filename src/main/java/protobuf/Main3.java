package protobuf;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Main3 {

    static public void main(String args[]){
        Paxos.ProcessId p = Paxos.ProcessId.newBuilder().setOwner("zasd").setHost("127.0.0.1").
                setPort(5006).setIndex(3).build();
        System system = new System("sys-1", new Process(p));
        Runnable runnable = new RunnableClass(system);
        Socket socket = null;
        OutputStream out = null;
        try {
            socket = new Socket("127.0.0.1", 5000);
            out = socket.getOutputStream();
            Paxos.Message m = Paxos.Message.newBuilder().setType(Paxos.Message.Type.NETWORK_MESSAGE).
                    setNetworkMessage(Paxos.NetworkMessage.newBuilder().setSenderHost("127.0.0.1").setSenderListeningPort(5006).setMessage(Paxos.Message.newBuilder().
                            setType(Paxos.Message.Type.APP_REGISTRATION).setAppRegistration(Paxos.AppRegistration.newBuilder().setOwner("zasd").
                            setIndex(3).build())).build()).build();

            byte[] buffer = m.toByteArray();
            int size = buffer.length;
            byte[] b = ByteBuffer.allocate(1024).putInt(0,size).put(4,buffer).array();
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

        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(){
            public void run(){
                system.start();
            }
        };


        thread2.start();
        thread1.start();
        //system.start();

    }
}
