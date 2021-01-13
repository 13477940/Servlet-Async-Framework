package framework.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Public Thread Pool
 */
public class ThreadPoolStatic {

    private ThreadPoolStatic() {}

    public static ExecutorService getInstance() {
        if(null == StaticHolder.worker && !StaticHolder.isShutdown) {
            StaticHolder.worker = Executors.newCachedThreadPool();
        }
        if(null == StaticHolder.worker) {
            try {
                throw new Exception("已關閉 ThreadPool");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return StaticHolder.worker;
    }

    // execute with check rejected exception
    public static void execute(Runnable runnable) {
        if(null == StaticHolder.worker && !StaticHolder.isShutdown) {
            StaticHolder.worker = Executors.newCachedThreadPool();
        }
        if(null == StaticHolder.worker) {
            try {
                throw new Exception("已關閉 ThreadPool");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            StaticHolder.worker.execute(runnable);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            System.err.println("目前有太多的 Thread，Thread Pool 無法再接收處理新的 Runnable 事件");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 實作完整回收 ExecutorService 方式
    // http://blog.csdn.net/xueyepiaoling/article/details/61200270
    public static void shutdown() {
        ExecutorService worker = StaticHolder.worker;
        if (null != worker && !worker.isShutdown()) {
            // 設定 worker 已不能再接收新的請求
            worker.shutdown();
            try {
                // 設定一個 await 時限提供 thread 完成未完畢的工作的最後期限
                if (!worker.awaitTermination(3, TimeUnit.SECONDS)) {
                    // 當回收時限到期時，強制中斷所有 Thread 執行
                    worker.shutdownNow();
                }
            } catch (Exception e) {
                // e.printStackTrace();
                // 回收時發生錯誤時亦強制關閉所有 thread
                worker.shutdownNow();
            }
            StaticHolder.worker = null;
            StaticHolder.isShutdown = true;
        }
    }

    static class StaticHolder {
        // 藉由此 flag 判斷關閉依據
        public static boolean isShutdown = false;
        // ThreadPool 實例
        public static ExecutorService worker = null;
    }

}
