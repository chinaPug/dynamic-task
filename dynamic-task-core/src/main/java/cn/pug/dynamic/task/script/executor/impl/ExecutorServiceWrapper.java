package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

@Slf4j
public class ExecutorServiceWrapper {
    final ThreadPoolExecutor threadPoolExecutor;
    private final String threadPoolExecutorName;

    public ExecutorServiceWrapper(String threadPoolExecutorName, ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.threadPoolExecutorName = threadPoolExecutorName;
    }

    CompletableFuture<Result<?>> submit(Event<?> event, SceneService<?, ?> sceneService) {
        Supplier<Result<?>> supplier=() -> sceneService.action(event);
        return CompletableFuture.supplyAsync(supplier, threadPoolExecutor);
    }


    List<Runnable> shutdownNow() {
        runningInfo();
        return threadPoolExecutor.shutdownNow();
    }

    void shutdown() {
        runningInfo();
        threadPoolExecutor.shutdown();
    }

    private void runningInfo() {
        log.info("线程池【{}】", threadPoolExecutorName);
    }
}
