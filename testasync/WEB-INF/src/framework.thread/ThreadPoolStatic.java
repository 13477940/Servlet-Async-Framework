package framework.thread;

import java.util.concurrent.*;

public class ThreadPoolStatic {

    private static ThreadPool threadPool;

    private ThreadPoolStatic() {}

    static {
        threadPool = new ThreadPool.Builder()
                .setCorePoolSize(10)
                .setMaximumPoolSize(1024)
                .setKeepAliveTime(30)
                .setTimeUnit(TimeUnit.SECONDS)
                .setBlockingQueueSize(16384)
                .build();
    }

    public static ExecutorService getInstance() {
        return ThreadPoolStatic.InstanceHolder.worker;
    }

    public static void shutdown() {
        ThreadPoolStatic.threadPool.shutdown();
    }

    private static class InstanceHolder {
        static ExecutorService worker = ThreadPoolStatic.threadPool.getInstance();
    }

}
