package framework.encrypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * from https://gist.github.com/praseodym/f2499b3e14d872fe5b4a
 * https://crypto.stackexchange.com/questions/35727/does-aad-make-gcm-encryption-more-secure
 * https://blog.csdn.net/Vieri_32/article/details/48345023
 */
public class AES_256_GCM {

    private SecretKey key = null;
    private byte[] iv = null;

    private Cipher cipher = null;
    private GCMParameterSpec spec = null;

    private final int GCM_NONCE_LENGTH = 12; // in bytes
    private final int GCM_TAG_LENGTH = 16; // in bytes

    public AES_256_GCM(SecretKey key, byte[] iv) {
        try {
            this.key = key;
            if(null == key) {
                try {
                    SecureRandom random = SecureRandom.getInstanceStrong();
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(256, random); // key size
                    this.key = keyGen.generateKey();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            this.iv = iv;
            if(null == iv) {
                this.iv = getRandomString(GCM_NONCE_LENGTH).getBytes(StandardCharsets.UTF_8);
            }
            {
                this.cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
                this.spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, this.iv);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getKey() {
        return Base64.getEncoder().withoutPadding().encodeToString(this.key.getEncoded());
    }

    public void setKey(CharSequence charSequence) {
        if(null == charSequence) {
            this.key = null;
            return;
        }
        byte[] tmp = Base64.getDecoder().decode(charSequence.toString());
        this.key = new SecretKeySpec(tmp, 0, tmp.length, "AES");
    }

    public String getIV() {
        return Base64.getEncoder().withoutPadding().encodeToString(this.iv);
    }

    public void setIV(CharSequence charSequence) {
        if(null == charSequence) {
            this.iv = null;
            return;
        }
        this.iv = Base64.getDecoder().decode(charSequence.toString());
    }

    public void updateIV() {
        this.iv = getRandomString(GCM_NONCE_LENGTH).getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(CharSequence charSequence) {
        byte[] cipherText = null;
        try {
            this.spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, this.iv);
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key, this.spec);
            cipherText = this.cipher.doFinal(charSequence.toString().getBytes(StandardCharsets.UTF_8));
        } catch(Exception e) {
            e.printStackTrace();
        }
        if(null == cipherText) return null;
        byte[] tmpCipherText = Base64.getEncoder().withoutPadding().encode(cipherText);
        return new String(tmpCipherText, StandardCharsets.UTF_8);
    }

    public String decrypt(CharSequence charSequence) {
        byte[] encryptBytes = Base64.getDecoder().decode(charSequence.toString());
        byte[] cipherText = null;
        try {
            this.spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, this.iv);
            this.cipher.init(Cipher.DECRYPT_MODE, this.key, this.spec);
            cipherText = this.cipher.doFinal(encryptBytes);
        } catch(Exception e) {
            e.printStackTrace();
        }
        if(null == cipherText) return null;
        return new String(cipherText, StandardCharsets.UTF_8);
    }

    private String getRandomString(int ranStrSize) {
        final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sbd = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for(int i = 0; i < ranStrSize; i++) {
            int iRan = random.nextInt(ranStr.length()-1);
            char t = ranStr.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    public static class Builder {

        private SecretKey key = null;
        private byte[] iv = null;

        public AES_256_GCM.Builder setKey(CharSequence charSequence) {
            if(null == charSequence) return this;
            byte[] tmp = Base64.getDecoder().decode(charSequence.toString());
            this.key = new SecretKeySpec(tmp, 0, tmp.length, "AES");
            return this;
        }

        public AES_256_GCM.Builder setIV(CharSequence charSequence) {
            if(null == charSequence) return this;
            this.iv = Base64.getDecoder().decode(charSequence.toString());
            return this;
        }

        public AES_256_GCM build() {
            return new AES_256_GCM(this.key, this.iv);
        }

    }

}
