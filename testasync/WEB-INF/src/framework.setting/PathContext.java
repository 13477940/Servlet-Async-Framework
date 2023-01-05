package framework.setting;

import framework.file.FileFinder;

import java.io.File;

/**
 * 此為新版本規則 #20220902 -> 以 "WebAppFiles" 作為預設資料夾名稱
 * webapp_file -> 指提供 webapp 檔案存放的本機位址（上傳、下載、暫存等形式）
 * webapp_name -> 當下 webapp 專案名稱，此為重要參數（非 Tomcat 環境建議手動設定）
 * webapp_project -> 指當下正在使用的 webapp 專案專屬的檔案資料夾
 */
public class PathContext {

    private final String host_os = System.getProperty("os.name");
    private String file_separator = System.getProperty("file.separator");

    private String webapp_name = null; // this webapp name
    private String webapp_project_folder_path = null;
    private String webapp_file_folder_path = null;
    private String base_webapp_file_folder_name = "WebAppFiles";
    private String base_webapp_file_folder_path = System.getProperty("user.dir");

    public PathContext()  {
        // fix for like WINDOWS OS etc.
        if("\\".equalsIgnoreCase(file_separator)) {
            this.file_separator = "\\\\";
        }
        update_base_path();
    }

    public void set_webapp_name(String webapp_name) {
        this.webapp_name = webapp_name;
    }

    public void set_webapp_file_folder_name(String folder_name) {
        this.base_webapp_file_folder_name = folder_name;
        update_base_path();
    }

    public void set_webapp_file_folder_base_path(String folder_path) {
        this.base_webapp_file_folder_path = folder_path;
        update_base_path();
    }

    public String get_file_separator() {
        return this.file_separator;
    }

    public String get_webapp_project_folder_path() {
        if(null == this.webapp_project_folder_path) {
            try {
                throw new Exception("目前執行環境不具備 Web Container 設定，WebApp 類設定為空值");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.webapp_project_folder_path;
    }

    public String get_webapp_log_file_folder_path() {
        return proc_folder_path_str("log");
    }

    public String get_webapp_temp_file_folder_path() {
        return proc_folder_path_str("temp");
    }

    public String get_webapp_upload_file_folder_path() {
        return proc_folder_path_str("upload");
    }

    public String get_webapp_export_file_folder_path() {
        return proc_folder_path_str("export");
    }

    private String proc_folder_path_str(String path_define_folder_name) {
        StringBuilder sbd = new StringBuilder();
        {
            sbd.append(this.webapp_file_folder_path).append(this.file_separator);
            sbd.append(this.webapp_name).append(this.file_separator);
            sbd.append(path_define_folder_name).append(this.file_separator);
        }
        {
            File file = new File(sbd.toString());
            if(!file.exists()) {
                try {
                    throw new Exception("不存在的專案資料夾路徑："+ sbd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
        return sbd.toString();
    }

    private void update_base_path() {
        // try auto find webapp name
        {
            File targetFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            FileFinder finder = new FileFinder.Builder().setBaseFile(targetFile).build();
            File web_inf_folder = finder.find("WEB-INF");
            if(null != web_inf_folder && web_inf_folder.exists()) {
                File web_app_folder = web_inf_folder.getParentFile();
                this.webapp_name = web_app_folder.getName();
                this.webapp_project_folder_path = web_app_folder.getPath();
            }
        }
        // init set base path folder
        var target_file = new File(this.base_webapp_file_folder_path);
        FileFinder finder = new FileFinder.Builder().setBaseFile(target_file).build();
        File webapp_file_folder = finder.find(this.base_webapp_file_folder_name);
        if(null == webapp_file_folder || !webapp_file_folder.exists() || !webapp_file_folder.isDirectory()) {
            try {
                throw new Exception("請於同一個磁碟區之中建立 "+this.base_webapp_file_folder_name +" 資料夾");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        this.webapp_file_folder_path = webapp_file_folder.getPath();
    }

}
