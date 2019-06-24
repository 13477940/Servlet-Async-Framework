package framework.web.multipart;

import framework.random.RandomServiceStatic;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * implement by UrielTech.com TomLi.
 * TODO 需要思考如何降低 bigO(n) 的邏輯複雜度，目前此方法會隨著上傳檔案大小而呈現線性增加處理時間
 */
public class MultiPartParser {

    private AsyncContext asyncContext;

    private File file;
    private File repository;
    private String dirSlash = System.getProperty("file.separator");

    private MultiPartParser(ServletContext servletContext, AsyncContext asyncContext, File file, File repository) {
        this.asyncContext = asyncContext;
        this.file = file;
        this.repository = repository;
        if(null == this.repository) {
            this.repository = new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString());
        }
    }

    /**
     * Like Apache FileItem Parser For Async Implement
     * https://www.ibm.com/developerworks/cn/java/fileup/index.html
     */
    public FileItemList parse() {
        if(null == file || !file.exists()) return null;
        byte[] byte_boundary;
        {
            String contentType = asyncContext.getRequest().getContentType();
            // contentType.startsWith("multipart/form-data");
            int start = contentType.indexOf("boundary=");
            int boundaryLen = "boundary=".length();
            String boundary = contentType.substring(start + boundaryLen);
            boundary = "--" + boundary;
            byte_boundary = boundary.getBytes(StandardCharsets.UTF_8);
        }
        ArrayList<Integer> boundary_start = new ArrayList<>(); // boundary 座標集合
        ArrayList<Integer> boundary_end = new ArrayList<>(); // boundary 座標集合
        // bigO(n) - 藉由逐個 byte 讀取檢查 boundary 斷點處作為切割上傳內容二進位座標點
        {
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
                        boundary_start.add(index - byte_boundary.length);
                        boundary_end.add(index);
                    }
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        FileItemList fileItems = new FileItemList();
        // 空的 POST MultiPart 時
        if(boundary_start.size() == 1 && boundary_end.size() == 1) return fileItems;
        // bigO(n) - 逐個 byte 進行處理，將上傳內容二進位寫入成檔案形式
        BufferedOutputStream fOut = null;
        {
            // 藉由 boundary 起始及結束位置分割出每個檔案範圍中的內容 byte
            try ( BufferedInputStream inputStream = new WeakReference<>( new BufferedInputStream( new FileInputStream( this.file ) ) ).get() ) {
                HashMap<Integer, Integer> boundary_index = new HashMap<>();
                {
                    for(int tmp : boundary_start) boundary_index.put(tmp, tmp);
                    for(int tmp : boundary_end) boundary_index.put(tmp, tmp);
                }
                byte[] buffer = new byte[1];
                int index = 0;
                int headIndx = 1;
                int tailIndx = 0;
                int status = 0; // 0 = off, 1 = on;
                int type = 1; // 0 = context, 1 = binary;
                int nowMatchCount = 0;
                int checkToNextByteCount = 0;
                int fileByteCount = 0;
                int prevIndex = -1; // 防止結尾時多製作一個空白檔案
                String[] chk = { "13", "10" };
                ArrayList<Byte> list_ctx = new ArrayList<>(); // file context byte temp
                FileItem.Builder fileItemBuilder = new FileItem.Builder();
                File tmpFile = null; // 初始為空值防止第一個檔案為空內容
                while(true) {
                    if(null == inputStream) break;
                    if(-1 >= inputStream.read(buffer)) break;
                    if(index >= boundary_end.get(tailIndx) && index < boundary_start.get(headIndx)) {
                        String tmp = String.valueOf(buffer[0]);
                        String check = chk[nowMatchCount];
                        if(tmp.equals(check)) {
                            nowMatchCount++;
                        }
                        checkToNextByteCount++;
                        {
                            // 如果是資料區塊
                            if (status == 1) {
                                // 0 = context, 1 = binary;
                                if (type == 0) {
                                    list_ctx.add(buffer[0]);
                                } else {
                                    if(!boundary_end.contains(index + byte_boundary.length +1) && !boundary_end.contains(index + byte_boundary.length +2)) {
                                        assert null != fOut;
                                        fOut.write(buffer[0]);
                                        fileByteCount++;
                                    }
                                }
                            }
                        }
                    }
                    // header index 會提前 +1 所以要注意取用範圍值
                    if(index > boundary_start.get(headIndx)) {
                        if(headIndx >= boundary_start.size() -1) break;
                        headIndx++;
                        tailIndx++;
                    }
                    index++;
                    // when miss
                    if(nowMatchCount != checkToNextByteCount) {
                        nowMatchCount = 0;
                        checkToNextByteCount = 0;
                    }
                    // 命中時
                    if(nowMatchCount == chk.length) {
                        { nowMatchCount = 0; checkToNextByteCount = 0; }
                        // end point switch
                        if(status == 0) {
                            status = 1;
                        } else {
                            if(type == 0) {
                                status = 0;
                            } else {
                                // 避免檔案中 byte 具有 1310 造成干擾
                                if(boundary_index.containsKey(index)) {
                                    status = 0;
                                }
                            }
                        }
                        // 資料區段時
                        if(status == 1) {
                            // 藉由 boundary index 判斷是否為檔案內的 1310 分割符，才不會造成切割出錯
                            if (type == 1 && boundary_index.containsKey(index - 2)) {
                                type = 0;
                            } else {
                                type = 1;
                            }
                        }
                        // type switch
                        if(status == 0) {
                            if (type == 0) {
                                {
                                    if(prevIndex != headIndx) {
                                        prevIndex = headIndx;
                                        if(null != fOut) fOut.close();
                                        tmpFile = File.createTempFile(RandomServiceStatic.getInstance().getTimeHash(4), null, new File(this.repository.getPath() + dirSlash));
                                        tmpFile.deleteOnExit();
                                        fOut = new WeakReference<>( new BufferedOutputStream( new FileOutputStream( tmpFile ) ) ).get();
                                    }
                                }
                                {
                                    byte[] bytes = new byte[list_ctx.size()];
                                    int iTmp = 0;
                                    // 統一去除 context 尾部多餘的 1310
                                    list_ctx.remove(list_ctx.size()-1);
                                    list_ctx.remove(list_ctx.size()-1);
                                    for (byte b : list_ctx) {
                                        bytes[iTmp] = b;
                                        iTmp++;
                                    }
                                    String checkStr = new String(bytes, StandardCharsets.UTF_8);
                                    if(checkStr.contains("Content-Type")) {
                                        String hText = "Content-Type: ";
                                        String mime = checkStr.substring(hText.length());
                                        fileItemBuilder.setContentType(mime);
                                    } else {
                                        // form name
                                        {
                                            int hIndx = checkStr.indexOf("filename=");
                                            String hText = "Content-Disposition: form-data; name=";
                                            String fieldName;
                                            if( checkStr.indexOf(hIndx) > -1 ) {
                                                fieldName = checkStr.substring(0, hIndx - 2);
                                                fieldName = fieldName.substring(hText.length()+1);
                                                fieldName = fieldName.substring(0, fieldName.indexOf("\""));
                                            } else {
                                                fieldName = checkStr.substring(hText.length()+1);
                                                fieldName = fieldName.substring(0, fieldName.indexOf("\""));
                                            }
                                            fileItemBuilder.setFieldName(fieldName);
                                        }
                                        // file name
                                        if(checkStr.contains("filename=")) {
                                            status = 1;
                                            type = 0;
                                            int hIndx = checkStr.indexOf("filename=");
                                            String fileName = checkStr.substring(hIndx + ("filename=".length() + 1));
                                            fileName = fileName.substring(0, fileName.indexOf("\""));
                                            fileItemBuilder.setName(fileName);
                                            fileItemBuilder.setIsFormField(false);
                                        } else {
                                            fileItemBuilder.setIsFormField(true);
                                        }
                                    }
                                }
                            } else {
                                {
                                    FileItem fileItem = fileItemBuilder.build();
                                    if (null != fileItem.getFieldName()) {
                                        fileItemBuilder.setFile(tmpFile);
                                        fileItemBuilder.setSize(fileByteCount);
                                        fileItems.add(fileItemBuilder.build());
                                        fileByteCount = 0;
                                    }
                                    fileItemBuilder = new FileItem.Builder();
                                }
                            }
                        }
                        list_ctx.clear();
                    }
                } // while end.
                list_ctx.clear();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(null != fOut) fOut.close();
                } catch (Exception ex) {
                    // ex.printStackTrace();
                }
            }
        }
        return fileItems;
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
