package framework.random;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基礎的亂數字串取得方法
 * ThreadLocalRandom 相當於 Random 且提供更好的效能，但並不代表隨機性有所提高
 * SecureRandom 為相較安全的亂數產生類別，除了系統時間種子外會加上其他因素來達成不可預測的隨機數因子
 * "在密碼學中，安全的隨機數非常重要。如果使用不安全的僞隨機數，
 * 所有加密體系都將被攻破。因此，時刻牢記必須使用 SecureRandom 來產生安全的隨機數。"
 * -
 * <a href="https://www.cnblogs.com/deng-cc/p/8064481.html">...</a>
 * <a href="https://www.twblogs.net/a/5e51442bbd9eee21167f63d1">...</a>
 */
public abstract class RandomService {

    private final SecureRandom random;

    RandomService() {
        SecureRandom secureRandom;
        try {
            // 預設取得高安全性的隨機生成器
            secureRandom = SecureRandom.getInstanceStrong();
        } catch ( Exception e ) {
            // 若不支援則改採標準隨機生成器
            secureRandom = new SecureRandom();
        }
        random = new WeakReference<>( secureRandom ).get();
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
        // default random string length
        int defaultSize = 8;
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
     * <a href="https://juejin.im/post/5b8742eb6fb9a019ba68480f">...</a>
     */
    public String getRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        String str_full = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int strLen = str_full.length();
        for(int i = 0; i < ranStrLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_full.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 產生一組小寫英文的英數亂數字串
     */
    public String getLowerCaseRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        String str_lower_num = "abcdefghijklmnopqrstuvwxyz0123456789";
        int strLen = str_lower_num.length();
        for(int i = 0; i < ranStrLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_lower_num.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 產生一組大寫英文的英數亂數字串
     */
    public String getUpperCaseRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        String str_upper_num = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int strLen = str_upper_num.length();
        for(int i = 0; i < ranStrLength; i++) {
            int iRan = random.nextInt(strLen);
            char t = str_upper_num.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 產生一組整數值亂數字串
     */
    public String getRandomNumber(int ranNumLength) {
        StringBuilder sbd = new StringBuilder();
        String str_num = "0123456789";
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
        int max = numR + 1; // +1 表示會出現最大值本身
        return random.nextInt( max - min ) + min;
    }

}
