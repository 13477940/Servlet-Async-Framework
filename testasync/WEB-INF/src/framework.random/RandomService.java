package framework.random;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基礎的亂數字串取得方法
 * ThreadLocalRandom 相當於 Random 且提供更好的效能，但並不代表隨機性有所提高
 * SecureRandom 為較安全的亂數產生類別，除了系統時間種子外會加上其他因素來達成不可預測的隨機數
 * https://www.cnblogs.com/deng-cc/p/8064481.html
 */
public abstract class RandomService {

    private final String str_num = "0123456789";
    private final String str_lower_num = "abcdefghijklmnopqrstuvwxyz0123456789";
    private final String str_full = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final int defaultSize = 8; // default random string length

    private SecureRandom random;

    RandomService() {
        random = new WeakReference<>( new SecureRandom() ).get();
    }

    /**
     * now millisecond + [random string]
     * 藉由時間戳記與亂數字串生成一筆雜湊字串，亦可以藉由增加亂數字串長度減少重複機率
     */
    public String getTimeHash(int ranStrLength) {
        String tmp = String.valueOf(System.currentTimeMillis());
        StringBuilder sbd = new StringBuilder();
        {
            sbd.append(tmp);
            sbd.append(getRandomString(ranStrLength));
        }
        return sbd.toString();
    }
    public String getTimeHash() {
        return getTimeHash(defaultSize);
    }

    /**
     * now datetime string + [random string]
     * 輸出 20191231121109 + [random string] 的日期時間字串含亂數模式
     */
    public String getDateTimeStringHash(int ranStrLength) {
        LocalDateTime ldt = LocalDateTime.now();
        String dtStr = ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dtStr + getRandomString(ranStrLength);
    }

    /**
     * 標準的英數亂數字串產生，使用 ThreadLocalRandom 加快亂數產生效率
     * 要注意這只適用於大小寫敏感的用途，若大小寫不敏感則建議改用 getUpCaseRandomString()
     * https://juejin.im/post/5b8742eb6fb9a019ba68480f
     */
    public String getRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        int strLen = str_full.length();
        for(int i = 0; i < ranStrLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_full.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 產生一組大小寫不敏感的英數亂數字串
     */
    public String getLowerCaseRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        int strLen = str_lower_num.length();
        for(int i = 0; i < ranStrLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_lower_num.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 產生一組整數值亂數字串
     */
    public String getRandomNumber(int ranNumLength) {
        StringBuilder sbd = new StringBuilder();
        int strLen = str_num.length();
        for(int i = 0; i < ranNumLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_num.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 取得兩整數之間的整數亂數值
     */
    public int getRangeRandom(int numL, int numR) {
        // 維持左小右大原則，保護亂數產生器運算正常
        int tmp;
        if (numL > numR) {
            tmp = numR;
            numR = numL;
            numL = tmp;
        }
        // 製作亂數
        int min = numL;
        int max = numR + 1; // +1 表示有機率出現最大值本身
        return random.nextInt( max + 1 - min ) + min;
    }

}
