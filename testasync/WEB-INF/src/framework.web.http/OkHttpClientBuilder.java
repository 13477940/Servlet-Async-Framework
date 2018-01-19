package framework.web.http;

import okhttp3.OkHttpClient;

import java.util.concurrent.ExecutorService;

public class OkHttpClientBuilder {

    private static OkHttpClient instance = null;

    static {}

    public static OkHttpClient build() {
        if(null == instance) instance = new OkHttpClient();
        return instance;
    }

    /**
     * 關閉並回收 OkHttp 引用的 Thread Pool
     */
    public static void shutdown() {
        if(null == instance) return;
        instance.connectionPool().evictAll();
        ExecutorService es = instance.dispatcher().executorService();
        if(null == es) return;
        if(!es.isShutdown()) es.shutdown();
        try {
            if(!es.isShutdown()) es.shutdownNow();
            if(null != instance.cache() && !instance.cache().isClosed()) {
                instance.cache().close();
            }
        } catch (Exception e) {
            es.shutdownNow();
        }
    }

}
