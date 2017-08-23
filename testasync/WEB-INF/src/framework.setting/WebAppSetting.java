package framework.setting;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class WebAppSetting {

    private String classesPath = null;
    private File tomcatParentDir = null;
    private File webAppDir = null;
    private final String baseDirName = "TomcatAppFiles";

    private String hostOS = null;
    private String dirSlash = null;
    private WebAppPathContext wapc = null;

    private String propFileName = "webapp.properties"; // 參數檔案名稱
    private Properties properties = null; // 參數實例

    public WebAppSetting() {
        classesPath = this.getClass().getClassLoader().getResource("").getPath(); // classes dir
        tomcatParentDir = getTomcatParentDir();
        webAppDir = getWebAppDir();
        identifyHostOS();
        createWebAppDir();
        loadPropertiesFile();
    }

    /**
     * 取得 webapp.properites 實例
     */
    public Properties getWebAppProperties() {
        return properties;
    }

    /**
     * 取得應用程式路徑資訊
     */
    public WebAppPathContext getPathContext() {
        return wapc;
    }

    /**
     * 取得應用程式名稱
     */
    public String getWebAppName() {
        return webAppDir.getName();
    }

    /**
     * 取得作業系統資料夾階層符
     */
    public String getDirSlash() {
        return dirSlash;
    }

    // 讀取 webapp.properties
    private void loadPropertiesFile() {
        properties = new Properties();
        try {
            String propPath = wapc.getWebInfPath()+"conf"+dirSlash+propFileName;
            FileInputStream fis = new FileInputStream(propPath);
            properties.load(fis);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 建立該 WebApp 增量檔案儲存資料夾
    private void createWebAppDir() {
        wapc = new WebAppPathContext(tomcatParentDir.getPath(), baseDirName, webAppDir.getName(), webAppDir.getPath(), dirSlash);
        File fTemp = new File(wapc.getTempDirPath());
        File fUpload = new File(wapc.getUploadDirPath());
        File fExport = new File(wapc.getExportDirPath());
        File fLog = new File(wapc.getLogDirPath());

        // 如果不具有資料夾時則建立
        try {
            if(!fTemp.exists()) fTemp.mkdirs();
            if(!fUpload.exists()) fUpload.mkdirs();
            if(!fExport.exists()) fExport.mkdirs();
            if(!fLog.exists()) fLog.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 辨識 Tomcat 容器運行的作業系統類型 Unix or Windows
    private void identifyHostOS() {
        if(classesPath.contains("/")) {
            hostOS = "unix";
            dirSlash = "/";
        } else if(classesPath.contains("\\")) {
            hostOS = "windows";
            dirSlash = "\\";
        } else {
            hostOS = "unknown";
        }
    }

    // 取得 Tomcat 容器的上一層資料夾路徑
    private File getTomcatParentDir() {
        return new File(classesPath).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
    }

    // 取得該 WebApp 資料夾路徑
    private File getWebAppDir() {
        return new File(classesPath).getParentFile().getParentFile();
    }

}
