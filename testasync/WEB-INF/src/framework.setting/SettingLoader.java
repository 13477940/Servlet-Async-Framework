package framework.setting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 僅使用於 webapp config.json, config.properties 檔案解析
 * TODO 預計取代 AppSetting 取得參數值的功能
 */
public class SettingLoader {

    public JsonObject getProperties(File file) {
        JsonObject res = null;

        return res;
    }

    // 由 json object 檔案內容取得參數值
    public JsonObject getJsonObjectFile(File file) {
        JsonObject res = null;
        if(null != file) {
            if(file.exists()) {
                String content = readFileText(file);
                if (null != content && content.length() > 0) {
                    {
                        try {
                            res = new Gson().fromJson(content, JsonObject.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            res = null;
                        }
                    }
                    if (null == res) {
                        try {
                            res = new JsonObject();
                            res.add("config", new Gson().fromJson(content, JsonArray.class));
                        } catch (Exception e) {
                            e.printStackTrace();
                            res = null;
                        }
                    }
                    // 既不是 JSONObject 也不是 JSONArray
                    if (null == res) {
                        try {
                            throw new Exception(file.getName() + " 不是有效的 JSON 內容");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                try {
                    throw new Exception("getConfig() 指定的檔案不存在");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                throw new Exception("請輸入指定的 File 型態物件調用方法");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    // 讀取純文字類型檔案內容
    private String readFileText(File file) {
        StringBuilder sbd = new StringBuilder();
        FileInputStream fr = null;
        try {
            fr = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null == fr) {
            try {
                throw new Exception("FileInputStream 內容為空值");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fr, StandardCharsets.UTF_8))) {
            while(br.ready()) {
                String line = br.readLine();
                sbd.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return sbd.toString();
    }

}
