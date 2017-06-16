package framework.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeService {

    private SimpleDateFormat ISO8601_date = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat ISO8601_datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String[] month = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
    private StringBuilder sbdTmp = new StringBuilder();
    private StringBuilder sbd = new StringBuilder();

    private String nowDate = null;
    private String nowTime = null;
    private String nowYear = null;
    private String nowMonth = null;
    private String nowDay = null;

    /**
     * 處理格式依照 ISO-8601 標準為主，公曆：年-月-日(空格)時:分:秒；例如：2015-01-01 05:03:01
     */
    public TimeService() {
        updateTime();
    }

    /**
     * 更新目前時間狀態
     */
    public void updateTime() {
        String tmp = getCurrentTime();
        // split Date & Time
        String[] timeFlag = tmp.split(" ");
        nowDate = timeFlag[0];
        nowTime = timeFlag[1];
        // split Date To Y,M,D
        String[] dateArr = nowDate.split("-");
        nowYear = dateArr[0];
        nowMonth = dateArr[1];
        nowDay = dateArr[2];
    }

    /**
     * 取得 年-月-日
     */
    public String getDate() {
        return nowDate;
    }

    /**
     * 取得 時:分:秒
     */
    public String getTime() {
        return nowTime;
    }

    /**
     * 取得當前年
     */
    public String getYear() {
        return nowYear;
    }

    /**
     * 取得當年月
     */
    public String getMonth() {
        return nowMonth;
    }

    /**
     * 取得當前日
     */
    public String getDay() {
        return nowDay;
    }

    /**
     * 基於輸入的日期(年-月-日 ISO-8601)往前(後)幾天的日期，若 Range 為 0 則不作動
     */
    public String getRangeDate(String date, int range) {
        Date d = new Date();
        try {
            d = ISO8601_date.parse(date);
        } catch (ParseException e) {
            System.err.println("輸入日期字串格式不符合 ISO-8601 標準。");
            e.printStackTrace();
        }

        Calendar ca = Calendar.getInstance();
        ca.setTime(d);
        ca.add(Calendar.DAY_OF_MONTH, range);

        sbd.delete(0, sbd.length());
        sbd.append(ca.get(Calendar.YEAR));
        sbd.append("-");
        sbd.append(month[ca.get(Calendar.MONTH)]);
        sbd.append("-");
        sbd.append(checkZero(ca.get(Calendar.DAY_OF_MONTH)));
        return sbd.toString();
    }

    /**
     * 輸入 ISO-8601 字串日期格式會轉換為 Milliseconds，相比日期大小時較方便
     */
    public long DateToMilliseconds(String date) {
        String[] ref = {};
        if(date.length() == 0 || date.equals("null")) return 0;
        try {
            ref = date.split("-");
        } catch(Exception e) {
            e.printStackTrace();
        }
        Calendar ca = Calendar.getInstance();
        if(ref.length > 0) {
            ca.set(Integer.valueOf(ref[0]), Integer.valueOf(ref[1]), Integer.valueOf(ref[2]));
        }
        return ca.getTimeInMillis();
    }

    /**
     * 處理 "2015-01-01 01:01:01" 字串格式(ISO-8601)，比大小好用
     */
    public long DateTimeToMilliseconds(String datetime) {
        if(datetime.length() == 0 || datetime.equals("null")) return 0;
        ISO8601_datetime.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        java.util.Date date = null;
        try {
            date = ISO8601_datetime.parse(datetime);
        } catch(Exception e) {
            e.printStackTrace();
        }
        String res = String.valueOf(date.getTime());
        return Long.valueOf(res);
    }

    // 建立此次時間封裝內容
    private String getCurrentTime() {
        Calendar ca = Calendar.getInstance();
        sbd.delete(0, sbd.length());
        sbd.append(ca.get(Calendar.YEAR));
        sbd.append("-");
        sbd.append(month[ca.get(Calendar.MONTH)]);
        sbd.append("-");
        sbd.append(checkZero(ca.get(Calendar.DAY_OF_MONTH)));
        sbd.append(" ");
        sbd.append(checkZero(ca.get(Calendar.HOUR_OF_DAY)));
        sbd.append(":");
        sbd.append(checkZero(ca.get(Calendar.MINUTE)));
        sbd.append(":");
        sbd.append(checkZero(ca.get(Calendar.SECOND)));
        return sbd.toString();
    }

    // 時間回傳可能是 0~9，這個時候要補齊前面的 0，才會符合格式要求
    private String checkZero(int num) {
        sbdTmp.delete(0, sbdTmp.length());
        if(num < 10) { sbdTmp.append("0"); }
        sbdTmp.append(String.valueOf(num));
        return sbdTmp.toString();
    }

}
