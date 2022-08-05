package dslab.transfer.tcp;

import dslab.transfer.TransferServer;
import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;

public class MailboxClient extends Thread {

    private String domain, host;
    private int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connect, deliveryFailed;
    private TransferServer transferServer;
    private String from, to, subject, data, hash;


    public MailboxClient(TransferServer transferServer, String domain, String from, String to, String subject, String data, String hash) {
        this.transferServer = transferServer;
        this.domain = domain;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.data = data;
        this.hash = hash;
        this.deliveryFailed = false;
    }

    private void deliveryFailed(){
        deliveryFailed = true;
    }

    private boolean domainLookup() {
        try {
            String adress = transferServer.domainLookup(domain);
            if (adress != null) {
                String parts[] = adress.split(":");
                if (parts.length == 2) {
                    host = parts[0];
                    port = Integer.parseInt(parts[1]);
                    return true;
                }
            }
        } catch (RemoteException e) {return false;}
        return false;
    }

    private boolean connect(){
        if(!connect){
            if(domainLookup()){
                try {
                    socket = new Socket(host, port);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(socket.getOutputStream());
                    connect = read().equals("ok DMEP2.0");
                }
                catch (IOException e){
                    connect = false;
                }
            }
        }
        return connect;
    }

    private String read() throws IOException {
        return reader.readLine();
    }

    private String sendAndRead(String message) throws IOException {
        writer.println(message);
        writer.flush();
        return reader.readLine();
    }

    private String sendMail(){

        if(!connect()){
            return "error not connected to mailboxserver : " + domain;
        }

        try {
            String r = sendAndRead("begin");
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("to " + to);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("from " + from);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("subject " + subject);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("data " + data);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("hash " + hash);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("send");
            if(!r.startsWith("ok")){ return r; }

        } catch (IOException e) {
            return "error " + e.getMessage();
        }

        return "ok";
    }

    private void close() {
        try {
            if(socket!=null && !socket.isClosed()){
                if(connect){
                    sendAndRead("quit");
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String res = sendMail();
        transferServer.sendStatistic(host, port, from);

        if(!deliveryFailed && !res.startsWith("ok")){
            domain = from.split("@")[1];
            data =  to + " " + res + " ";
            subject = "Mail delivery failed : " + subject;
            MailboxClient client = new MailboxClient(transferServer, domain, from, from, subject, data, hash);
            client.deliveryFailed();
            client.start();
        }
        close();
    }
}
