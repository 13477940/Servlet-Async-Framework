package app.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.io.File;

/**
 * 建立一個可提供下載主機端任意檔案服務的 RequestHandler，
 * 應注意其安全性原則與路徑隱私達到保護主機檔案資源的機制
 */
public class FileHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            process_request();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        if( asyncActionContext.isFileAction() ) return false; // 排除 post multipart upload
        return ( "file_download".equalsIgnoreCase(asyncActionContext.getParameters().get("act")) );
    }

    private void process_request() {
        String file_path = requestContext.getParameters().get("file_path");
        if(null == file_path || file_path.isEmpty()) {
            JsonObject obj = new JsonObject();
            {
                obj.addProperty("status", "fail");
                obj.addProperty("msg_zht", "file_path：請輸入正確的檔案路徑");
            }
            requestContext.printToResponse(obj, new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    requestContext.complete();
                }
            });
            return;
        }
        process_file_download(file_path);
    }

    private void process_file_download(String file_path) {
        JsonObject resObj = getDirContent( file_path );
        try {
            // 如果是檔案則直接下載，若是資料夾則顯示其檔案列表
            String file_status = resObj.get("status").getAsString();
            if ("path_is_file".equalsIgnoreCase( file_status )) {
                // 當路徑為檔案時
                File file = new File( file_path );
                requestContext.outputFileToResponse(file_path, file.getName(), null, false, new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            } else {
                // 當路徑為資料夾時
                requestContext.printToResponse(resObj, new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            {
                JsonObject obj = new JsonObject();
                {
                    obj.addProperty("status", "fail");
                    obj.addProperty("msg_zht", "伺服器端發生錯誤");
                }
                requestContext.printToResponse(obj, new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            }
        }
    }

    // 取出該路徑下所有資料
    private JsonObject getDirContent(String path) {
        String _path = "/";
        if(null != path && !path.isEmpty()) _path = path;
        File target = new File(_path);
        JsonObject obj = new JsonObject();
        if(target.isFile()) {
            obj.addProperty("status", "path_is_file");
            obj.addProperty("msg_zh", "檔案類型的路徑");
            return obj;
        }
        JsonArray arr = new JsonArray();
        String[] list = target.list();
        if(null != list) {
            for(String str : list) {
                arr.add(str);
            }
        }
        obj.addProperty("status", "done");
        obj.add("content", arr);
        return obj;
    }

}
