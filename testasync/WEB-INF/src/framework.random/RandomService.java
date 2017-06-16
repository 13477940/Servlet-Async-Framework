package framework.random;

public class RandomService {

    private final String ranStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

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
        return getTimeHash(10);
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

}
