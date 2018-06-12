package nl.bigbrotter.androidclient.Helpers;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by Martijn on 11-6-2018.
 */

public class EncryptionHelper {

    private KeyPairGenerator kpg;
    private KeyPair kp;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] encryptedBytes, decryptedBytes;
    private Cipher cipherEncrypt, cipherDecrypt;
    private String encrypted, decrypted;

    public byte[] Encrypt (byte[] bytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();

        cipherEncrypt = Cipher.getInstance("RSA");
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, privateKey);
        encryptedBytes = cipherEncrypt.doFinal(bytes);

        return encryptedBytes;
    }

    public byte[] Decrypt (byte[] encryptedBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        cipherDecrypt = Cipher.getInstance("RSA");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, publicKey);
        decryptedBytes = cipherDecrypt.doFinal(encryptedBytes);
        return decryptedBytes;
    }
}
