package dslab.mailbox.tcp;


import dslab.mailbox.MailboxServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class ListenerThread extends Thread {

    private ServerSocket serverSocket;
    private MailboxServer mailboxServer;
    private MailboxServer.Protocol protocol;

    public ListenerThread(ServerSocket serverSocket, MailboxServer mailboxServer, MailboxServer.Protocol protocol) {
        this.serverSocket = serverSocket;
        this.protocol = protocol;
        this.mailboxServer = mailboxServer;
    }

    public void run() {

        while (true) {
            try {
                Socket socket = serverSocket.accept();

                if (protocol == MailboxServer.Protocol.DMAP){
                    new DMAPThread(socket, mailboxServer).start();
                }
                else if (protocol == MailboxServer.Protocol.DMEP){
                    new DMEPThread(socket, mailboxServer).start();
                }

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
