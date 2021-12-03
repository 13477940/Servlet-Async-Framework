package framework.authenticate;

import org.bouncycastle.util.encoders.Base32;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Formatter;

/**
 * 實作 Google Authenticator
 *
 * require jar
 * https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15on
 *
 * Google Authenticator QRCode generator
 * https://greddyblogs.gitlab.io/2019/07/04/googleAuthenticator
 * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
 */
public class GoogleAuthenticatorUtil {

    /**
     * 產生隨機密鑰（需儲存再重複使用）
     */
    public String genRandomSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String secretKey = Base32.toBase32String(bytes);
        return secretKey.toLowerCase();
    }

    /**
     * 產生明碼定義密鑰（可由字串重複使用）
     */
    public String genStrSecretKey(String raw_str) {
        String hash_str = HashService.stringToSHA256(raw_str);
        byte[] bytes = hash_str.getBytes(StandardCharsets.UTF_8);
        String secretKey = Base32.toBase32String(bytes);
        return secretKey.toLowerCase();
    }

    /**
     * 產生一組 TOTP Code
     */
    public String getTOTP(String secretKey, long time) {
        byte[] bytes = Base32.decode(secretKey.toUpperCase());
        String hexKey = Hex.toHexString(bytes);
        String hexTime = Long.toHexString(time);
        return TOTP.generateTOTP(hexKey, hexTime, "6");
    }

    /**
     * 產生 Authenticator 適用的 URI Path，一般使用於產生 QRCode 內容中
     */
    public String buildAuthenticatorPath(String secret, String account, String issuer) {
        String qrCodeData = "otpauth://totp/%s?secret=%s&issuer=%s";
        try {
            return String.format(
                    qrCodeData,
                    URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8).replace("+", "%20"),
                    URLEncoder.encode(secret, StandardCharsets.UTF_8).replace("+", "%20"),
                    URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 驗證 TOTP Code，需帶入原金鑰字串
     */
    public boolean verify(String secretKey, String code) {
        int iTimeExcursion = 1; // 容錯範圍（往前、往後幾個時間週期，越少週期標準越嚴格）
        long time = System.currentTimeMillis() / 1000 / 30;
        for ( int i = -iTimeExcursion; i <= iTimeExcursion; i++ ) {
            String totp = getTOTP(secretKey, time + i);
            if (code.equals(totp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This an example implementation of the OATH TOTP algorithm.
     * Visit www.openauthentication.org for more information.
     *
     * @author Johan Rydell, PortWise, Inc.
     */
    private static class TOTP {

        private TOTP() {}

        private static byte[] hmac_sha1(String crypto, byte[] keyBytes, byte[] text) {
            Mac hmac = null;
            try {
                hmac = Mac.getInstance(crypto);
                SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
                hmac.init(macKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assert hmac != null;
            return hmac.doFinal(text);
        }

        private static byte[] hexStr2Bytes(String hex){
            // Adding one byte to get the right conversion
            // values starting with "0" can be converted
            byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

            // Copy all the REAL bytes, not the "first"
            byte[] ret = new byte[bArray.length - 1];
            System.arraycopy(bArray, 1, ret, 0, ret.length);
            return ret;
        }

        private static final int[] DIGITS_POWER
                // 0 1  2   3    4     5      6       7        8
                = {1,10,100,1000,10000,100000,1000000,10000000,100000000 };

        public static String generateTOTP(String key, String time, String returnDigits) {
            return generateTOTP(key, time, returnDigits, "HmacSHA1");
        }

        public static String generateTOTP256(String key, String time, String returnDigits) {
            return generateTOTP(key, time, returnDigits, "HmacSHA256");
        }

        public static String generateTOTP512(String key, String time, String returnDigits) {
            return generateTOTP(key, time, returnDigits, "HmacSHA512");
        }

        private static String generateTOTP(String key, String time, String returnDigits, String crypto) {
            int codeDigits = Integer.decode(returnDigits);
            StringBuilder result;
            byte[] hash;

            // Using the counter
            // First 8 bytes are for the movingFactor
            // Complaint with base RFC 4226 (HOTP)
            StringBuilder timeBuilder = new StringBuilder(time);
            while(timeBuilder.length() < 16 ) timeBuilder.insert(0, "0");
            time = timeBuilder.toString();

            // Get the HEX in a Byte[]
            byte[] msg = hexStr2Bytes(time);

            // Adding one byte to get the right conversion
            byte[] k = hexStr2Bytes(key);

            hash = hmac_sha1(crypto, k, msg);

            // put selected bytes into result int
            int offset = hash[hash.length - 1] & 0xf;

            int binary =
                    ((hash[offset] & 0x7f) << 24) |
                            ((hash[offset + 1] & 0xff) << 16) |
                            ((hash[offset + 2] & 0xff) << 8) |
                            (hash[offset + 3] & 0xff);

            int otp = binary % DIGITS_POWER[codeDigits];

            result = new StringBuilder(Integer.toString(otp));
            while (result.length() < codeDigits) {
                result.insert(0, "0");
            }
            return result.toString();
        }

    }

    /**
     * Hash String Generator
     */
    private static class HashService {

        /**
         * Hash Tool
         */
        private static String stringToSHA256(String content) {
            return stringToHash("SHA-256", content);
        }

        /**
         * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/MessageDigest.html
         * https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#messagedigest-algorithms
         */
        private static String stringToHash(String hashType, String content) {
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

        private static String byteToHex(byte[] hash) {
            Formatter formatter = new Formatter();
            for ( byte b : hash ) { formatter.format("%02x", b); }
            String result = formatter.toString();
            formatter.close();
            return result;
        }

    }

}
