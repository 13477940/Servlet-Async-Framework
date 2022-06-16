package framework.logs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Log Tool
 * 輔助不方便記錄 log 的環境時使用，例如 service 化的 tomcat 等
 *
 * 自定義路徑初始化、若無設定檔名則以 log_file_日期.txt 規則建立
 * 內容格式以 { json_object } 為一列，讀取時自行加工 [] 前後結尾及每行逗號即可當作 json_array
 */
public class LoggerService {

    static {}

    /**
     * 測試 log 檔案建立路徑
     */
    public static String test() {
        String str = System.getProperty("user.dir");
        System.out.println(str);
        return str;
    }

    public static void logDEBUG(String msg) {
        appendLogMsg("debug", msg);
    }

    public static void logINFO(String msg) {
        appendLogMsg("info", msg);
    }

    public static void logWARN(String msg) {
        appendLogMsg("warning", msg);
    }

    public static void logERROR(String msg) {
        appendLogMsg("error", msg);
    }

    public static void logFATAL(String msg) {
        appendLogMsg("fatal", msg);
    }

    // 寫入記錄訊息至檔案尾段
    private static void appendLogMsg(String level, String msg) {
        // 判斷是否需要建立新檔案
        String now_date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // check log file path dir exist
        {
            if( !( new File( var_holder.file_path ).exists() ) ) {
                new File(var_holder.file_path).mkdirs();
            }
        }
        // 寫入 log 資料至檔案中
        String file_name = "log_" + now_date;
        if(null != var_holder.app_name && var_holder.app_name.length() > 0) {
            file_name = var_holder.app_name + "_log_" + now_date;
        }
        File logFile = new File(var_holder.file_path + var_holder.dir_slash + file_name);
        {
            try {
                if(!logFile.exists()) logFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileWriter fw = null;
        if(logFile.exists() && logFile.isFile()) {
            try {
                JsonObject obj = createLogObj(level, msg);
                if( var_holder.console_output ) System.out.println(new Gson().toJson(obj));
                // true = 尾端 append；false = 不進行 append
                fw = new FileWriter(logFile, true);
                String sys_new_line = System.lineSeparator();
                fw.write(new Gson().toJson(obj) + sys_new_line);
                fw.flush();
            } catch(Exception e) {
                e.printStackTrace();
                close(fw);
            }
        }
        close(fw);
    }

    // 關閉寫入資源
    private static void close(FileWriter fw) {
        try {
            if(fw != null) {
                fw.flush();
                fw.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化 log file 的資料夾路徑，未輸入則採用預設值
     */
    public static void init_logger(String file_path) {
        var_holder.file_path = file_path;
    }

    /**
     * 設定辨識標籤
     */
    public static void set_app_name(String app_name) {
        var_holder.app_name = app_name;
    }

    /**
     * 是否開啟預設的 console output
     */
    public static void set_console_output_status(boolean output_status) {
        var_holder.console_output = output_status;
    }

    // 建立記錄訊息物件
    private static JsonObject createLogObj(String level, String msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("app_name", var_holder.app_name);
        obj.addProperty("user_ip", var_holder.user_ip);
        obj.addProperty("user_name", var_holder.user_name);
        obj.addProperty("type", var_holder.type);
        obj.addProperty("level", level);
        LocalDateTime now = LocalDateTime.now();
        obj.addProperty("date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        obj.addProperty("time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        obj.addProperty("message", msg);
        return obj;
    }

    /**
     * 私有變數儲存
     */
    private static class var_holder {

        private static boolean console_output = true; // default
        private static String file_path = "";

        private static String dir_slash = "";

        private static String app_name = "";
        private static String user_ip = "";
        private static String user_name = "";
        private static String type = "";

        static {
            if(null != get_top_node()) {
                String hostOS = System.getProperty("os.name");
                String dirSlash;
                if (hostOS.toLowerCase().contains("windows")) {
                    dirSlash = "\\\\";
                } else {
                    dirSlash = System.getProperty("file.separator");
                }
                dir_slash = dirSlash;
                // String tmp_path = get_top_node().getPath();
                String tmp_path = System.getProperty("user.dir");
                file_path = tmp_path + dirSlash + "java_run_log";
            }
        }

        /**
         * 取得該磁區頂端節點
         */
        private static File get_top_node() {
            ArrayList<File> files = new ArrayList<>();
            try {
                URL url = Thread.currentThread().getContextClassLoader().getResource("");
                if(null == url) {
                    throw new Exception("初始化檔案路徑發生錯誤");
                }
                String path = String.valueOf( url.getPath() );
                File targetFile = new File( path );
                while(true) {
                    if(Objects.requireNonNull(targetFile.listFiles()).length > 0) {
                        targetFile = targetFile.getParentFile();
                        files.add(targetFile);
                        // System.out.println(Arrays.toString(targetFile.listFiles()));
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
            if(0 == files.size()) return null; // if exception
            return files.get(files.size() - 2);
        }

    }

}
