package framework.text;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TextFileReader {

    private String encoding = StandardCharsets.UTF_8.name();

    private final File file;

    private TextFileReader(String encoding, File file) {
        if(null != encoding && !encoding.isEmpty()) this.encoding = encoding;
        this.file = file;
    }

    /**
     * 分段讀取，較節省記憶體的實作
     */
    public void readToHandlerByLine(Handler handler) {
        if(null == handler) {
            try {
                throw new Exception("請設定一個 Handler 接收純文字內容回傳");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null == inputStream) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
            while(br.ready()) {
                String line = br.readLine();
                {
                    Bundle b = new Bundle();
                    b.put("src", "TextFileReader");
                    b.put("status", "content");
                    b.put("content", line);
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            {
                Bundle b = new Bundle();
                b.put("src", "TextFileReader");
                b.put("status", "done");
                b.put("content", "");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    /**
     * 完整載入純文字檔進記憶體暫存，較花費記憶體空間
     */
    public ArrayList<String> readAllByLine() {
        ArrayList<String> res = null;
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null == inputStream) return null;
        try ( BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, encoding) ) ) {
            while(br.ready()) {
                String line = br.readLine();
                if(null == res) res = new ArrayList<>();
                res.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return res;
    }

    public static class Builder {

        private String encoding = null;

        private File file = null;

        public Builder setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public TextFileReader build() {
            if(null == file || !file.exists()) {
                try {
                    throw new Exception("選擇的文字檔不存在");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new TextFileReader(encoding, file);
        }

    }

}
