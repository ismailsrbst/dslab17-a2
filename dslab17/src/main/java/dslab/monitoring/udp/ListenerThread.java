package dslab.monitoring.udp;


import dslab.monitoring.MonitoringServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class ListenerThread extends Thread {

    private DatagramSocket datagramSocket;
    private MonitoringServer monitoringServer;

    public ListenerThread(DatagramSocket datagramSocket,MonitoringServer monitoringServer) {
        this.datagramSocket = datagramSocket;
        this.monitoringServer = monitoringServer;
    }

    public void run() {

        byte[] buffer;
        DatagramPacket packet;
        try {
            while (true) {
                buffer = new byte[1024];
                packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String s = new String(packet.getData());
                String parts[] = s.trim().split("\\s");
                if(parts.length==2) {
                    monitoringServer.add(parts[0], parts[1]);
                }
            }
        } catch (SocketException e) {
            // when the socket is closed, the send or receive methods of the DatagramSocket will throw a SocketException
            System.out.println("SocketException while waiting for/handling packets: " + e.getMessage());
            return;
        } catch (IOException e) {
            // other exceptions should be handled correctly in your implementation
            throw new UncheckedIOException(e);
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }

    }
}
