package framework.random;

import java.security.MessageDigest;
import java.util.Formatter;

public class RandomService {

    private final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int defaultSize = 10;

    /**
     * 藉由時間戳記產生亂數，因時間的不可回朔性及亂數的數量級，可以確定該亂數為唯一值
     */
    public String getTimeHash(int ranStrSize) {
        StringBuilder sbd = new StringBuilder();
        String tmp = String.valueOf(System.currentTimeMillis());
        java.util.Random random = new java.util.Random();
        sbd.append(tmp);
        for(int i = 0; i < ranStrSize; i++) {
            int strLen = ranStr.length()-1;
            int iRan = random.nextInt(strLen);
            char t = ranStr.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }
    public String getTimeHash() {
        return getTimeHash(defaultSize);
    }

    public String getTimeHashToSHA1() { return getTimeHashToSHA1(defaultSize); }
    public String getTimeHashToSHA1(int ranStrSize) { return stringToHash("SHA-1", getTimeHash(ranStrSize)); }

    public String getTimeHashToSHA256() { return getTimeHashToSHA256(defaultSize); }
    public String getTimeHashToSHA256(int ranStrSize) { return stringToHash("SHA-256", getTimeHash(ranStrSize)); }

    /**
     * 標準的英數亂數字串產生
     */
    public String getRandomString(int ranStrSize) {
        StringBuilder sbd = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for(int i = 0; i < ranStrSize; i++) {
            int strLen = ranStr.length()-1;
            int iRan = random.nextInt(strLen);
            char t = ranStr.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 取得整數區間的亂數值
     */
    public int getRangeRandom(int numL, int numR) {
        // 確保左小右大原則，保護亂數產生器
        {
            int tmp = 0;
            if (numL > numR) {
                tmp = numR;
                numR = numL;
                numL = tmp;
            }
        }
        // 製作亂數
        int min = numL;
        int max = numR + 1; // +1 表示有機率出現最大值本身
        double d = Math.random()*(max-min)+min;
        return Double.valueOf(d).intValue();
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
