package faurecia.efcs.encryption;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.matrixone.fcs.common.Logger;

public class FCSEncryption {

    protected static final String S_CIPHER = "AES/CFB8/NoPadding";

    protected final SecretKey key;

    protected final IvParameterSpec iv;

    public FCSEncryption(String key, String salt) throws GeneralSecurityException {
        byte[] biv = new byte[16];
        byte[] bsalt = salt.getBytes();
        for (int i = 0; i < 16 && i < salt.length(); i++) {
            biv[i] = bsalt[i];
        }
        try {
            this.key = getSecretKey(key.toCharArray(), bsalt);
            this.iv = new IvParameterSpec(biv);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Logger.log(e);
            throw e;
        }
    }

    public static SecretKey getSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt, 1024, 128);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public InputStream decrypt(InputStream inputStream) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        final Cipher cipher = Cipher.getInstance(S_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, this.key, this.iv);
        return new CipherInputStream(inputStream, cipher);
    }

    public OutputStream encrypt(OutputStream outputStream) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        final Cipher cipher = Cipher.getInstance(S_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, this.key, this.iv);
        return new CipherOutputStream(outputStream, cipher);
    }
}
