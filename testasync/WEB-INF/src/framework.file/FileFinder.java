package framework.file;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

/**
 * FileFinder 使用 find 主要是快速搜尋某個資料夾或檔案，
 * 當使用 find_all 才會遍歷每個子資料夾內部
 */
public class FileFinder {

    private File base_file; // 搜尋起始點

    private FileFinder(File base_file) {
        this.base_file = base_file;
        init_fn();
    }

    /**
     * 向上資料夾持續遍歷，每次只尋找同層級的資料夾內容
     */
    public File find(String file_name) {
        final String _file_name = file_name.toLowerCase(Locale.ENGLISH);
        boolean hit = false;
        Path now_target_path = Paths.get(base_file.getPath());
        File file = null;
        if( Files.isDirectory( now_target_path ) ) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream( now_target_path )) {
                for (Path _path : stream) {
                    String now_file_name = _path.getFileName().toString().toLowerCase(Locale.ENGLISH);
                    if (now_file_name.equalsIgnoreCase(_file_name)) {
                        file = _path.toFile();
                        hit = true;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 由於封裝成 jar 發布，所以搜尋初始階層可能是 .jar 本身，會有這個情況
            String now_file_name = now_target_path.getFileName().toString().toLowerCase(Locale.ENGLISH);
            if (now_file_name.equalsIgnoreCase(_file_name)) {
                file = now_target_path.toFile();
                hit = true;
            }
        }
        if(!hit) {
            Path parent_path = Paths.get(base_file.getPath()).getParent();
            if(null != parent_path) {
                this.base_file = parent_path.toFile();
                return find(file_name);
            }
        }
        return file;
    }

    public File find_all(String file_name) {
        String _file_name = file_name.toLowerCase(Locale.ENGLISH);
        File file = null;
        try {
            Optional<Path> res = Files.walk(Paths.get(base_file.getPath()))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).contains(_file_name))
                    .findFirst();
            if(res.isEmpty()) {
                Path parent_path = Paths.get(base_file.getPath()).getParent();
                if(null != parent_path) {
                    this.base_file = parent_path.toFile();
                    return find_all(file_name);
                }
            } else {
                file = res.get().toFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * 總的來說，使用 Java NIO 的 Paths 類別和 getProtectionDomain().getCodeSource().getLocation()
     * 方法更加通用和可靠，可以處理各種不同的路徑。而使用 Java 的 Class.getResource()
     * 方法則只適用於較簡單的路徑，並且可能會因為操作系統不同而產生問題。
     */
    private void init_fn() {
        if(null == base_file) {
            try {
                Path targetPath = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                base_file = targetPath.toFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Builder {
        private File baseFile = null;

        public FileFinder.Builder setBaseFile(File file) {
            this.baseFile = file;
            return this;
        }

        public FileFinder build() {
            return new FileFinder(this.baseFile);
        }
    }

}
