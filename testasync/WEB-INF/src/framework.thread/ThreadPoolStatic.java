package framework.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolStatic
 * ＊修改後要進行壓力測試才能確保執行正常
 */
public class ThreadPoolStatic {

    private static ThreadPool threadPool;

    private ThreadPoolStatic() {}

    public static ExecutorService getInstance() {
        // TODO 於 tomcat 環境中使用會有無法即時回收的問題
        // if(null == threadPool) initNewThreadPool();
        // return threadPool.getInstance();
        return Executors.newCachedThreadPool();
    }

    /**
     * 當超過正常的負載量會提醒開發者
     */
    public static void execute(Runnable runnable) {
        try {
            ThreadPoolStatic.getInstance().execute(runnable);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            System.err.println("目前有太多的 Thread，Thread Pool 無法再接收處理新的 Runnable 事件");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        ThreadPoolStatic.getInstance();
        if(!ThreadPoolStatic.getInstance().isShutdown()) {
            ThreadPoolStatic.getInstance().shutdown();
        }
    }

    private static void initNewThreadPool() {
        threadPool = new DefaultThreadPool().getThreadPool();
    }

    private static class DefaultThreadPool {
        ThreadPool getThreadPool() {
            return new ThreadPool.Builder()
                    .setCorePoolSize(20) // 常駐 Thread 數量
                    .setMaximumPoolSize(200) // 可同時執行的 Thread 數量
                    .setKeepAliveTime(60)
                    .setTimeUnit(TimeUnit.SECONDS)
                    .setBlockingQueueSize(2000) // 事件排程池的限制數量
                    .build();
        }
    }

}
