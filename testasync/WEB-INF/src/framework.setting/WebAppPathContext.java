package framework.setting;

import java.io.File;

public class WebAppPathContext {

    private String webinfPath = null;
    private String tempDirPath = null;
    private String uploadDirPath = null;
    private String exportDirPath = null;
    private String logDirPath = null;

    public WebAppPathContext(String tomcatParentDir, String baseDirName, String webAppName, String webAppPath, String dirSlash) {
        String appFileDirPath = tomcatParentDir + dirSlash + baseDirName + dirSlash + webAppName;
        webinfPath = new File(webAppPath + dirSlash + "WEB-INF").getPath() + dirSlash;
        tempDirPath = new File(appFileDirPath + dirSlash + "temp").getPath() + dirSlash;
        uploadDirPath = new File(appFileDirPath + dirSlash + "uploads").getPath() + dirSlash;
        exportDirPath = new File(appFileDirPath + dirSlash + "exports").getPath() + dirSlash;
        logDirPath = new File(appFileDirPath + dirSlash + "logs").getPath() + dirSlash;
    }

    public String getWebInfPath() { return webinfPath; }

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
