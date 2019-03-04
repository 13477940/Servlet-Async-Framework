package framework.random;

import java.util.concurrent.ThreadLocalRandom;

public abstract class RandomService {

    private final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int defaultSize = 8; // default random string length

    RandomService() {}

    /**
     * 藉由時間戳記與亂數字串生成一筆雜湊字串，亦可以藉由增加亂數字串長度減少重複機率
     */
    public String getTimeHash(int ranStrSize) {
        StringBuilder sbd = new StringBuilder();
        String tmp = String.valueOf(System.currentTimeMillis());
        sbd.append(tmp);
        sbd.append(getRandomString(ranStrSize));
        return sbd.toString();
    }
    public String getTimeHash() {
        return getTimeHash(defaultSize);
    }

    /**
     * 標準的英數亂數字串產生，使用 ThreadLocalRandom 加快亂數產生效率
     * https://juejin.im/post/5b8742eb6fb9a019ba68480f
     */
    public String getRandomString(int ranStrSize) {
        StringBuilder sbd = new StringBuilder();
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for(int i = 0; i < ranStrSize; i++) {
            int strLen = ranStr.length()-1;
            int iRan = localRandom.nextInt(strLen);
            char t = ranStr.charAt(iRan);
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
        // double d = Math.random() * (max-min) + min;
        // return Double.valueOf(d).intValue();
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        return localRandom.nextInt(max+1-min)+min;
    }

}
