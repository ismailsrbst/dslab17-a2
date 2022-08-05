package dslab.client.tcp;

import dslab.util.Keys;
import org.bouncycastle.util.encoders.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;


public class DMAPClient {

    private String host, user, password;
    private int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private Cipher sessionEncoder, sessionDecoder;

    public DMAPClient(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public boolean connect() throws IOException{
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream());
        return read().equals("ok DMAP2.0");
    }

    public boolean startsecure() throws Exception{
        String s = sendAndRead("startsecure");
        if(!s.startsWith("ok")){
            return false;
        }
        String componentId = s.substring(3);

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(32*8); // KEYSIZE is in bits
        SecretKey secretKey = generator.generateKey();

        SecureRandom secureRandom = new SecureRandom();
        final byte[] iv_number = new byte[16];
        final byte[] client_chalenge = new byte[32];
        secureRandom.nextBytes(iv_number);
        secureRandom.nextBytes(client_chalenge);

        IvParameterSpec iv = new IvParameterSpec(iv_number);

        sessionEncoder = Cipher.getInstance("AES/CTR/NoPadding","BC");
        sessionEncoder.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        sessionDecoder = Cipher.getInstance("AES/CTR/NoPadding","BC");
        sessionDecoder.init(Cipher.DECRYPT_MODE, secretKey, iv);


        PublicKey publicKey = Keys.readPublicKey(new File("./keys/client/" + componentId + ".pub"));

        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        String str_client_chalenge = new String(Base64.encode(client_chalenge));
        String msg = String.format("ok %s %s %s",
                str_client_chalenge,
                new String(Base64.encode(secretKey.getEncoded())),
                new String(Base64.encode(iv_number))
        );

        msg = new String(Base64.encode(cipher.doFinal(msg.getBytes())));

        s = decode(sendAndRead(msg));
        if(!s.startsWith("ok ") || !s.substring(3).equals(str_client_chalenge)){
            return false;
        }

        send(encode("ok"));
        return true;
    }

    public boolean login() throws Exception {
        String s = sendAndReadSecure(String.format("login %s %s", user, password));
        return s.equals("ok");
    }

    public boolean connectAndSecureLogin() throws Exception{
        return connect() && startsecure() && login();
    }

    public String encode(String s) throws BadPaddingException, IllegalBlockSizeException {
        return new String(Base64.encode(sessionEncoder.doFinal(s.getBytes())));
    }

    public String decode(String s) throws BadPaddingException, IllegalBlockSizeException {
        return new String(sessionDecoder.doFinal(Base64.decode(s.getBytes())));
    }

    public void send(String s){
        writer.println(s);
        writer.flush();
    }

    public String read() throws IOException {
        return reader.readLine();
    }

    public String sendAndRead(String s) throws IOException {
        send(s);
        return read();
    }

    public String readSecure() throws Exception {
        return decode(read());
    }

    public String sendAndReadSecure(String s) throws Exception {
        return decode(sendAndRead(encode(s)));
    }

    public void close(){
        try {
            socket.close();
        } catch (IOException e) {

        }
    }

}
