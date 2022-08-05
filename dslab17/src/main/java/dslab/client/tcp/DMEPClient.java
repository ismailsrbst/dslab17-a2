package dslab.client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class DMEPClient {

    private String host, email;
    private int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public DMEPClient(String host, int port, String email) {
        this.host = host;
        this.port = port;
        this.email = email;
    }

    public boolean connect(){
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            return read().equals("ok DMEP2.0");
        }
        catch (IOException e){}
        return false;
    }

    private String read() throws IOException {
        return reader.readLine();
    }

    private String sendAndRead(String message) throws IOException {
        writer.println(message);
        writer.flush();
        return reader.readLine();
    }

    public String sendMail(String to, String subject, String data, String hash){

        try {
            String r = sendAndRead("begin");
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("to " + to);
            if(!r.startsWith("ok")){ return r; }
            r = sendAndRead("from " + email);
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

    public void close() {
        try {
            if(socket!=null && !socket.isClosed()){
                sendAndRead("quit");
                socket.close();
            }
        } catch (IOException e) {}
    }

}
