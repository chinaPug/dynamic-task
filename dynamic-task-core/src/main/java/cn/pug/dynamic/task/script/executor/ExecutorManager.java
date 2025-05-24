package cn.pug.dynamic.task.script.executor;

import java.util.concurrent.ExecutorService;

public interface ExecutorManager extends ExecutorLoaderBalance, SceneSubmitter {

    void registerExecutor(String executorServiceName, ExecutorService executorService);

    void unregisterExecutor(String executorServiceName, boolean shutdownNow);
}
