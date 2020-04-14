package framework.thread;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/**
 * ThreadPoolStatic
 *
 * 2020-04-14 修正初始化過程及 shutdown 指令流程
 */
public class ThreadPoolStatic {

    private static final ThreadPool threadPool;
    private static final ExecutorService worker;

    private ThreadPoolStatic() {}

    static {
        threadPool = new DefaultThreadPool().getThreadPool();
        worker = threadPool.getInstance();
    }

    public static ExecutorService getInstance() {
        return new WeakReference<>( worker ).get();
    }

    public static void shutdown() {
        if(null != worker && !worker.isShutdown()) {
            threadPool.shutdown();
        }
    }

    private static class DefaultThreadPool {

        ThreadPool getThreadPool() {
            return new ThreadPool.Builder()
                    .setCorePoolSize(10)
                    .setMaximumPoolSize(1024)
                    .setKeepAliveTime(30)
                    .setTimeUnit(TimeUnit.SECONDS)
                    .setBlockingQueueSize(16384)
                    .build();
        }

    }

}
