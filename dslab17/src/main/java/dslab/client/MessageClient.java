package dslab.client;

import java.io.*;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dslab.ComponentFactory;
import dslab.client.tcp.DMAPClient;
import dslab.client.tcp.DMEPClient;
import dslab.util.Config;
import dslab.util.Keys;
import org.bouncycastle.util.encoders.Base64;
import javax.crypto.Mac;

public class MessageClient implements IMessageClient, Runnable {
    public String componentId, userEmail;
    public Config config;
    private InputStream in;
    private PrintStream out;
    private final Pattern msgPattern = Pattern.compile("^msg (.*?) \"(.*?)\" \"(.*?)\"$");
    private final Pattern mailPattern = Pattern.compile("^from (.*?)\nto (.*?)\nsubject (.*?)\ndata (.*?)\nhash (.*?)\nok");

    private DMEPClient dmepClient;
    private DMAPClient dmapClient;
    private Mac hMac;

    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;

        String transfer_host = config.getString("transfer.host");
        int transfer_port = config.getInt("transfer.port");
        userEmail = config.getString("transfer.email");
        dmepClient = new DMEPClient(transfer_host,transfer_port,userEmail);

        String mailbox_host = config.getString("mailbox.host");
        int mailbox_port = config.getInt("mailbox.port");
        String mailbox_user = config.getString("mailbox.user");
        String mailbox_password = config.getString("mailbox.password");
        dmapClient = new DMAPClient(mailbox_host, mailbox_port, mailbox_user, mailbox_password);
    }

    @Override
    public void run() {
        try {
            hMac = Mac.getInstance("HmacSHA256");
            hMac.init(Keys.readSecretKey(new File("./keys/hmac.key")));
            //hMac.init(Keys.readSecretKey(new File("./keys/wrongHmac.key")));

            if(!dmapClient.connectAndSecureLogin()){
                out.println("error not login");
            }
        } catch (Exception e) {
            out.println("error " + e.getMessage());
            //e.printStackTrace(out);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String request;
            while ((request = reader.readLine()) != null) {

                if (request.equals("inbox")){
                    inbox();
                }
                else if (request.startsWith("delete")){
                    delete(request.substring(6).trim());
                }
                else if (request.startsWith("verify")){
                    verify(request.substring(6).trim());
                }
                else if (request.startsWith("msg")){
                    Matcher m = msgPattern.matcher(request);
                    if(m.find()){
                        msg(m.group(1),m.group(2),m.group(3));
                    }
                }
                else if (request.equals("shutdown")){
                    break;
                }
                else{
                    out.println("usange : inbox | delete <id> | verify <id> | msg <to> <subject> <data>");
                }
            }
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

        shutdown();
    }

    @Override
    public void inbox() {
        try {
            String[] list = dmapClient.sendAndReadSecure("list").split("\n");
            if(list.length<2){
                out.println("empty inbox");
            }
            for (int i=0; i<list.length-1; i++){
                String id = list[i].substring(0, list[i].indexOf(' '));
                out.printf("id %s\n%s\n\n", id, dmapClient.sendAndReadSecure("show " + id));
            }
        } catch (Exception e) {
            out.println("error " + e.getMessage());
        }
    }

    @Override
    public void delete(String id) {
        try {
            out.println(dmapClient.sendAndReadSecure("delete " + id));
        } catch (Exception e) {
            out.println("error " + e.getMessage());
        }
    }

    @Override
    public void verify(String id) {
        try {
            String mail = dmapClient.sendAndReadSecure("show " + id);
            if(mail.startsWith("error")){
                out.println(mail);return;
            }

            Matcher m = mailPattern.matcher(mail.trim());
            if(!m.find() || m.groupCount()!=5){
                out.println("error parse"); return;
            }
            if(m.group(5).isEmpty()){
                out.println("error no hash"); return;
            }

            byte[] computedHash = computeHash(m.group(1),m.group(2),m.group(3),m.group(4));
            byte[] receivedHash = Base64.decode(m.group(5));
            if(MessageDigest.isEqual(computedHash, receivedHash)){
                out.println("ok");
            }
            else{
                out.println("error not equal");
            }

        } catch (Exception e) {
            out.println("error " + e.getMessage());
        }
    }

    @Override
    public void msg(String to, String subject, String data) {
       if(dmepClient.connect()){
           String hash = new String(Base64.encode(computeHash(userEmail, to, subject, data)));
           out.println(dmepClient.sendMail(to, subject, data, hash));
           dmepClient.close();
       }
       else {
           out.println("error not connection");
       }
    }

    @Override
    public void shutdown() {
        dmapClient.close();
    }

    private byte[] computeHash(String from, String to, String subject, String data){
        String msg = String.join("\n", from, to, subject, data);
        return computeHash(msg.getBytes());
    }

    private byte[] computeHash(byte[] msg){
        hMac.update(msg);
        return hMac.doFinal();
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
