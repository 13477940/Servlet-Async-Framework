package framework.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ThreadPoolStatic
 *
 * 2020-04-14 修正初始化過程及 shutdown 指令流程
 * 2020-07-29 修正關閉後再次調用 getInstance 會出錯的問題（即時重新建立一個 ThreadPool）
 */
public class ThreadPoolStatic {

    private static ThreadPool threadPool;

    private ThreadPoolStatic() {}

    public static ExecutorService getInstance() {
        // TODO 目前實測用自行創建的 ThreadPool 會有假死問題！
        // if(null == threadPool) initNewThreadPool();
        // return new WeakReference<>( threadPool.getInstance() ).get();
        return Executors.newCachedThreadPool();
        // return Executors.newSingleThreadExecutor();
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
        if(null != threadPool && !threadPool.getInstance().isShutdown()) {
            threadPool.shutdown();
            threadPool = null;
        }
    }

    private static void initNewThreadPool() {
        threadPool = new DefaultThreadPool().getThreadPool();
    }

    private static class DefaultThreadPool {
        ThreadPool getThreadPool() {
            return new ThreadPool.Builder()
                    .setCorePoolSize(32) // 常駐 Thread 數量
                    .setMaximumPoolSize(256) // 可同時執行的 Thread 數量
                    // .setKeepAliveTime(30)
                    // .setTimeUnit(TimeUnit.SECONDS)
                    .setBlockingQueueSize(4096) // 事件排程池的限制數量
                    .build();
        }
    }

}
