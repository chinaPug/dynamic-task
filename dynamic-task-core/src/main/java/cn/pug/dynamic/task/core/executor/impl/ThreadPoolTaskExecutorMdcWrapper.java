package cn.pug.dynamic.task.core.executor.impl;

import cn.pug.dynamic.task.core.executor.logging.LogContext;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class ThreadPoolTaskExecutorMdcWrapper extends ThreadPoolTaskExecutor {


    public ThreadPoolTaskExecutorMdcWrapper(int corePoolSize,
                                            int maximumPoolSize,
                                            int keepAliveSeconds,
                                            int queueCapacity,
                                            ThreadFactory threadFactory,
                                            RejectedExecutionHandler handler) {
        this.setCorePoolSize(corePoolSize);
        this.setMaxPoolSize(maximumPoolSize);
        this.setQueueCapacity(queueCapacity);
        this.setKeepAliveSeconds(keepAliveSeconds);
        this.setThreadFactory(threadFactory);
        this.setRejectedExecutionHandler(handler);
        this.initialize();
    }


    @Override
    public void execute(Runnable task) {
        super.submit(this.wrap(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(this.wrap(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(this.wrap(task));
    }

    public <T> Callable<T> wrap(final Callable<T> callable){
        Map<String,String> context= MDC.getCopyOfContextMap();
        return ()->{
            try {
                if (Objects.nonNull(context)) {
                    MDC.setContextMap(context);
                    LogContext.setLogGroup();
                }
                return callable.call();
            }finally {
                MDC.clear();
            }
        };
    }

    public Runnable wrap(final Runnable runnable){
        Map<String,String> context=MDC.getCopyOfContextMap();
        return ()->{
            try {
                if (Objects.nonNull(context)){
                    MDC.setContextMap(context);
                    LogContext.setLogGroup();
                }
                runnable.run();
            }finally {
                LogContext.clear();
                MDC.clear();
            }
        };
    }
}
