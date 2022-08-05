package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivateKey;

import dslab.ComponentFactory;
import dslab.mailbox.tcp.ListenerThread;
import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;
import dslab.util.Keys;

public class MailboxServer implements IMailboxServer, Runnable {


    private ServerSocket serverSocketDMEP, serverSocketDMAP;
    public String componentId;
    public Config config, userConfig;
    private InputStream in;
    private PrintStream out;
    public enum Protocol {DMAP, DMEP}
    public MailBoxDB db;
    public PrivateKey privateKey;
    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        this.db = new MailBoxDB();
        this.userConfig = new Config(config.getString("users.config"));

        try {
            File file = new File("./keys/server/" + componentId);
            privateKey = Keys.readPrivateKey(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            serverSocketDMEP = new ServerSocket(config.getInt("dmep.tcp.port"));
            new ListenerThread(serverSocketDMEP, this,  Protocol.DMEP).start();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
            //System.err.println("Error while creating server socket");
        }

        try {
            serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
            new ListenerThread(serverSocketDMAP, this, Protocol.DMAP).start();
        } catch (IOException e) {
            //System.err.println("Error while creating server socket");
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
            INameserverRemote remote = (INameserverRemote) registry.lookup(config.getString("root_id"));
            String IP = "127.0.0.1"; // TODO: ich bin nicht sicher
            remote.registerMailboxServer(config.getString("domain"), IP+":"+config.getString("dmep.tcp.port"));
        } catch (RemoteException e) {
            System.err.println("Error while obtaining registry/server-remote-object.");
        } catch (NotBoundException e) {
            System.err.println("Error while looking for server-remote-object.");
        } catch (InvalidDomainException e) {
            System.err.println("InvalidDomainException");
        } catch (AlreadyRegisteredException e) {
            System.err.println("AlreadyRegisteredException");
        }


        out.println("Server is up! Hit <ENTER> to exit!");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            reader.readLine();
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

        // close socket and listening thread
        shutdown();
    }

    @Override
    public void shutdown() {
        if (serverSocketDMAP != null) {
            try {
                serverSocketDMAP.close();
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }

        if (serverSocketDMEP != null) {
            try {
                serverSocketDMEP.close();
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
