package cn.pug.dynamic.task.core.executor.impl;

import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import cn.pug.dynamic.task.core.executor.logging.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ExecutorServiceWrapper {
    final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final String threadPoolTaskExecutorName;

    public ExecutorServiceWrapper(String threadPoolTaskExecutorName, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.threadPoolTaskExecutorName = threadPoolTaskExecutorName;
    }

    CompletableFuture<OutputWrapper<?>> submit(InputWrapper inputWrapper, SceneService<?,?> sceneService) {
        LogContext.setInputWrapper(inputWrapper);
        return CompletableFuture.supplyAsync(() -> {
            log.debug("任务{}运行开始", inputWrapper.getTaskId());
            OutputWrapper<?> outputWrapper =sceneService.action(inputWrapper);
            log.debug("任务{}运行结束", outputWrapper.getTaskId());
            LogContext.setOutputWrapper(outputWrapper);
            return outputWrapper;
        }, threadPoolTaskExecutor);
    }

    public String getThreadPoolTaskExecutorName() {
        return threadPoolTaskExecutorName;
    }

    List<Runnable> shutdownNow() {
        return threadPoolTaskExecutor.getThreadPoolExecutor().shutdownNow();
    }

    void shutdown() {
        threadPoolTaskExecutor.shutdown();
    }

}
