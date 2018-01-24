package framework.time;

import framework.time.pattern.TimeContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public abstract class TimeService {

    private SimpleDateFormat ISO8601_date = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat ISO8601_datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String[] month = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };

    /**
     * 處理格式依照 ISO-8601 標準為主，公曆：年-月-日(空格)時:分:秒；例如：2015-01-01 05:03:01
     */
    public TimeService() {}

    public TimeContext getTimeContext() {
        return new TimeContext();
    }

    public TimeContext getTimeContext(String dFlag) {
        return new TimeContext(dFlag);
    }

    public TimeContext getTimeContext(String dFlag, String tFlag) {
        return new TimeContext(dFlag, tFlag);
    }

    public TimeContext getTimeContext(Calendar calendar) {
        return new TimeContext(calendar);
    }

    public TimeContext getTimeContext(Calendar calendar, String dFlag) {
        return new TimeContext(calendar, dFlag);
    }

    public TimeContext getTimeContext(Calendar calendar, String dFlag, String tFlag) {
        return new TimeContext(calendar, dFlag, tFlag);
    }

    /**
     * 藉由日期字串取得 Calendar 實例，藉由該 API 可以進行更多的時間操作
     */
    public Calendar getCalendar(String date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(stringToDate(date));
        return calendar;
    }

    /**
     * 基於輸入的日期(年-月-日 ISO-8601)往前(後)幾天的日期，若 Range 為 0 則不作動
     */
    public String getRangeDate(String date, int range) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(stringToDate(date));
        ca.add(Calendar.DAY_OF_MONTH, range);

        StringBuilder sbd;
        sbd = new StringBuilder();
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
        Calendar ca = Calendar.getInstance();
        ca.setTime(stringToDate(date));
        return ca.getTimeInMillis();
    }

    /**
     * 處理 "2015-01-01 01:01:01" 字串格式(ISO-8601)，瞬時片段的 ms 可用於比大小
     */
    public long DateTimeToMilliseconds(String datetime) {
        if(null == datetime || datetime.length() == 0 || datetime.equals("null")) return 0;
        ISO8601_datetime.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = ISO8601_datetime.parse(datetime);
        } catch(Exception e) {
            System.err.println(datetime + " 不是標準的 ISO-8601 時間字串");
            e.printStackTrace();
        }
        Calendar ca = Calendar.getInstance();
        if(date != null) {
            ca.setTime(date);
            return ca.getTimeInMillis();
        } else {
            return 0;
        }
    }

    // 時間回傳可能是 0~9，這個時候要補齊前面的 0，才會符合格式要求
    private String checkZero(int num) {
        StringBuilder sbd = new StringBuilder();
        if(num < 10) { sbd.append("0"); }
        sbd.append(String.valueOf(num));
        return sbd.toString();
    }

    // 藉由 Date 解析 ISO-8601 日期字串
    private Date stringToDate(String ISO_8601) {
        Date date;
        try {
            date = ISO8601_date.parse(ISO_8601);
        } catch (Exception e) {
            e.printStackTrace();
            date = null;
        }
        if(null == date) {
            try {
                date = ISO8601_datetime.parse(ISO_8601);
            } catch (Exception e) {
                e.printStackTrace();
                date = null;
            }
        }
        if(null == date) {
            System.err.println("輸入的 ISO 8601 時間格式有錯誤");
        }
        return date;
    }

}
