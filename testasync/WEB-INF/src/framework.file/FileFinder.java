package framework.file;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * FileFinder 並不是萬能的，搜尋原理為每一個資料夾階層查詢一次檔案名稱，
 * 依序往上尋找父階層與其指定資料夾下一階層的內容是否具有查詢目標。
 * 要注意此處不能使用 AppSetting 要不然會有重複嵌套的邏輯錯誤發生，
 * 主要是因為 AppSetting 改為 new Builder() 模式，所以每次都會重新建立實例，
 * 如果在此類別引用 AppSetting 的情況下會造成無窮迴圈的錯誤。
 * https://stackoverflow.com/questions/37902711/getting-the-path-of-a-running-jar-file-returns-rsrc
 */
public class FileFinder {

    private String dirSlash = null;

    private File baseFile = null; // 搜尋起始點

    private FileFinder(File baseFile) { initFileFinder(baseFile); }

    // 該階層與往上查找的階層內的：資料夾名稱 or 檔案名稱
    public File find(String fileName) {
        if(null == fileName || fileName.length() == 0) return null;
        // for Windows directory root path
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            if("/".equals(fileName) || "\\".equals(fileName)) {
                URL resourceURL = this.getClass().getClassLoader().getResource("");
                if(null != resourceURL) {
                    String tmpPath = resourceURL.getPath();
                    String[] arr = tmpPath.split(":");
                    String diskFlag = arr[0];
                    return new File(diskFlag + ":/");
                } else {
                    try {
                        throw new Exception("無法取得目前類別於 Windows OS 根目錄的 ClassLoader 路徑！");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        File res;
        if(fileName.contains(dirSlash)) {
            res = new File(fileName);
        } else {
            res = findFunc(this.baseFile, null, fileName);
        }
        if(null == res) return null;
        if(!res.exists()) return null;
        return res;
    }

    // 該階層與往上查找的階層內的：子資料夾路徑 + 資料夾名稱 or 檔案名稱
    public File find(String fileDirectoryName, String fileName) {
        return findFunc(this.baseFile, fileDirectoryName, fileName);
    }

    // 藉由迴圈替代遞迴實作訪問資料夾的動作
    private File findFunc(File baseFile, String fileDirectoryName, String fileName) {
        File res = null; // result
        File tempFile = baseFile;
        while(true) {
            if(null == tempFile) break; // 已到磁區最頂層時
            if(null != res) break; // 已找到最接近的相符檔案時
            {
                if(null == fileDirectoryName) {
                    res = processFile(tempFile, fileName);
                } else {
                    res = processDirectory(tempFile, fileDirectoryName, fileName);
                }
                if(null == res) tempFile = tempFile.getParentFile(); // update to parent
            }
        }
        return res;
    }

    // 針對檔案處理方式
    private File processFile(File baseFile, String fileName) {
        if(null == baseFile) return null; // break;
        File res = null; // result
        if(baseFile.isDirectory()) {
            File[] list = baseFile.listFiles();
            if(null == list) return null;
            for(File pFile : list) {
                if(fileName.equalsIgnoreCase(pFile.getName())) {
                    res = pFile;
                }
            }
        } else {
            if(fileName.equalsIgnoreCase(baseFile.getName())) {
                res = baseFile;
            }
        }
        return res;
    }

    // 針對資料夾處理方式
    private File processDirectory(File baseFile, String fileDirectoryName, String fileName) {
        if (null == baseFile) return null; // break;
        File pFile = new File(baseFile.getPath()); // current directory
        // pFile 為向上查找的路徑，並加上想要尋找的子路徑（可以藉由 file.separator 多層存取）作為查詢條件
        if (fileDirectoryName.contains(dirSlash)) {
            pFile = new File(pFile.getPath() + dirSlash + fileDirectoryName);
        }
        if (pFile.isDirectory()) {
            File[] list = pFile.listFiles();
            if (null == list) return null;
            for (File listFile : list) {
                if (fileDirectoryName.equals(listFile.getName())) {
                    return processFile(new File(pFile.getPath() + dirSlash + fileDirectoryName), fileName);
                }
                // 如果直接輸入兩階層含以上的資料夾路徑會直接找到該檔案
                if (fileDirectoryName.equals(fileName)) {
                    return new File(pFile.getPath() + dirSlash + fileDirectoryName);
                }
            }
        }
        return null;
    }

    // 設定系統參數
    private void setHostInfo() {
        this.dirSlash = System.getProperty("file.separator"); // 系統資料夾階層符號
    }

    // 初始化
    private void initFileFinder(File baseFile) {
        setHostInfo();
        if(null == baseFile) {
            URL resourceURL = this.getClass().getClassLoader().getResource("");
            // URL resourceURL = Thread.currentThread().getContextClassLoader().getResource("");
            if(null != resourceURL) {
                String classPath = URLDecoder.decode(resourceURL.getPath(), StandardCharsets.UTF_8);
                this.baseFile = new File(classPath);
            } else {
                try {
                    throw new Exception("FileFinder 無法取得目前類別的 ClassLoader 路徑！");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            this.baseFile = baseFile;
        }
    }

    public static class Builder {
        private File baseFile = null;

        /**
         * 設定搜尋起始點，未設定則由目前的類別資料夾層級向上查找
         */
        public FileFinder.Builder setBaseFile(File file) {
            this.baseFile = file;
            return this;
        }

        public FileFinder build() {
            return new FileFinder(this.baseFile);
        }
    }

}
