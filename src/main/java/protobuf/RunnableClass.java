package protobuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;

class RunnableClass implements java.lang.Runnable {

    System system;
    public RunnableClass(System system){ this.system = system; }

    @Override
    public void run() {
            ServerSocket serverSocket = null;
            InputStream input = null;
            java.lang.System.out.println("Network thread");
            try {
                serverSocket = new ServerSocket(system.getPort());
                while(true) {
                    Socket socket = serverSocket.accept();
                    input = socket.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte buffer[] = new byte[1024];
                    baos.write(buffer, 0, input.read(buffer));
                    byte result[] = baos.toByteArray();

                    ByteBuffer bBuf = ByteBuffer.wrap(result);
                    byte[] b = new byte[4];
                    bBuf.get(b, 0, b.length);
                    ByteBuffer newBBuf = ByteBuffer.wrap(b);
                    int size = newBBuf.getInt();

                    byte[] buf = ByteBuffer.allocate(size).put(buffer, 4, size).array();

                    bBuf.clear();
                    newBBuf.clear();

                   // java.lang.System.out.println(size);

                    Paxos.Message ms = Paxos.Message.parseFrom(buf);
                    Paxos.Message m = ms.getNetworkMessage().getMessage();
                    if (m.getType().equals(Paxos.Message.Type.APP_PROPOSE))
                        system.events.add(m);
                    else {
                        //system.trigger(m);
                        system.events.add(Paxos.Message.newBuilder().setType(Paxos.Message.Type.PL_DELIVER).
                                setAbstractionId(ms.getAbstractionId()).setSystemId(system.id).setPlDeliver(Paxos.PlDeliver.newBuilder().
                                setMessage(m).
                                setSender(Paxos.ProcessId.newBuilder().setHost(ms.getNetworkMessage().getSenderHost()).
                                setPort(ms.getNetworkMessage().getSenderListeningPort()).build()).build()).build());
                    }
                }
            } catch (UnknownHostException u) {
                java.lang.System.out.println(u);
            } catch (IOException i) {
                java.lang.System.out.println(i);
            }
            try {
                input.close();
                serverSocket.close();
            } catch (IOException i) {
                java.lang.System.out.println(i);
            }
        }
}