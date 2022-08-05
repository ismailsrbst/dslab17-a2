package dslab.transfer.tcp;

import dslab.transfer.*;
import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;


public class DMEPThread extends Thread {

    private Socket socket;
    private TransferServer transferServer;

    private boolean hasBegin, hasRecipient, hasFrom, hasSubject, hasData, hasSend, hasQuit;
    private String recipients[];
    private String from, subject, data, hash;

    public DMEPThread(Socket socket, TransferServer transferServer) {
        this.socket = socket;
        this.transferServer = transferServer;
    }

    private boolean validDomain(String domain){
        return true;
        /*try {
            return transferServer.domainLookup(domain) != null;
        } catch (RemoteException e) {}
        return false;*/
    }

    private void reset(){
        hasBegin = false;
        hasRecipient = false;
        hasFrom = false;
        hasSubject = false;
        hasData = false;
        hasSend = false;
        recipients = null;
        from = null;
        subject = null;
        data = null;
    }

    private String cmdBegin(){
        reset();
        hasBegin = true;
        return "ok";
    }

    private String cmdTo(String request){
        if(!hasBegin){ return "error: begin"; }
        if(request.length()<4){
            return "error";
        }

        String[] addresses = request.substring(3).split(",");
        recipients = new String[addresses.length];
        for(int i=0; i<addresses.length; i++){
            recipients[i] = addresses[i].trim();
            String[] parts = recipients[i].split("@");

            if(parts.length!=2){
                return "error invalid mail : " + recipients[i];
            }
            else if (!validDomain(parts[1])){
                return "error invalid domain : " + parts[1];
            }



        }
        hasRecipient = true;
        return "ok " + recipients.length;
    }

    private String cmdFrom(String request){
        if(!hasBegin){ return "error: begin"; }
        if(request.length()<6){
            return "error";
        }
        from = request.substring(5);
        String[] parts = from.split("@");
        if(parts.length!=2){
            return "error invalid mail";
        }
        else if (!validDomain(parts[1])){
            return "error invalid domain : " + parts[1];
        }
        hasFrom = true;
        return "ok";
    }

    private String cmdSubject(String request){
        if(!hasBegin){ return "error: begin"; }
        if(request.length()<9){
            return "error";
        }
        subject = request.substring(8);
        hasSubject = true;
        return "ok";
    }

    private String cmdHash(String request){
        if(!hasBegin){ return "error: begin"; }

        if(request.length()<6){
            return "error";
        }
        hash = request.substring(5);

        return "ok";
    }

    private String cmdData(String request){
        if(!hasBegin){ return "error: begin"; }

        if(request.length()<6){
            return "error";
        }
        data = request.substring(5);
        hasData = true;

        return "ok";
    }

    private String cmdSend(){
        if(!hasBegin){ return "error: begin"; }
        if(!hasRecipient){ return "error: no recipient"; }
        if(!hasFrom){ return "error: no sender"; }
        if(!hasSubject){ return "error: no subject"; }
        if(!hasData){ return "error: no data"; }

        // <Domain, Mail>
        HashMap<String, String> map = new HashMap<>();
        for (String recipient : recipients){
            String domain = recipient.split("@")[1];
            if(!map.containsKey(domain)) {
                map.put(domain, recipient);
            }
            else {
                map.put(domain, map.get(domain) + "," + recipient);
            }
        }

        for(Map.Entry<String, String> entry : map.entrySet()) {
            String domain = entry.getKey();
            String to = entry.getValue();
            MailboxClient client = new MailboxClient(transferServer, domain, from, to , subject, data, hash);
            client.start();

        }

        return "ok";
    }

    private String cmdQuit(){
        reset();
        hasQuit = true;
        return "ok bye";
    }

    public void run() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.println("ok DMEP2.0");
            writer.flush();

            String request;
            while (!hasQuit && (request = reader.readLine()) != null) {
                String response = "error";

                if (request.startsWith("begin")){
                    response = cmdBegin();
                }
                else if (request.startsWith("to")){
                    response = cmdTo(request);
                }
                else if (request.startsWith("from")){
                    response = cmdFrom(request);
                }
                else if (request.startsWith("subject")){
                    response = cmdSubject(request);
                }
                else if (request.startsWith("data")){
                    response = cmdData(request);
                }
                else if (request.startsWith("hash")){
                    response = cmdHash(request);
                }
                else if (request.startsWith("send")){
                    response = cmdSend();
                }
                else if (request.startsWith("quit")){
                    response = cmdQuit();
                }

                writer.println(response);
                writer.flush();
            }

        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            System.out.println("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            // you should properly handle all other exceptions
            throw new UncheckedIOException(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }

        }
    }
}
