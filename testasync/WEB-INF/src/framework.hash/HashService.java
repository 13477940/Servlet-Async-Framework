package framework.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private String byteToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        for ( byte b : hash ) { formatter.format("%02x", b); }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

}
