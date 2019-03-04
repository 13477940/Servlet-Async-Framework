package framework.setting;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import framework.file.FileFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * AppSetting 主要管理作業系統判斷、WebApp 名稱與統一的檔案路徑，
 * 在運行 Tomcat 的 WebApp 中會直接藉由 WEB-INF 資料夾作為定位點，
 * 非 Tomcat 環境中則需要由使用者自行創建一個 baseFileDir 作為定位點。
 * ！當 AppSetting 於多層次的 jar 檔封裝後可能會造成路徑無法取得的問題
 */
public class AppSetting {

    private String configDirName = null;
    private String configFileName = null;
    private String baseFileDirName = null;
    private String appName = null;
    private AppSetting.PathContext pathContext = null;

    private String hostOS = null;
    private String dirSlash = null;

    private AppSetting(String configDirName, String configFileName, String baseFileDirName, String appName, AppSetting.PathContext pathContext) {
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
        return new FileFinder.Builder().build().find(baseFileDirName);
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

    public JSONObject getConfig(String configDirName, String configFileName) {
        return getConfig(new FileFinder.Builder().build().find(configDirName, configFileName));
    }

    public JSONObject getConfig() {
        FileFinder finder = new FileFinder.Builder().build();
        File file = finder.find(this.configDirName, this.configFileName);
        return getConfig(file);
    }

    // getConfig 實作
    public JSONObject getConfig(File file) {
        JSONObject res = null;
        if(null != file) {
            if(file.exists()) {
                String content = readFileText(file);
                if (null != content && content.length() > 0) {
                    {
                        try {
                            res = JSON.parseObject(content);
                        } catch (Exception e) {
                            e.printStackTrace();
                            res = null;
                        }
                    }
                    if (null == res) {
                        try {
                            res = new JSONObject();
                            res.put("config", JSON.parseArray(content));
                        } catch (Exception e) {
                            e.printStackTrace();
                            res = null;
                        }
                    }
                    // 既不是 JSONObject 也不是 JSONArray
                    if (null == res) {
                        try {
                            throw new Exception(file.getName() + " 不是有效的 JSON 內容");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                try {
                    throw new Exception("getConfig() 指定的檔案不存在");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                throw new Exception("請輸入指定的 File 型態物件調用方法");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
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
            FileFinder finder = new FileFinder.Builder().build();
            // 檢查是否為 Tomcat 環境中
            File targetFile = finder.find("WEB-INF");
            {
                if(null == this.appName) {
                    if(null == targetFile || !targetFile.exists()) {
                        try {
                            throw new Exception("在不具有 WEB-INF 的環境下需自定義 APP 名稱！");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(null == targetFile.getParentFile()) {
                            try {
                                throw new Exception("找尋不到指定的資料夾名稱父節點："+this.appName);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            this.appName = targetFile.getParentFile().getName();
                        }
                    }
                }
            }
            // 檢查是否具有應用程式基本儲存的資料夾區域
            String baseFileDirPath;
            {
                File baseFileDir = finder.find(this.baseFileDirName);
                if(null != baseFileDir && baseFileDir.exists()) {
                    baseFileDirPath = baseFileDir.getPath();
                    this.pathContext = new AppSetting.PathContext(baseFileDirPath, this.appName, System.getProperty("file.separator"));
                } else {
                    try {
                        throw new Exception("由 AppSetting 類別路徑往上尋找並未找到名為 " + this.baseFileDirName + " 的資料夾！");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return new AppSetting(this.configDirName, this.configFileName, this.baseFileDirName, this.appName, this.pathContext);
        }
    }

    /**
     * WebApp 預設資源路徑封裝
     */
    public static class PathContext {
        private String tempDirPath = null;
        private String uploadDirPath = null;
        private String exportDirPath = null;
        private String logDirPath = null;

        private PathContext(String baseFileDirPath, String webAppName, String dirSlash) {
            String tmpPath = baseFileDirPath + dirSlash + webAppName;
            tempDirPath = new File(tmpPath + dirSlash + "temp").getPath() + dirSlash;
            uploadDirPath = new File(tmpPath + dirSlash + "uploads").getPath() + dirSlash;
            exportDirPath = new File(tmpPath + dirSlash + "exports").getPath() + dirSlash;
            logDirPath = new File(tmpPath + dirSlash + "logs").getPath() + dirSlash;
        }

        public String getTempDirPath() {
            processPreMKDIR(tempDirPath);
            return tempDirPath;
        }

        public String getUploadDirPath() {
            processPreMKDIR(uploadDirPath);
            return uploadDirPath;
        }

        public String getExportDirPath() {
            processPreMKDIR(exportDirPath);
            return exportDirPath;
        }

        public String getLogDirPath() {
            processPreMKDIR(logDirPath);
            return logDirPath;
        }

        private void processPreMKDIR(String path) {
            File tmp = new File(path);
            if(!tmp.exists() || !tmp.isDirectory()) {
                boolean isDirCreated = tmp.mkdirs();
                if(isDirCreated) {
                    System.out.println("自動建立預設資料夾："+tmp.getPath());
                } else {
                    try {
                        throw new Exception("無法正常建立上傳暫存資料夾");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
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
        if(null == fr) {
            try {
                throw new Exception("FileInputStream 內容為空值");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fr, StandardCharsets.UTF_8))) {
            while(br.ready()) {
                String line = br.readLine();
                sbd.append(line);
            }
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sbd.toString();
    }

}
