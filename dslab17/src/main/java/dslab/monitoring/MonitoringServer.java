package dslab.monitoring;

import java.io.*;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;
import dslab.ComponentFactory;
import dslab.monitoring.udp.ListenerThread;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private String componentId;
    public Config config;
    private InputStream in;
    private PrintStream out;
    private DatagramSocket datagramSocket;
    private HashMap<String, Integer> servers, addresses;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        this.servers = new HashMap<>();
        this.addresses = new HashMap<>();
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));
            new ListenerThread(datagramSocket, this).start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }

        //out.println("Server is up!");
        //usange();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String request;
            while ((request = reader.readLine()) != null) {

                if (request.equals("addresses")){
                    addresses();
                }
                else if (request.equals("servers")){
                    servers();
                }
                else if (request.equals("shutdown")){
                    break;
                }
                else{
                    usange();
                }

            }
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }


        shutdown();
    }

    public synchronized void add(String server, String address){
        if(!servers.containsKey(server)){
            servers.put(server,0);
        }
        servers.put(server, servers.get(server) + 1);

        if(!addresses.containsKey(address)){
            addresses.put(address,0);
        }
        addresses.put(address, addresses.get(address) + 1);
    }


    public void usange() {
        out.println("usange : addresses | servers | shutdown");
    }


    @Override
    public void addresses() {
        if(addresses.isEmpty()){
            out.println("addresses list empty");
        }
        else {
            for (Map.Entry<String, Integer> entry : addresses.entrySet()) {
                out.printf("%s %d\n", entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void servers() {
        if(servers.isEmpty()){
            out.println("servers list empty");
        }
        else {
            for (Map.Entry<String, Integer> entry : servers.entrySet()) {
                out.printf("%s %d\n", entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void shutdown() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
