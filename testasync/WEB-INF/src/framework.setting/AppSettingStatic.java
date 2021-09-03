package framework.setting;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 主要用於持久化儲存 web-app 資料夾路徑
 * 此類別主要解決 Windows 或相關環境會有路徑取得問題時使用
 */
class AppSettingStatic {

    private static File cache_web_inf_dir = null; // [web_app_dir] -> [WEB-INF]
    private static File cache_web_app_dir = null;

    private static final String sys_slash = System.getProperty("file.separator");

    public static void set_web_inf_dir(File web_inf_dir) {
        if(null != web_inf_dir) {
            cache_web_inf_dir = new WeakReference<>( web_inf_dir ).get();
            assert cache_web_inf_dir != null;
            cache_web_app_dir = cache_web_inf_dir.getParentFile();
        } else {
            try {
                throw new Exception("請輸入正確的 web-app/WEB-INF 資料夾路徑檔案");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void set_web_app_dir(File web_app_dir) {
        if(null != web_app_dir) {
            cache_web_app_dir = web_app_dir;
            cache_web_inf_dir = new File( web_app_dir.getPath() + sys_slash + "WEB-INF" );
        } else {
            try {
                throw new Exception("請輸入正確的 web-app 資料夾路徑檔案");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static File get_web_inf_dir() {
        return cache_web_inf_dir;
    }

    public static File get_web_app_dir() {
        return cache_web_app_dir;
    }

}
