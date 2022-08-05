package dslab.mailbox.tcp;

import dslab.mailbox.MailBoxDB;
import dslab.mailbox.MailboxServer;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;


public class DMAPThread extends Thread {

    private Socket socket;
    private boolean hasLogin, hasQuit;
    private String username, mailadresse;
    private MailboxServer mailboxServer;
    private byte handSchakeStep = 0;
    private Cipher sessionEncoder, sessionDecoder;

    public DMAPThread(Socket socket, MailboxServer mailboxServer) {
        this.socket = socket;
        this.mailboxServer = mailboxServer;
    }

    private void reset(){
        hasLogin = false;
        hasQuit = false;
        username = null;
        mailadresse = null;
    }

    private String cmdLogin(String request){
        String[] parts = request.split("\\s");
        if(parts.length!=3){ return "error protocol error"; }

        if(!mailboxServer.userConfig.containsKey(parts[1])){
            return "error unknown user";
        }
        else if (!mailboxServer.userConfig.getString(parts[1]).equals(parts[2])){
            return "error wrong password";
        }
        else{
            reset();
            hasLogin = true;
            username = parts[1];
            mailadresse = username + "@" + mailboxServer.config.getString("domain");
            return "ok";
        }
    }

    private String cmdList(){
        if(!hasLogin){ return "error not logged in"; }
        String res = "";
        for (MailBoxDB.Mail mail : mailboxServer.db.getByAccount(mailadresse)){
            res += mail.id + " " + mail.from + " " + mail.subject + "\n";
        }
        res += "ok";
        return res.trim();
    }

    private String cmdShow(String request){
        if(!hasLogin){ return "error not logged in"; }
        String[] parts = request.split("\\s");
        if(parts.length!=2){ return "error protocol error"; }

        MailBoxDB.Mail m = mailboxServer.db.getByID(Integer.parseInt(parts[1]));
        if(m==null){ return "error wrong id"; }
        if(!m.account.equals(mailadresse)){ return "error wrong authorization"; }

        return String.format("from %s\nto %s\nsubject %s\ndata %s\nhash %s\nok", m.from, m.to, m.subject, m.data, m.hash);
    }

    private String cmdDelete(String request){
        if(!hasLogin){ return "error not logged in"; }
        String[] parts = request.split("\\s");
        if(parts.length!=2){ return "error protocol error"; }

        MailBoxDB.Mail m = mailboxServer.db.getByID(Integer.parseInt(parts[1]));
        if(m==null){ return "error wrong id"; }
        if(!m.account.equals(mailadresse)){ return "error wrong authorization"; }
        mailboxServer.db.remove(m.id);
        return "ok";
    }

    private String cmdLogout(){
        if(!hasLogin){ return "error not logged in"; }
        reset();
        return "ok";
    }

    private String cmdQuit(){
        reset();
        hasQuit = true;
        return "ok bye";
    }

    private String parseRequest(String request){
        if (request.startsWith("startsecure")){
            return cmdStartSecure();
        }
        else if (request.startsWith("login")){
            return cmdLogin(request);
        }
        else if (request.startsWith("list")){
            return cmdList();
        }
        else if (request.startsWith("show")){
            return cmdShow(request);
        }
        else if (request.startsWith("delete")){
            return cmdDelete(request);
        }
        else if (request.startsWith("logout")){
            return cmdLogout();
        }
        else if (request.startsWith("quit")){
            return cmdQuit();
        }
        return null;
    }

    private String cmdStartSecure(){
        handSchakeStep=2;
        return "ok " + mailboxServer.componentId;
    }


    private String sessionInit(byte[] encryptedRequest) throws Exception{
        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, mailboxServer.privateKey);
        byte[] dectyptedMessage = cipher.doFinal(encryptedRequest);
        String[] parts = new String(dectyptedMessage).split(" ");
        SecretKey secretKey = new SecretKeySpec(Base64.decode(parts[2]), "AES");
        IvParameterSpec iv = new IvParameterSpec(Base64.decode(parts[3]));

        sessionEncoder = Cipher.getInstance("AES/CTR/NoPadding","BC");
        sessionEncoder.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        sessionDecoder = Cipher.getInstance("AES/CTR/NoPadding","BC");
        sessionDecoder.init(Cipher.DECRYPT_MODE, secretKey, iv);

        handSchakeStep = 4;
        return "ok " + parts[1];
    }


    private String cmdSecure(String request) throws Exception {
        String response = null;
        byte[] encryptedRequest = Base64.decode(request.getBytes());

        if(handSchakeStep==2){
            response = sessionInit(encryptedRequest);
        }
        else if(handSchakeStep==4){
            request = new String(sessionDecoder.doFinal(encryptedRequest));
            if(request.equals("ok")){
               handSchakeStep = 5;
            }
            else {
                throw new Exception("Protocol Error");
            }
        }
        else if(handSchakeStep==5){
            request = new String(sessionDecoder.doFinal(encryptedRequest));
            response = parseRequest(request);
        }
        else {
            throw new Exception("Protocol Error");
        }

        if(response!=null) {
            response = new String(Base64.encode(sessionEncoder.doFinal(response.getBytes())));
        }

        return response;

    }

    public void run() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.println("ok DMAP2.0");
            writer.flush();

            String request, response;
            while (!hasQuit && (request = reader.readLine()) != null) {

                if(handSchakeStep!=0) {
                    try {
                        response = cmdSecure(request);
                    } catch (Exception e) {
                        break;
                    }
                }
                else{
                    response = parseRequest(request);
                }

                if(response!=null){
                    for (String line : response.split("\\n")) {
                        writer.println(line);
                    }
                    writer.flush();
                }
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
