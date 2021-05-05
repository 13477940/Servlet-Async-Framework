package framework.setting;

import framework.file.FileFinder;

import java.io.File;

public class PathContext {

    private String tempDirPath;
    private String uploadDirPath;
    private String exportDirPath;
    private String logDirPath;

    private final String hostOS;
    private final String dirSlash;

    private boolean initStatus = false; // 記錄是否正確建立

    /**
     * targetClass 通常為當下的那個 class 即可，會透過 FileFinder 由上尋找需要使用的資料夾路徑
     */
    public PathContext(Class targetClass, String webAppName) {
        {
            hostOS = System.getProperty("os.name");
            if ( hostOS.toLowerCase().contains("windows") ) {
                dirSlash = "\\\\";
            } else {
                // if Windows OS
                dirSlash = System.getProperty("file.separator");
            }
        }
        {
            if ( null == webAppName || webAppName.length() == 0 ) {
                try {
                    throw new Exception("請輸入正確的 webapp name，一般專案資料夾名稱");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        {
            File targetFile = new File(targetClass.getProtectionDomain().getCodeSource().getLocation().getPath());
            FileFinder finder = new FileFinder.Builder().setBaseFile(targetFile).build();
            File mainDir = finder.find("TomcatAppFiles");
            if(null == mainDir || !mainDir.exists() || !mainDir.isDirectory()) {
                try {
                    throw new Exception("請於同一個磁碟區之中建立 TomcatAppFiles 資料夾");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            String tmpPath = finder.find("TomcatAppFiles") + dirSlash + webAppName;
            tempDirPath = new File(tmpPath + dirSlash + "temp").getPath() + dirSlash;
            uploadDirPath = new File(tmpPath + dirSlash + "uploads").getPath() + dirSlash;
            exportDirPath = new File(tmpPath + dirSlash + "exports").getPath() + dirSlash;
            logDirPath = new File(tmpPath + dirSlash + "logs").getPath() + dirSlash;
        }
        {
            initStatus = true;
        }
    }

    public String getDirSlash() {
        if(!checkIsInit()) return null;
        return dirSlash;
    }

    public String getHostOS() {
        if(!checkIsInit()) return null;
        return hostOS;
    }

    public String getTempDirPath() {
        if(!checkIsInit()) return null;
        processPreMkDir(tempDirPath);
        return tempDirPath;
    }

    public String getUploadDirPath() {
        if(!checkIsInit()) return null;
        processPreMkDir(uploadDirPath);
        return uploadDirPath;
    }

    public String getExportDirPath() {
        if(!checkIsInit()) return null;
        processPreMkDir(exportDirPath);
        return exportDirPath;
    }

    public String getLogDirPath() {
        if(!checkIsInit()) return null;
        processPreMkDir(logDirPath);
        return logDirPath;
    }

    private boolean checkIsInit() {
        if(!initStatus) {
            try {
                throw new Exception("請先完成 init 方法");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private static void processPreMkDir(String path) {
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
