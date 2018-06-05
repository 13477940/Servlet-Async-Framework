package test.aes;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * praseodym/AESGCMUpdateAAD2.java
 * from https://gist.github.com/praseodym/f2499b3e14d872fe5b4a
 * AAD itself is not required（是否有 AAD 並不影響安全性）
 * https://crypto.stackexchange.com/questions/35727/does-aad-make-gcm-encryption-more-secure
 * https://blog.csdn.net/Vieri_32/article/details/48345023
 */
public class AES_256_GCM {

	private SecretKey key = null;

	private Cipher cipher = null;
	private GCMParameterSpec spec = null;

	private final int GCM_NONCE_LENGTH = 12; // in bytes
    private final int GCM_TAG_LENGTH = 16; // in bytes

    public AES_256_GCM(SecretKey key) {
    	try {
    		this.key = key;
    		this.cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
    		{
	    		byte[] nonce = new byte[GCM_NONCE_LENGTH];
				this.spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
    		}
		} catch(Exception e) {
			e.printStackTrace();
		}
    }

	public String getKey() {
		return Base64.getEncoder().withoutPadding().encodeToString(this.key.getEncoded());
	}

	public String encrypt(CharSequence charSequence) {
		byte[] cipherText = null;
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, this.spec);
			{ // 如果有 updateAAD 則加解密都要啟用
				byte[] tag = new byte[GCM_TAG_LENGTH];
	            cipher.updateAAD(tag);
			}
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
			this.cipher.init(Cipher.DECRYPT_MODE, this.key, this.spec);
			{ // 如果有 updateAAD 則加解密都要啟用
				byte[] tag = new byte[GCM_TAG_LENGTH];
	            cipher.updateAAD(tag);
			}
	        cipherText = this.cipher.doFinal(encryptBytes);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if(null == cipherText) return null;
		return new String(cipherText, StandardCharsets.UTF_8);
	}

	public static class Builder {

		private final int AES_KEY_SIZE = 256; // key size
		private SecretKey key = null;

		public AES_256_GCM.Builder newKey() {
			try {
				SecureRandom random = SecureRandom.getInstanceStrong();
		        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		        keyGen.init(AES_KEY_SIZE, random);
		        this.key = keyGen.generateKey();
			} catch(Exception e) {
				e.printStackTrace();
			}
			return this;
		}

		public AES_256_GCM.Builder setKey(CharSequence charSequence) {
			if(null == charSequence) return this;
			byte[] tmp = Base64.getDecoder().decode(charSequence.toString());
			this.key = new SecretKeySpec(tmp, 0, tmp.length, "AES");
			return this;
		}

		public AES_256_GCM build() {
			return new AES_256_GCM(key);
		}

	}

}
