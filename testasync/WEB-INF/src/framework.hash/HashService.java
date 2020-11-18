package framework.hash;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Formatter;

/**
 * https://tools.ietf.org/html/rfc6234
 * 雜湊碼標準中英文大小寫不敏感，但預設為 LowerCase 輸出
 */
public abstract class HashService {

    HashService() {}

    public String stringToSHA512(String content) {
        return stringToHash("SHA-512", content);
    }

    public String stringToSHA384(String content) {
        return stringToHash("SHA-384", content);
    }

    public String stringToSHA256(String content) {
        return stringToHash("SHA-256", content);
    }

    public String stringToSHA1(String content) {
        return stringToHash("SHA-1", content);
    }

    public String stringToMD5(String content) {
        return stringToHash("MD5", content);
    }

    /**
     * HmacSha256
     * 提供相較一般 SHA 更好的防護能力，因為其具有自身的亂數因子，避免彩虹表猜值攻擊
     */
    public String stringToHmacSha256(String content, String pKey) {
        String res = null;
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(pKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(content.getBytes(StandardCharsets.UTF_8));
            res = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/MessageDigest.html
     * https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#messagedigest-algorithms
     */
    private String stringToHash(String hashType, String content) {
        String result = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(hashType);
            messageDigest.reset();
            messageDigest.update(content.getBytes(StandardCharsets.UTF_8));
            result = byteToHex(messageDigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 雜湊碼 byte[] 轉換為 string 的通用方法
     */
    private String byteToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        for ( byte b : hash ) { formatter.format("%02x", b); }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

}
