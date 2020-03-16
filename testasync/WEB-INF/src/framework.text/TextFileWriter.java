package framework.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * 簡易純文字檔案寫入工具
 */
public class TextFileWriter {

    private File targetFile;

    private BufferedWriter bufferedWriter = null;
    private FileWriter fileWriter = null;

    private TextFileWriter(File targetFile, Boolean isAppend) {
        this.targetFile = targetFile;
        try {
            if(null == targetFile || !targetFile.exists()) {
                throw new Exception("需要使用 TextFileWriter 的檔案不存在");
            }
            // 內容是否為 append 模式決定在於 FileWriter 而不是 BufferedWriter
            // 此 append 模式影響到第一次寫入時的狀態，意即開檔後是清除重寫還是接續內容寫入
            fileWriter = new FileWriter(targetFile, isAppend);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(CharSequence content) {
        if(this.targetFile.canWrite()) {
            try {
                bufferedWriter.append(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                throw new Exception(this.targetFile.getName() + " 該檔案為無法寫入的狀態");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 關閉順序是固定的，不能更動
    public void close() {
        try {
            if(null != fileWriter) {
                fileWriter.flush();
            }
            if(null != bufferedWriter) {
                bufferedWriter.flush();
            }
            if(null != fileWriter) {
                fileWriter.close();
            }
            if(null != bufferedWriter) {
                bufferedWriter.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public static class Builder {
        private File targetFile = null;
        private Boolean isAppend = false; // 預設值

        public TextFileWriter.Builder setTargetFile(File targetFile) {
            this.targetFile = targetFile;
            return this;
        }

        public TextFileWriter.Builder setIsAppend(Boolean isAppend) {
            this.isAppend = isAppend;
            return this;
        }

        public TextFileWriter build() {
            return new TextFileWriter(this.targetFile, this.isAppend);
        }
    }

}
