package dslab.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * Reads encryption keys from the file system.
 */
public final class Keys {

    private Keys() {
        // util class
    }

    /**
     * Reads an RSA private key from the given file.
     *
     * @param file the file containing the RSA key
     * @return a PrivateKey instance
     * @throws IOException if an exception occurred while reading the key file
     * @throws IllegalStateException if an error occurred in the java security api
     */
    public static PrivateKey readPrivateKey(File file) throws IOException, IllegalStateException {
        try (PemReader reader = new PemReader(new FileReader(file))) {
            PemObject pemObject = reader.readPemObject();
            return createPrivateKey(new PKCS8EncodedKeySpec(pemObject.getContent()));
        }
    }

    /**
     * Reads an RSA public key from the given file.
     *
     * @param file the file containing the RSA key
     * @return a PublicKey instance
     * @throws IOException if an exception occurred while reading the key file
     * @throws IllegalStateException if an error occurred in the java security api
     */
    public static PublicKey readPublicKey(File file) throws IOException, IllegalStateException {
        try (PemReader reader = new PemReader(new FileReader(file))) {
            PemObject pemObject = reader.readPemObject();
            return createPublicKey(new X509EncodedKeySpec(pemObject.getContent()));
        }
    }

    /**
     * Reads the {@link SecretKeySpec} from the given location which is expected to contain a HMAC SHA-256 key.
     *
     * @param file the path to key located in the file system
     * @return the secret key
     * @throws IOException if an I/O error occurs or the security provider cannot handle the file
     */
    public static SecretKeySpec readSecretKey(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] keyBytes = new byte[1024];
            if (in.read(keyBytes) < 0) {
                throw new IOException(String.format("Cannot read key file %s.", file.getCanonicalPath()));
            }
            byte[] input = Hex.decode(keyBytes);
            return new SecretKeySpec(input, "HmacSHA256");
        }
    }

    private static PrivateKey createPrivateKey(KeySpec spec) {
        try {
            return getRsaKeyFactory().generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Error creating private key", e);
        }
    }

    private static PublicKey createPublicKey(KeySpec spec) {
        try {
            return getRsaKeyFactory().generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Error creating private key", e);
        }
    }

    private static KeyFactory getRsaKeyFactory() throws IllegalStateException {
        try {
            return KeyFactory.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IllegalStateException("Error creating RSA key factory with bouncycastle", e);
        }
    }

}
