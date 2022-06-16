package framework.thread;

import java.util.concurrent.*;

/**
 * 不建議直接使用 Executors.newCachedThreadPool(); 因為其並未針對使用資源最大值設定限制，
 * 其他預設的 Executors 模式彈性也不好控制，所以建議直接採用 ThreadPoolExecutor 建立去制定。
 *
 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html
 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html
 * https://juejin.im/post/5c90698ef265da611d7423ae
 * http://givemepass-blog.logdown.com/posts/296960-how-to-use-the-threadpool
 * https://blog.csdn.net/zxysshgood/article/details/80499034
 *
 * TODO 目前此類別不採用，因為自建的 ThreadPool 會造成固定數量的執行緒使用完畢後就造成卡死的問題
 *
 * https://my.oschina.net/u/5079097/blog/5448512
 */
@Deprecated public class ThreadPool {

    private ExecutorService worker = null;

    private ThreadPool(Integer corePoolSize, Integer maximumPoolSize, Integer keepAliveTime, TimeUnit timeUnit, Integer blockingQueueSize) {
        initExecutorService(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueueSize);
    }

    public ExecutorService getInstance() {
        return worker;
    }

    // 實作完整回收 ExecutorService 方式
    // http://blog.csdn.net/xueyepiaoling/article/details/61200270
    public void shutdown() {
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
        }
    }

    // 初始化 ThreadPool
    private void initExecutorService(Integer corePoolSize, Integer maximumPoolSize, Integer keepAliveTime, TimeUnit timeUnit, Integer blockingQueueSize) {
        if(null == corePoolSize && null == maximumPoolSize && null == keepAliveTime && null == timeUnit && null == blockingQueueSize) {
            this.worker = Executors.newCachedThreadPool();
        } else {
            if(null == corePoolSize) {
                try {
                    throw new Exception("請設定 CorePoolSize，基礎 Thread 數量");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if(null == maximumPoolSize) {
                try {
                    throw new Exception("請設定 MaximumPoolSize，總 Thread 數量限制");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if(null == keepAliveTime) {
                try {
                    throw new Exception("請設定 KeepAliveTime，最大閒置時間，系統會回收超過 CorePoolSize 的 Thread 數量");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if(null == blockingQueueSize) {
                try {
                    throw new Exception("請設定 BlockingQueueSize，Thread Pool 滿載時的佇列大小");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            // https://stackoverflow.com/questions/17674931/arraydeque-and-linkedblockingdeque
            this.worker = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, new ArrayBlockingQueue<>(blockingQueueSize));
        }
    }

    public static class Builder {

        private Integer corePoolSize;
        private Integer maximumPoolSize;
        private Integer keepAliveTime;
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS; // default
        private Integer blockingQueueSize;

        /**
         * 閒置時因該保持的待命 Thread 數量
         */
        public ThreadPool.Builder setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        /**
         * 完全運作時限制的有效 Thread 數量
         */
        public ThreadPool.Builder setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        /**
         * 超過閒置的 Thread 可以待命多久後被回收
         */
        public ThreadPool.Builder setKeepAliveTime(int keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public ThreadPool.Builder setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * 通常會設定一個幾倍於 MaximumPoolSize 的數目，確保所有事件可以進入等待佇列，
         * 若設定很大的數值仍出現 rejectedExecution，則需要去思考使用過多 Thread 的問題，
         * 過多的 Thread 會導致 CPU 頻繁存取 Thread Context 造成效能低落
         */
        public ThreadPool.Builder setBlockingQueueSize(int blockingQueueSize) {
            this.blockingQueueSize = blockingQueueSize;
            return this;
        }

        public ThreadPool build() {
            return new ThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueueSize);
        }

    }

}
