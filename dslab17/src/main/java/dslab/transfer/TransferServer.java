package dslab.transfer;

import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import dslab.ComponentFactory;
import dslab.nameserver.INameserverRemote;
import dslab.transfer.tcp.ListenerThread;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

    private ServerSocket serverSocket;
    private String componentId;
    public Config config;
    private InputStream in;
    private PrintStream out;

    private DatagramSocket datagramSocket = null;
    private InetAddress monitoring_host;
    private int monitoring_port;

    private INameserverRemote rootNameServer;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
    }

    public void sendStatistic(String host, int port, String sender){
        byte[] buffer = String.format("%s:%d %s", host, port, sender).getBytes();
        try {
            datagramSocket.send(new DatagramPacket(buffer, buffer.length, monitoring_host, monitoring_port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String domainLookup(String domain) throws RemoteException {
        String[] zones = domain.split("\\.");
        return domainLookup(zones, zones.length-1, rootNameServer);
    }

    private String domainLookup(String[] zones, int i, INameserverRemote remote) throws RemoteException {
        if(i==0){
            return remote.lookup(zones[i]);
        }
        else if(remote!=null){
            return domainLookup(zones, i-1, remote.getNameserver(zones[i]));
        }
        return null;
    }

    @Override
    public void run() {
        try{
            datagramSocket = new DatagramSocket();
            monitoring_host = InetAddress.getByName(config.getString("monitoring.host"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }catch (SocketException e) {
            e.printStackTrace();
        }
        monitoring_port = config.getInt("monitoring.port");

        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
            new ListenerThread(serverSocket, this).start();
        } catch (IOException e) {
            //throw new UncheckedIOException("Error while creating server socket", e);
            System.err.println("Error while creating server socket");
        }

        try {
            Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
            rootNameServer = (INameserverRemote) registry.lookup(config.getString("root_id"));
        } catch (RemoteException e) {
            out.println("Error while obtaining registry/server-remote-object.");
        } catch (NotBoundException e) {
            out.println("Error while looking for server-remote-object.");
        }


        out.println("Server is up! Hit <ENTER> to exit!");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            reader.readLine();
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

        shutdown();
    }

    @Override
    public void shutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
