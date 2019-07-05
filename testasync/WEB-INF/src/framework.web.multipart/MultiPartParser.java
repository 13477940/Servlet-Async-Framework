package framework.web.multipart;

import framework.random.RandomServiceStatic;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * implement by UrielTech.com TomLi.
 * 190704 初步模塊化程式碼，增加可讀性
 * 190705 重構完成並修正可能發生的 byte 取碼錯誤問題
 */
public class MultiPartParser {

    private AsyncContext asyncContext;

    private File file;
    private File tempDir;
    private String dirSlash;

    private MultiPartParser(ServletContext servletContext, AsyncContext asyncContext, File file, File repository) {
        {
            String hostOS = System.getProperty("os.name");
            this.dirSlash = System.getProperty("file.separator");
            if(hostOS.toLowerCase().contains("windows")) { this.dirSlash = "\\\\"; }
        }
        this.asyncContext = asyncContext;
        this.file = file;
        this.tempDir = repository;
        if(null == this.tempDir) this.tempDir = new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString());
    }

    /**
     * Like Apache FileItem Parser For Async Implement
     * https://www.ibm.com/developerworks/cn/java/fileup/index.html
     * TODO 需要思考如何降低 bigO(n) 的邏輯複雜度，目前此方法會隨著上傳檔案大小而呈現線性增加處理時間
     * TODO 需要思考 Integer 型態對於上傳檔案大小的影響為何？
     */
    public FileItemList parse() {
        if(null == file || !file.exists()) return null;
        FileItemList fileItemList;
        byte[] byte_boundary = getBoundaryByteArray();
        ArrayList<Integer> list_boundary_index = getBoundaryIndexList(byte_boundary);
        if(list_boundary_index.size() <= 2) return null;
        fileItemList = getFileItemList(byte_boundary, list_boundary_index);
        return fileItemList;
    }

    private FileItemList getFileItemList(byte[] byte_boundary, ArrayList<Integer> list_boundary_index) {
        ByteProcessor byteProcessor = new ByteProcessor();
        try ( BufferedInputStream inputStream = new WeakReference<>( new BufferedInputStream( new FileInputStream( this.file ) ) ).get() ) {
            assert null != inputStream;
            byte[] buffer = new byte[1];
            int index = 0;
            int step = 0;
            final int boundary_length = byte_boundary.length;
            int boundary_match_count = 0;
            int binary_start = 0;
            int binary_end = 0;
            while(0 <= inputStream.read(buffer)) {
                byte b = buffer[0];
                if(list_boundary_index.size() <= step) break;
                if(index >= list_boundary_index.get(step) && index <= list_boundary_index.get(step + 1)) {
                    if(byte_boundary[boundary_match_count] == b) {
                        boundary_match_count++;
                        if(boundary_match_count == boundary_length) {
                            boundary_match_count = 0;
                            step += 2;
                            if(list_boundary_index.size() > step) {
                                binary_start = list_boundary_index.get(step - 1);
                                binary_end = list_boundary_index.get(step);
                            }
                        }
                    }
                } else {
                    if(index >= binary_start && index < binary_end) {
                        // content byte
                        byteProcessor.processFormDataContent(b, index, list_boundary_index);
                    }
                }
                index++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return byteProcessor.getFileItemList();
    }

    // TODO bigO(n) - 藉由逐個 byte 讀取檢查 boundary 斷點處作為切割上傳內容二進位座標點
    // ArrayList<Integer> = [ boundary_頭, boundary_尾, boundary_頭, boundary_尾 ... ]
    // 兩個座標代表一筆 boundary 字串，四個座標兩兩之間（2:form-data:2）代表一個 form-data 檔案的內容分界線
    private ArrayList<Integer> getBoundaryIndexList(byte[] byte_boundary) {
        ArrayList<Integer> res = new ArrayList<>();
        try ( BufferedInputStream inputStream = new WeakReference<>( new BufferedInputStream( new FileInputStream( this.file ) ) ).get() ) {
            byte[] buffer = new byte[1];
            int nowMatchCount = 0;
            int checkToNextByteCount = 0;
            int index = 0;
            // 逐個 byte 檢查，連續命中時才累計
            while(true) {
                if(null == inputStream) break;
                if(-1 >= inputStream.read(buffer)) break;
                String tmp = String.valueOf(buffer[0]);
                String check = String.valueOf(byte_boundary[nowMatchCount]); // 目前命中第幾個
                if(check.equals(tmp)) { nowMatchCount++; }
                checkToNextByteCount++;
                index++;
                if(nowMatchCount != checkToNextByteCount) { nowMatchCount = 0; checkToNextByteCount = 0; } // when miss
                if(nowMatchCount == byte_boundary.length) {
                    { nowMatchCount = 0; checkToNextByteCount = 0; }
                    // boundary_start.add(index - byte_boundary.length);
                    // boundary_end.add(index);
                    res.add(index - byte_boundary.length);
                    res.add(index);
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return res;
    }

    // 取得 boundary 字串 byte 的比對值
    private byte[] getBoundaryByteArray() {
        String contentType = asyncContext.getRequest().getContentType();
        int start = contentType.indexOf("boundary=");
        int boundaryLen = "boundary=".length();
        String boundary = contentType.substring(start + boundaryLen);
        boundary = "--" + boundary;
        return boundary.getBytes(StandardCharsets.UTF_8);
    }

    // 針對上傳檔案逐個 byte 進行處理
    private class ByteProcessor {

        private int parseContentType = 0; // 0 = title, 1 = mime, 2 = binary
        private boolean validByte = false;
        private ArrayList<Byte> tmpByteList = new ArrayList<>();
        private boolean watchByte = false;
        // private Byte prevByte = null;
        private boolean isBinary = false; // 連續兩次 13 10 命中後才開啟檔案寫入模式

        private FileItemList fileItemList;
        private FileItem fileItem = null;
        private OutputStream tmpOutputStream = null;
        private int fileSize = 0;

        ByteProcessor() {
            fileItemList = new WeakReference<>(new FileItemList()).get();
        }

        private FileItemList getFileItemList() {
            return this.fileItemList;
        }

        // 開始到結束代表一個 form-data 檔案的結束，如果只是純參數傳值也是這樣處理
        private void processFormDataContent(Byte b, int index, ArrayList<Integer> list_boundary_index) {
            boolean isStart = list_boundary_index.contains(index-2);
            boolean isEnd = list_boundary_index.contains(index+3);
            // start byte
            if(isStart) {
                validByte = true;
                fileItem = newFileItem();
                try {
                    tmpOutputStream = new BufferedOutputStream(new FileOutputStream(fileItem.getFile()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // default status
            {
                if ( validByte ) {
                    if ( 0 == parseContentType ) {
                        if ( watchByte ) {
                            if ( 10 == b ) {
                                byte[] tmp = new byte[tmpByteList.size()];
                                for(int i = 0, len = tmpByteList.size(); i < len; i++) {
                                    tmp[i] = tmpByteList.get(i);
                                }
                                String content_disposition = new String(tmp, StandardCharsets.UTF_8);
                                tmpByteList.clear();
                                watchByte = false;
                                if(content_disposition.contains("filename=")) {
                                    parseContentType = 1;
                                    {
                                        fileItem.setIsFormField(false);
                                        fileItem.setFieldName( getFieldName(content_disposition) );
                                        fileItem.setName( getFileName(content_disposition) );
                                    }
                                    return;
                                } else {
                                    parseContentType = 2;
                                    {
                                        fileItem.setIsFormField(true);
                                        fileItem.setFieldName( getFieldName(content_disposition) );
                                        fileItem.setContentType("text/plain");
                                    }
                                    return;
                                }
                            }
                        } else {
                            if ( 13 == b ) {
                                watchByte = true;
                                // prevByte = b;
                            } else {
                                tmpByteList.add(b);
                            }
                        }
                    }
                    if ( 1 == parseContentType ) {
                        if ( watchByte ) {
                            if ( 10 == b ) {
                                byte[] tmp = new byte[tmpByteList.size()];
                                for(int i = 0, len = tmpByteList.size(); i < len; i++) {
                                    tmp[i] = tmpByteList.get(i);
                                }
                                String content_type = new String(tmp, StandardCharsets.UTF_8);
                                fileItem.setContentType( getMime(content_type) );
                                tmpByteList.clear();
                                watchByte = false;
                                parseContentType = 2;
                                return;
                            }
                        } else {
                            if (13 == b) {
                                watchByte = true;
                                // prevByte = b;
                            } else {
                                tmpByteList.add(b);
                            }
                        }
                    }
                    if ( 2 == parseContentType ) {
                        if ( watchByte ) {
                            if (10 == b) {
                                watchByte = false;
                                isBinary = true;
                            }
                        } else {
                            if(isBinary) {
                                // output binary byte
                                try {
                                    fileSize++;
                                    tmpOutputStream.write(b);
                                } catch (Exception e) {
                                    // e.printStackTrace();
                                }
                            } else {
                                if (13 == b) {
                                    watchByte = true;
                                    // prevByte = b;
                                }
                            }
                        }
                    }
                }
            }
            // end byte
            if(isEnd) {
                parseContentType = 0;
                isBinary = false;
                validByte = false;
                watchByte = false;
                {
                    fileItem.setSize(fileSize);
                    try {
                        tmpOutputStream.flush();
                        tmpOutputStream.close();
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    fileItemList.add(fileItem);
                    fileItem = null;
                    fileSize = 0;
                }
            }
        }

        private FileItem newFileItem() {
            FileItem fileItem = new FileItem.Builder().build();
            String fileName = RandomServiceStatic.getInstance().getTimeHash(24).toUpperCase();
            String filePath = MultiPartParser.this.tempDir.toPath() + MultiPartParser.this.dirSlash;
            File file = null;
            try {
                file = File.createTempFile(fileName, null, new File(filePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(null != file && file.exists()) file.deleteOnExit();
            fileItem.setFile(file);
            return new WeakReference<>(fileItem).get();
        }

        private String getFieldName(String content_disposition) {
            int s = content_disposition.indexOf("name=\"");
            int e = content_disposition.length() - 1;
            if(content_disposition.contains("; filename")) {
                e = content_disposition.indexOf("; filename") - 1;
            }
            return content_disposition.substring(s+6, e);
        }

        private String getFileName(String content_disposition) {
            int s = 0;
            if(content_disposition.contains("; filename")) {
                s = content_disposition.indexOf("filename=\"");
            }
            int e = content_disposition.length() - 1;
            return content_disposition.substring(s+10, e);
        }

        private String getMime(String content_type) {
            int s = content_type.indexOf("Content-Type: ");
            return content_type.substring(s+14);
        }

    }

    public static class Builder {

        private ServletContext servletContext = null;
        private AsyncContext asyncContext = null;
        private File file = null;
        private File repository = null; // 暫存資料夾

        public MultiPartParser.Builder setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        public MultiPartParser.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
            return this;
        }

        public MultiPartParser.Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public MultiPartParser.Builder setRepository(File repository) {
            this.repository = repository;
            return this;
        }

        public MultiPartParser build() {
            return new MultiPartParser(servletContext, asyncContext, file, repository);
        }

    }

}
