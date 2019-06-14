package framework.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 遵循 ISO 8601 格式處理適用於資料庫儲存的日期與時間字串
 * yyyy-MM-dd HH:mm:ss（年-月-日 時:分:秒）
 * 字串結果僅適合儲存或提供解析，要比較時間、調整時間等需要切換回 LocalDateTime 型態
 */
public class TimeService {

    private static final String pattern_date = "yyyy-MM-dd";
    private static final String pattern_time = "HH:mm:ss";

    private LocalDateTime ldt;

    private TimeService(LocalDateTime ldt) {
        this.ldt = ldt;
    }

    public String getDate() {
        return ldt.format(DateTimeFormatter.ofPattern(pattern_date));
    }

    public String getTime() {
        return ldt.format(DateTimeFormatter.ofPattern(pattern_time));
    }

    public String getDateTime() {
        return ldt.format(DateTimeFormatter.ofPattern(pattern_date + " " + pattern_time));
    }

    public String getDatePatternString() {
        return pattern_date;
    }

    public String getTimePatternString() {
        return pattern_time;
    }

    public LocalDateTime prototype() {
        return this.ldt;
    }

    public static class Builder {

        private LocalDateTime ldt = null;

        public TimeService.Builder setLocalDateTime(LocalDateTime ldt) {
            this.ldt = ldt;
            return this;
        }

        public TimeService.Builder setLocalDateTime(String dateTimeString) {
            ldt = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(pattern_date + " " + pattern_time));
            return this;
        }

        public TimeService.Builder setLocalDate(String date) {
            if(null == date || date.length() == 0) {
                try {
                    throw new Exception("TimeService.setLocalDate 必須帶入有效的日期內容");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            ldt = LocalDateTime.parse(date + " 00:00:00", DateTimeFormatter.ofPattern(pattern_date + " " + pattern_time));
            return this;
        }

        public TimeService build() {
            if(null == ldt) {
                try {
                    throw new Exception("請先設定 LocalDateTime 實例來使用 TimeService");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            return new TimeService(this.ldt);
        }

    }

}
