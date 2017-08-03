package framework.hash;

import java.security.MessageDigest;
import java.util.Formatter;

public class HashService {

    public HashService() {}

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
     * 可以自行帶入使用 SHA-1 或 SHA-256 等雜湊格式
     */
    private String stringToHash(String hashType, String content) {
        String result = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance(hashType);
            crypt.reset();
            crypt.update(content.getBytes("UTF-8"));
            result = byteToHex(crypt.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toUpperCase();
    }

    private String byteToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        for(byte b : hash) { formatter.format("%02x", b); }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

}
