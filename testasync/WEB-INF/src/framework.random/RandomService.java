package framework.random;

public class RandomService {

    private final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int defaultSize = 10;

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
     * 取得兩整數之間的亂數值
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
        double d = Math.random() * (max-min) + min;
        return Double.valueOf(d).intValue();
    }

}
