package dslab.transfer.tcp;


import dslab.transfer.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ListenerThread extends Thread {

    private ServerSocket serverSocket;
    private TransferServer transferServer;

    public ListenerThread(ServerSocket serverSocket, TransferServer transferServer) {
        this.serverSocket = serverSocket;
        this.transferServer = transferServer;
    }

    public void run() {

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new DMEPThread(socket, transferServer).start();

            } catch (SocketException e) {
                // when the socket is closed, the I/O methods of the Socket will throw a SocketException
                // almost all SocketException cases indicate that the socket was closed
                System.out.println("SocketException while handling socket: " + e.getMessage());
                break;
            } catch (IOException e) {
                // you should properly handle all other exceptions
                throw new UncheckedIOException(e);
            }

        }
    }
}
