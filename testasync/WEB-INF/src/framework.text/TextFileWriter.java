package framework.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class TextFileWriter {

    private File target = null;
    private boolean isAppend = true;

    private BufferedWriter writer = null;
    private FileWriter fileWriter = null;

    public TextFileWriter(File target) {
        initWriter(target, true);
    }
    public TextFileWriter(File target, boolean isAppend) {
        initWriter(target, isAppend);
    }

    /**
     * 建立 TextFileWriter 實例
     */
    public BufferedWriter build() {
        try {
            fileWriter = new FileWriter(target, isAppend);
            writer = new BufferedWriter(fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return writer;
    }

    /**
     * 寫入字串內容
     */
    public void write(CharSequence charSequence) {
        try {
            if(!target.canWrite()) {
                System.err.println(target.getName() + " 該檔案為不可被寫入的狀態");
            } else {
                if (null != writer) {
                    writer.append(charSequence);
                } else {
                    System.err.println("請先建立 TextFileWriter 再進行 write 指令");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 關閉 TextFileWriter
     */
    public void close() {
        try {
            if(null != writer) {
                if(null != fileWriter) {
                    fileWriter.flush();
                }
                writer.flush();
                if(null != fileWriter) {
                    fileWriter.close();
                }
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initWriter(File target, boolean isAppend) {
        if(target.exists()) {
            this.target = target;
        } else {
            try {
                boolean status = target.createNewFile();
                if(status) this.target = target;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.isAppend = isAppend;
    }

}
