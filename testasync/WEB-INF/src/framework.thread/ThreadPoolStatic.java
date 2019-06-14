package framework.thread;

import java.util.concurrent.*;

public class ThreadPoolStatic {

    private ThreadPoolStatic() {}

    static {}

    public static ExecutorService getInstance() {
        return ThreadPoolStatic.InstanceHolder.worker;
    }

    public static void shutdown() {
        ThreadPoolStatic.getInstance().shutdown();
    }

    private static class InstanceHolder {
        static ExecutorService worker = new DefaultThreadPool().getThreadPool().getInstance();
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
