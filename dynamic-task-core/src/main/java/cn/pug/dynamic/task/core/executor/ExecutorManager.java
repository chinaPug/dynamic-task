package cn.pug.dynamic.task.core.executor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;

public interface ExecutorManager extends ExecutorLoaderBalance, SceneSubmitter {

    void registerExecutor(String threadPoolTaskExecutorName, ThreadPoolTaskExecutor threadPoolTaskExecutor);

    void unregisterExecutor(String threadPoolTaskExecutorName, boolean shutdownNow);
}
