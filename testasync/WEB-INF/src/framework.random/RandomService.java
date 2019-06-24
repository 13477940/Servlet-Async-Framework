package framework.random;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基礎的亂數字串取得方法
 */
public abstract class RandomService {

    private final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final String ranNum = "0123456789";
    private final int defaultSize = 8; // default random string length

    RandomService() {}

    /**
     * now millisecond + [random string]
     * 藉由時間戳記與亂數字串生成一筆雜湊字串，亦可以藉由增加亂數字串長度減少重複機率
     */
    public String getTimeHash(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        String tmp = String.valueOf(System.currentTimeMillis());
        sbd.append(tmp);
        sbd.append(getRandomString(ranStrLength));
        return sbd.toString();
    }
    public String getTimeHash() {
        return getTimeHash(defaultSize);
    }

    /**
     * now datetime string + [random string]
     * 輸出 20191231121109 + [random string] 的日期時間字串含亂數模式
     */
    public String getTimeStringHash(int ranStrLength) {
        LocalDateTime ldt = LocalDateTime.now();
        String dtStr = ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dtStr + getRandomString(ranStrLength);
    }

    /**
     * 標準的英數亂數字串產生，使用 ThreadLocalRandom 加快亂數產生效率
     * https://juejin.im/post/5b8742eb6fb9a019ba68480f
     */
    public String getRandomString(int ranStrLength) {
        StringBuilder sbd = new StringBuilder();
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for(int i = 0; i < ranStrLength; i++) {
            int strLen = ranStr.length();
            int iRan = localRandom.nextInt(strLen);
            char t = ranStr.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 取得一組連續亂數的整數字串
     */
    public String getRandomNumber(int ranNumLength) {
        StringBuilder sbd = new StringBuilder();
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for(int i = 0; i < ranNumLength; i++) {
            int strLen = ranNum.length();
            int iRan = localRandom.nextInt(strLen);
            char t = ranNum.charAt(iRan);
            sbd.append(t);
        }
        return sbd.toString();
    }

    /**
     * 取得兩整數之間的亂數值
     */
    public int getRangeRandom(int numL, int numR) {
        // 確保左小右大原則，保護亂數產生器
        int tmp;
        if (numL > numR) {
            tmp = numR;
            numR = numL;
            numL = tmp;
        }
        // 製作亂數
        int min = numL;
        int max = numR + 1; // +1 表示有機率出現最大值本身
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        return localRandom.nextInt(max+1-min)+min;
    }

}
