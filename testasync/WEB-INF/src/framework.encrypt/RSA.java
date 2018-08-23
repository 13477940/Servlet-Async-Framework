package framework.encrypt;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * https://www.jianshu.com/p/f837840411a5
 * https://www.zhihu.com/question/25912483
 *
 * RSA 為非對稱加密，缺點為內容長度會受到加密位元數的限制，
 * 所以通常 RSA 作為相互傳輸 AES 密鑰的用途
 */
public class RSA {

    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    private RSA(PrivateKey privateKey, PublicKey publicKey, int keySize) {
        if(null != privateKey || null != publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        } else {
            buildKeyPair(keySize);
        }
    }

    public String encrypt(CharSequence content) {
        String res = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
            byte[] encrypt = cipher.doFinal(content.toString().getBytes(StandardCharsets.UTF_8));
            res = new String(Base64.getEncoder().withoutPadding().encode(encrypt), StandardCharsets.UTF_8);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public String decrypt(CharSequence encryptString) {
        String res = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            res = new String(cipher.doFinal(Base64.getDecoder().decode(encryptString.toString())), StandardCharsets.UTF_8);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public String getPrivateKey() {
        return new String(Base64.getEncoder().withoutPadding().encode(this.privateKey.getEncoded()), StandardCharsets.UTF_8);
    }

    public String getPublicKey() {
        return new String(Base64.getEncoder().withoutPadding().encode(this.publicKey.getEncoded()), StandardCharsets.UTF_8);
    }

    private void buildKeyPair(int keySize) {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
        } catch(Exception e) {
            keyPairGenerator = null;
            e.printStackTrace();
        }
        if(null == keyPairGenerator) return;
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public static class Builder {

        private PrivateKey privateKey = null;
        private PublicKey publicKey = null;
        private int keySize = 2048; // default key size

        public RSA.Builder setPrivateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public RSA.Builder setPrivateKey(CharSequence base64string) {
            try {
                byte[] buffer = Base64.getDecoder().decode(base64string.toString());
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.privateKey = keyFactory.generatePrivate(keySpec);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        public RSA.Builder setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public RSA.Builder setPublicKey(CharSequence base64string) {
            try {
                byte[] buffer = Base64.getDecoder().decode(base64string.toString());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
                this.publicKey = keyFactory.generatePublic(keySpec);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        public RSA.Builder setKeySize(int keySize) {
            this.keySize = keySize;
            return this;
        }

        public RSA build() {
            return new RSA(privateKey, publicKey, keySize);
        }

    }

}
