package top.codecrab.upload.utils;

import top.codecrab.upload.config.Constant;

import java.util.concurrent.*;

/**
 * 懒汉模式加载线程池
 *
 * @author codecrab
 * @since 2021年08月21日 下午 05:03
 */
public class ThreadPoolFactory {

    public static ExecutorService getInstance() {
        return ThreadPoolFactory.Inner.INSTANCE;
    }

    private static class Inner {

        private static final ExecutorService INSTANCE;

        static {
            INSTANCE = new ThreadPoolExecutor(
                Constant.CONCURRENT_FILE_NUMBER, Constant.MAX_CONCURRENT_FILE_NUMBER,
                1000, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(Constant.MAX_CONCURRENT_FILE_NUMBER << 1),
                callable -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(callable);
                    thread.setDaemon(true);
                    return thread;
                }
            );
        }
    }

}
