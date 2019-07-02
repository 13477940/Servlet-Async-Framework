package framework.thread;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

public class ThreadPoolStatic {

    private ThreadPoolStatic() {}

    static {}

    public static ExecutorService getInstance() {
        return new WeakReference<>(ThreadPoolStatic.InstanceHolder.worker).get();
    }

    public static void shutdown() {
        if(null != ThreadPoolStatic.getInstance() && !ThreadPoolStatic.getInstance().isShutdown()) {
            ThreadPoolStatic.getInstance().shutdown();
        }
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
