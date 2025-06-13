package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.InputWrapper;
import cn.pug.dynamic.task.script.template.model.OutputWrapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class ExecutorServiceWrapper {
    final ExecutorService executorService;
    private final String executorServiceName;

    public ExecutorServiceWrapper(String executorServiceName, ExecutorService executorService) {
        this.executorService = executorService;
        this.executorServiceName = executorServiceName;
    }

    CompletableFuture<OutputWrapper<?>> submit(InputWrapper inputWrapper, SceneService<?,?> sceneService) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("任务{}运行开始", inputWrapper.toString());
            OutputWrapper<?> outputWrapper =sceneService.action(inputWrapper);
            log.debug("任务{}运行结束",outputWrapper.toString());
            return outputWrapper;
        }, executorService);
    }


    List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    void shutdown() {
        executorService.shutdown();
    }

}
