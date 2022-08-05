package dslab.nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import dslab.ComponentFactory;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Nameserver implements INameserver {

    private static final Log LOG = LogFactory.getLog(Nameserver.class);

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private Registry registry;
    private NameserverRemote nameserverRemote;
    private INameserverRemote iNameserverRemote;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        nameserverRemote = new NameserverRemote();

        if(config.containsKey("domain")){
            try {
                registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
                iNameserverRemote = (INameserverRemote) registry.lookup(config.getString("root_id"));
            } catch (RemoteException e) {
                throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
            } catch (NotBoundException e) {
                throw new RuntimeException("Error while looking for server-remote-object.", e);
            }

            try{
                INameserverRemote remote = (INameserverRemote) UnicastRemoteObject.exportObject(nameserverRemote, 0);

                iNameserverRemote.registerNameserver(config.getString("domain"), remote);
            } catch (AlreadyRegisteredException e) {
                throw new RuntimeException("Error while Registered", e);
            } catch (InvalidDomainException e) {
                throw new RuntimeException("InvalidDomain", e);
            } catch (RemoteException e) {
                e.printStackTrace();
                throw new RuntimeException("Error registerNameserver", e);
            }


        }
        else{
            try {
                // create and export the registry instance on localhost at the specified port
                registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
                // create a remote object of this server object
                iNameserverRemote = (INameserverRemote) UnicastRemoteObject.exportObject(nameserverRemote, 0);
                // bind the obtained remote object on specified binding name in the registry
                registry.bind(config.getString("root_id"), iNameserverRemote);
            } catch (RemoteException e) {
                throw new RuntimeException("Error while starting server.", e);
            } catch (AlreadyBoundException e) {
                throw new RuntimeException("Error while binding remote object to registry.", e);
            }
        }

        //out.printf("%s nameserver", componentId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String request;
            while ((request = reader.readLine()) != null) {

                if (request.equals("addresses")){
                    addresses();
                }
                else if (request.equals("nameservers")){
                    nameservers();
                }
                else if (request.equals("shutdown")){
                    break;
                }
                else{
                    usange();
                }
            }
        } catch (IOException e) { }

        shutdown();
    }

    @Override
    public void nameservers() {
        for (String s : nameserverRemote.getNameservers()){
            out.println(s);
        }
    }

    @Override
    public void addresses() {
        for (String s : nameserverRemote.getAddresses()){
            out.println(s);
        }
    }

    @Override
    public void shutdown() {

        try {
            // unbind the remote object so that a client can't find it anymore
            registry.unbind(config.getString("root_id"));
        } catch (Exception e) {
            System.err.println("Error while unbinding object: " + e.getMessage());
        }

        try {
            // unexport the previously exported remote object
            UnicastRemoteObject.unexportObject(nameserverRemote, true);
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException e) {
            System.err.println("Error while unexporting object: " + e.getMessage());
        }



    }

    public void usange() {
        out.println("usange : addresses | nameservers | shutdown");
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }

}
