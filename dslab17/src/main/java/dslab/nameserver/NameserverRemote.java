package dslab.nameserver;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class NameserverRemote implements INameserverRemote {

    private HashMap<String, INameserverRemote> nameservers;
    private HashMap<String, String> mailBoxServers;
    private static final Pattern pattern = Pattern.compile("^([a-z]+\\.)*[a-z]+$");

    public NameserverRemote() {
        nameservers = new HashMap<>();
        mailBoxServers = new HashMap<>();
    }

    private String[] domainZones(String domain) throws InvalidDomainException{
        domain = domain.trim().toLowerCase();
        if (!pattern.matcher(domain).find()){
            throw new InvalidDomainException(domain);
        }
        String[] zones = domain.split("\\.");
        if(zones.length>1 && !nameservers.containsKey(zones[zones.length-1])){
            throw new InvalidDomainException(domain);
        }
        return zones;
    }

    private String subDomain(String[] zones){
        return String.join(".", Arrays.copyOf(zones, zones.length - 1));
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String[] zones = domainZones(domain);
        if(zones.length == 1){
            if(nameservers.containsKey(zones[0])){
                throw new AlreadyRegisteredException(domain);
            }
            nameservers.put(zones[0], nameserver);
        }
        else {
            getNameserver(zones[zones.length-1]).registerNameserver(subDomain(zones), nameserver);
        }
    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String[] zones = domainZones(domain);
        if(zones.length == 1){
            if(mailBoxServers.containsKey(zones[0])){
                throw new AlreadyRegisteredException(domain);
            }
            mailBoxServers.put(zones[0], address);
        }
        else {
            getNameserver(zones[zones.length-1]).registerMailboxServer(subDomain(zones), address);
        }
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return nameservers.get(zone);
    }

    @Override
    public String lookup(String name) throws RemoteException {
        return mailBoxServers.get(name);
    }

    public String[] getNameservers(){
        String[] ret = new String[nameservers.size()];
        int i = 0;
        for(Map.Entry<String, INameserverRemote> entry : nameservers.entrySet()) {
            ret[i++] = entry.getKey();
        }
        return ret;
    }


    public String[] getAddresses(){
        String[] ret = new String[mailBoxServers.size()];
        int i = 0;
        for(Map.Entry<String, String> entry : mailBoxServers.entrySet()) {
            ret[i++] = entry.getKey() + " " + entry.getValue();
        }
        return ret;
    }

    public static void main(String[] args) throws InvalidDomainException, RemoteException, AlreadyRegisteredException {

        String domain = "planet";
       /* String[] zones = domain.split("\\.");
        for (String s :zones) {
            System.out.println(s);
        }
*/
        NameserverRemote root = new NameserverRemote();
        NameserverRemote planet = new NameserverRemote();

        root.registerNameserver("planet", planet);
        for (String s : root.getNameservers()){
            System.out.println(s);
        }

    }
}
