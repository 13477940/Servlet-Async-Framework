package framework.time.pattern;

import java.util.Calendar;

/**
 * 藉由 TimeContext 表示那一瞬間的時間狀態
 * 如果要更新時間，則直覺上因該是重新建立一次 TimeContext
 * 所以可以把 TimeContext 視為等同於一個瞬間的時間片段（Time Slice）
 * 以此方法解決於多執行緒環境下重複操作一個字串資源可能會造成的錯誤
 */
public class TimeContext {

    private String[] month = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };

    // JDK 時間工具
    private Calendar calendar = null;

    // 時間內容
    private String sYear = null;
    private String sMonth = null;
    private String sDay = null;
    private String sHour = null;
    private String sMinute = null;
    private String sSecond = null;

    // 預設是採用 ISO-8601 標準分隔符
    private String splitDateFlag = "-";
    private String splitTimeFlag = ":";

    /**
     * 全預設的情況下會讀取主機時間
     */
    public TimeContext() {
        update();
    }

    /**
     * 傳入自定義的 Calendar
     */
    public TimeContext(Calendar calendar) {
        this.calendar = calendar;
        update();
    }

    /**
     * splitDateFlag 定義分隔日期的符號字串為何
     */
    public TimeContext(String splitDateFlag) {
        if(null != splitDateFlag) {
            this.splitDateFlag = splitDateFlag;
        }
        update();
    }
    public TimeContext(Calendar calendar, String splitDateFlag) {
        this.calendar = calendar;
        if(null != splitDateFlag) {
            this.splitDateFlag = splitDateFlag;
        }
        update();
    }

    /**
     * splitDateFlag 定義分隔日期的符號字串為何
     * splitTimeFlag 定義分隔時間的符號字串為何
     */
    public TimeContext(String splitDateFlag, String splitTimeFlag) {
        if(null != splitDateFlag) {
            this.splitDateFlag = splitDateFlag;
        }
        if(null != splitTimeFlag) {
            this.splitTimeFlag = splitTimeFlag;
        }
        update();
    }
    public TimeContext(Calendar calendar, String splitDateFlag, String splitTimeFlag) {
        this.calendar = calendar;
        if(null != splitDateFlag) {
            this.splitDateFlag = splitDateFlag;
        }
        if(null != splitTimeFlag) {
            this.splitTimeFlag = splitTimeFlag;
        }
        update();
    }

    public String getDate() {
        return sYear + splitDateFlag + sMonth + splitDateFlag + sDay;
    }

    public String getTime() {
        return sHour + splitTimeFlag + sMinute + splitTimeFlag + sSecond;
    }

    public String getYear() {
        return sYear;
    }
    public String getMonth() {
        return sMonth;
    }
    public String getDay() {
        return sDay;
    }
    public String getHour() {
        return sHour;
    }
    public String getMinute() {
        return sMinute;
    }
    public String getSecond() {
        return sSecond;
    }

    // 更新日期內容，由於設定此時間封裝建立後不能被更動所以採用私有更新方法
    private void update() {
        Calendar _calendar = null;
        if(null == calendar) {
            _calendar = Calendar.getInstance();
        } else {
            _calendar = calendar;
        }
        this.sYear = String.valueOf(_calendar.get(Calendar.YEAR));
        this.sMonth = month[_calendar.get(Calendar.MONTH)];
        this.sDay = checkZero(_calendar.get(Calendar.DAY_OF_MONTH));
        this.sHour = checkZero(_calendar.get(Calendar.HOUR_OF_DAY));
        this.sMinute = checkZero(_calendar.get(Calendar.MINUTE));
        this.sSecond = checkZero(_calendar.get(Calendar.SECOND));
    }

    // 時間回傳可能是 0~9，這個時候要補齊前面的 0，才會符合格式要求
    private String checkZero(int num) {
        StringBuilder sbd = new StringBuilder();
        if(num < 10) { sbd.append("0"); }
        sbd.append(String.valueOf(num));
        return sbd.toString();
    }

}
