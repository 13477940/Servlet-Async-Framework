package framework.web.http;

import okhttp3.OkHttpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class OkHttpClientStatic {

    private static OkHttpClient instance = null;

    static {}

    public static OkHttpClient getInstance() {
        if(null == instance || instance.dispatcher().executorService().isShutdown()) {
            instance = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return instance;
    }

    public static void shutdown() {
        if(null != instance) {
            ExecutorService threadPool = instance.dispatcher().executorService();
            if(null != threadPool && !threadPool.isShutdown()) {
                threadPool.shutdown();
                instance.connectionPool().evictAll();
                try {
                    instance.cache().close();
                    if(!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow();
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                    threadPool.shutdownNow();
                }
            }
        }
    }

}
