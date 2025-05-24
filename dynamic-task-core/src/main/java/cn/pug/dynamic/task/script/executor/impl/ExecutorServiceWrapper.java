package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ExecutorServiceWrapper {
    private final ExecutorService executorService;
    private final String executorServiceName;
    private final Set<Event<?>> runningSet = new HashSet<>();

    public ExecutorServiceWrapper(String executorServiceName, ExecutorService executorService) {
        this.executorService = executorService;
        this.executorServiceName = executorServiceName;
    }

    CompletableFuture<Result<?>> submit(Event<?> event, SceneService<?, ?> sceneService) {
        runningSet.add(event);
        CompletableFuture<Result<?>> resultCompletableFuture = CompletableFuture.supplyAsync(() -> sceneService.action(event).join(), executorService);
        runningSet.remove(event);
        return resultCompletableFuture;
    }

    List<Runnable> shutdownNow() {
        runningInfo();
        return executorService.shutdownNow();
    }

    void shutdown() {
        runningInfo();
        executorService.shutdown();
    }

    private void runningInfo() {
        log.info("线程池【{}】\n正在运行任务数量{}", executorServiceName, runningSet.size());
        runningSet.forEach(event ->
                log.info("运行任务信息{}\n", event.toString())
        );
    }
}
