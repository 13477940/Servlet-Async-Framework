package framework.setting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.file.FileFinder;
import framework.web.servlet.ServletContextStatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * AppSetting 主要管理作業系統判斷、WebApp 名稱與統一的檔案路徑，
 * 在運行 Tomcat 的 WebApp 中會直接藉由 WEB-INF 資料夾作為定位點，
 * 非 Tomcat 環境中則需要由使用者自行創建一個 baseFileDir 作為定位點。
 * 要注意 AppSetting 於多層次的 jar 檔封裝後可能會有路徑無法取得的問題發生。
 */
public class AppSetting {

    private final String configDirName;
    private final String configFileName;
    private final String baseFileDirName;
    private final AppSetting.PathContext pathContext;

    private final String hostOS;
    private final String dirSlash;

    private AppSetting(String configDirName, String configFileName, String baseFileDirName, AppSetting.PathContext pathContext) {
        {
            this.configDirName = configDirName;
            this.configFileName = configFileName;
            this.baseFileDirName = baseFileDirName;
            this.pathContext = pathContext;
        }
        this.hostOS = System.getProperty("os.name");
        if ( hostOS.toLowerCase().contains("windows") ) {
            this.dirSlash = "\\\\";
        } else {
            this.dirSlash = System.getProperty("file.separator");
        }
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

    public AppSetting.PathContext getPathContext() {
        return this.pathContext;
    }

    public JsonObject getConfig(String configDirName, String configFileName) {
        return getConfig(new FileFinder.Builder().build().find(configDirName, configFileName));
    }

    public JsonObject getConfig() {
        FileFinder finder = new FileFinder.Builder().build();
        File file = finder.find(this.configDirName, this.configFileName);
        return getConfig(file);
    }

    public JsonObject getConfig(String filePath) {
        File file = new File(filePath);
        if(file.exists()) {
            return getConfig(file);
        } else {
            try {
                throw new Exception(filePath + " 該檔案不存在");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // getConfig 實作
    public JsonObject getConfig(File file) {
        JsonObject res = null;
        if(null != file) {
            if(file.exists()) {
                String content = readFileText(file);
                if (null != content && content.length() > 0) {
                    {
                        try {
                            res = new Gson().fromJson(content, JsonObject.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            res = null;
                        }
                    }
                    if (null == res) {
                        try {
                            res = new JsonObject();
                            res.add("config", new Gson().fromJson(content, JsonArray.class));
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
        private String baseFileDirPath = null;

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

        public AppSetting.Builder setBaseFileDirPath(String baseFileDirPath) {
            this.baseFileDirPath = baseFileDirPath;
            return this;
        }

        public AppSetting build() {
            // check is webapp(in servlet container)
            {
                if(null == this.appName || this.appName.length() == 0) {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    if(null != classLoader) {
                        URL url = classLoader.getResource("");
                        if(null != url) {
                            if( url.getPath().contains("WEB-INF") ) {
                                this.appName = ServletContextStatic.getInstance().getServletContextName();
                            }
                        }
                    }
                    if(null == this.appName) {
                        try {
                            throw new Exception("AppSetting 必需設定一個有效的 AppName 參數");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }
            }
            // System.ou.println(this.appName)
            // check base dir path
            {
                FileFinder finder = new FileFinder.Builder().build();
                File baseFileDir = finder.find(this.baseFileDirName);
                if(null != baseFileDir && baseFileDir.exists()) {
                    this.baseFileDirPath = baseFileDir.getPath();
                    this.pathContext = new AppSetting.PathContext(baseFileDirPath, this.appName, System.getProperty("file.separator"));
                } else {
                    if(null != this.baseFileDirPath) {
                        File tmpBaseFileDir = new File(this.baseFileDirPath);
                        if(tmpBaseFileDir.exists()) {
                            this.pathContext = new AppSetting.PathContext(baseFileDirPath, this.appName, System.getProperty("file.separator"));
                        } else {
                            try {
                                throw new Exception("由 AppSetting 類別路徑往上尋找並未找到名為 " + this.baseFileDirName + " 的資料夾！");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            throw new Exception("由 AppSetting 類別路徑往上尋找並未找到名為 " + this.baseFileDirName + " 的資料夾！");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return new AppSetting(this.configDirName, this.configFileName, this.baseFileDirName, this.pathContext);
        }
    }

    /**
     * WebApp 預設資源路徑封裝
     */
    public static class PathContext {
        private final String tempDirPath;
        private final String uploadDirPath;
        private final String exportDirPath;
        private final String logDirPath;

        private PathContext(String baseFileDirPath, String webAppName, String dirSlash) {
            String tmpPath = baseFileDirPath + dirSlash + webAppName;
            tempDirPath = new File(tmpPath + dirSlash + "temp").getPath() + dirSlash;
            uploadDirPath = new File(tmpPath + dirSlash + "uploads").getPath() + dirSlash;
            exportDirPath = new File(tmpPath + dirSlash + "exports").getPath() + dirSlash;
            logDirPath = new File(tmpPath + dirSlash + "logs").getPath() + dirSlash;
        }

        public String getTempDirPath() {
            processPreMkDir(tempDirPath);
            return tempDirPath;
        }

        public String getUploadDirPath() {
            processPreMkDir(uploadDirPath);
            return uploadDirPath;
        }

        public String getExportDirPath() {
            processPreMkDir(exportDirPath);
            return exportDirPath;
        }

        public String getLogDirPath() {
            processPreMkDir(logDirPath);
            return logDirPath;
        }

        private void processPreMkDir(String path) {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return sbd.toString();
    }

}
