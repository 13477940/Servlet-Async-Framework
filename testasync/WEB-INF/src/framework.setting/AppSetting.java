package framework.setting;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import framework.file.FileFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class AppSetting {

    private String configDirName = null;
    private String configFileName = null;
    private String baseFileDirName = null;
    private String appName = null;
    private AppSetting.PathContext pathContext = null;

    private String hostOS = null;
    private String dirSlash = null;

    public AppSetting(String configDirName, String configFileName, String baseFileDirName, String appName, AppSetting.PathContext pathContext) {
        {
            this.configDirName = configDirName;
            this.configFileName = configFileName;
            this.baseFileDirName = baseFileDirName;
            this.appName = appName;
            this.pathContext = pathContext;
        }
        this.hostOS = System.getProperty("os.name");
        this.dirSlash = System.getProperty("file.separator");
    }

    public File getBaseFileDir() {
        return new FileFinder().find(baseFileDirName);
    }

    public String getDirSlash() {
        return this.dirSlash;
    }

    public String getHostOS() {
        return this.hostOS;
    }

    public String getAppName() {
        return this.appName;
    }

    public AppSetting.PathContext getPathContext() {
        return this.pathContext;
    }

    public JSONObject getConfig() {
        FileFinder finder = new FileFinder();
        File file = finder.find(this.configDirName, this.configFileName);
        if(null == file) {
            try {
                throw new Exception("無法取得預設的設定檔內容：" + this.configDirName + dirSlash + this.configFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String content = readFileText(file);
        JSONObject res = null;
        if(content.length() > 0) {
            try {
                res = JSON.parseObject(content);
            } catch (Exception e1) {
                // e1.printStackTrace();
                try {
                    res = new JSONObject();
                    res.put("config", JSON.parseArray(content));
                } catch (Exception e2) {
                    // e2.printStackTrace();
                    res = null;
                }
            }
        }
        return res;
    }

    public JSONObject getConfig(File file) {
        JSONObject res = null;
        if(null != file && file.exists()) {
            String content = readFileText(file);
            if(content.length() > 0) {
                try {
                    res = JSON.parseObject(content);
                } catch (Exception e1) {
                    // e1.printStackTrace();
                    try {
                        res = new JSONObject();
                        res.put("config", JSON.parseArray(content));
                    } catch (Exception e2) {
                        // e2.printStackTrace();
                        res = null;
                    }
                }
            }
        }
        return res;
    }

    public JSONObject getConfig(String configDirName, String configFileName) {
        return getConfig(new FileFinder().find(configDirName, configFileName));
    }

    public static class Builder {
        private String configDirName = "conf";
        private String configFileName = "config.json";
        private String baseFileDirName = "TomcatAppFiles";
        private String appName = null;
        private AppSetting.PathContext pathContext = null;

        public Builder() {}

        public AppSetting.Builder setConfigDirName(String configDirName) {
            this.configDirName = configDirName;
            return this;
        }

        public AppSetting.Builder setConfigFileName(String configFileName) {
            this.configFileName = configFileName;
            return this;
        }

        public AppSetting.Builder setBaseFileDirName(String appFileDirName) {
            this.baseFileDirName = appFileDirName;
            return this;
        }

        public AppSetting.Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public AppSetting build() {
            FileFinder finder = new FileFinder();
            // 檢查是否為 Tomcat 環境中
            File targetFile = finder.find("WEB-INF");
            {
                if(null == this.appName) {
                    if (null == targetFile || !targetFile.exists()) {
                        try {
                            throw new Exception("在不具有 WEB-INF 的環境下需自定義 APP 名稱！");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.appName = targetFile.getParentFile().getName();
                }
            }
            // 檢查是否具有應用程式基本儲存的資料夾區域
            String baseFileDirPath = null;
            {
                File baseFileDir = finder.find(this.baseFileDirName);
                if(null != baseFileDir) {
                    baseFileDirPath = baseFileDir.getPath();
                } else {
                    try {
                        throw new Exception("由 AppSetting 類別路徑往上尋找並未找到 " + this.baseFileDirName + " 的資料夾！");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            this.pathContext = new AppSetting.PathContext(baseFileDirPath, this.appName, System.getProperty("file.separator"));
            return new AppSetting(this.configDirName, this.configFileName, this.baseFileDirName, this.appName, this.pathContext);
        }
    }

    public static class PathContext {
        private String tempDirPath = null;
        private String uploadDirPath = null;
        private String exportDirPath = null;
        private String logDirPath = null;

        public PathContext(String baseFileDirPath, String webAppName, String dirSlash) {
            String tmpPath = baseFileDirPath + dirSlash + webAppName;
            tempDirPath = new File(tmpPath + dirSlash + "temp").getPath() + dirSlash;
            uploadDirPath = new File(tmpPath + dirSlash + "uploads").getPath() + dirSlash;
            exportDirPath = new File(tmpPath + dirSlash + "exports").getPath() + dirSlash;
            logDirPath = new File(tmpPath + dirSlash + "logs").getPath() + dirSlash;
        }

        public String getTempDirPath() {
            return tempDirPath;
        }

        public String getUploadDirPath() {
            return uploadDirPath;
        }

        public String getExportDirPath() {
            return exportDirPath;
        }

        public String getLogDirPath() {
            return logDirPath;
        }
    }

    // 讀取純文字類型檔案內容
    private String readFileText(File file) {
        StringBuilder sbd = new StringBuilder();
        FileInputStream fr = null;
        try {
            fr = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fr, "UTF-8"))) {
            while(br.ready()) {
                String line = br.readLine();
                sbd.append(line);
            }
            fr.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sbd.toString();
    }

}
