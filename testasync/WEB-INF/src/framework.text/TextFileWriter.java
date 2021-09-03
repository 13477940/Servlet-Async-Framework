package framework.text;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * 簡易純文字檔案寫入工具
 *
 * https://my.oschina.net/u/3839951/blog/4890777
 */
public class TextFileWriter {

    private final File targetFile;
    private final boolean isAppend;
    private FileChannel fileChannel = null; // for nio

    private TextFileWriter(File targetFile, Boolean isAppend) {
        this.targetFile = targetFile;
        this.isAppend = isAppend;
        try {
            if(null == targetFile || !targetFile.exists()) {
                throw new Exception("需要使用 TextFileWriter 的檔案不存在");
            }
            fileChannel = new WeakReference<>( new RandomAccessFile(this.targetFile, "rw").getChannel() ).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(CharSequence content) {
        if(this.targetFile.canWrite()) {
            try {
                if(this.isAppend) fileChannel.position(fileChannel.size());
                fileChannel.write(ByteBuffer.wrap(content.toString().getBytes(StandardCharsets.UTF_8)));
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

    /**
     * 添加系統文檔換行
     */
    public void next_line() {
        if(this.targetFile.canWrite()) {
            try {
                if(this.isAppend) fileChannel.position(fileChannel.size());
                String sys_new_line = System.lineSeparator();
                fileChannel.write(ByteBuffer.wrap(sys_new_line.getBytes(StandardCharsets.UTF_8)));
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
            if(null != fileChannel) {
                fileChannel.close();
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
