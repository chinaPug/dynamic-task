package cn.pug.dynamic.task.script.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public interface ExecutorManager extends ExecutorLoaderBalance, SceneSubmitter {

    void registerExecutor(String executorServiceName, ThreadPoolExecutor threadPoolExecutor);

    void unregisterExecutor(String executorServiceName, boolean shutdownNow);
}
