package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
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
    private final Map<Supplier<Result<?>>,Map.Entry<Event<?>, SceneService<?,?>>> runningMap = new HashMap<>();

    public ExecutorServiceWrapper(String executorServiceName, ExecutorService executorService) {
        this.executorService = executorService;
        this.executorServiceName = executorServiceName;
    }

    CompletableFuture<Result<?>> submit(Event<?> event, SceneService<?, ?> sceneService) {
        Supplier<Result<?>> supplier=() -> {
            log.info("任务{}运行开始",event.toString());
            Result<?> result=sceneService.action(event);
            Map.Entry<Event<?>, SceneService<?,?>> entry=runningMap.remove(this);
            log.info("任务{}运行结束",entry.getKey().toString());
            return result;
        };
        Map.Entry<Event<?>, SceneService<?,?>> entry = new AbstractMap.SimpleEntry<>(event, sceneService);
        runningMap.put(supplier, entry);
        return CompletableFuture.supplyAsync(supplier, executorService);
    }

    Map.Entry<Event<?>, SceneService<?,?>> getEntry(Runnable r) {
        log.info("反射获取Evnet和SceneService");
        Supplier<Result<?>> supplier;
        Class<?>[] classes=CompletableFuture.class.getDeclaredClasses();
        Class<?> rClass=Arrays.stream(classes).filter(clazz->clazz.getName().equals("java.util.concurrent.CompletableFuture$AsyncSupply")).findFirst().get();
        Field field;
        try {
            field=rClass.getDeclaredField("fn");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        try {
            field.setAccessible(true);
            supplier=(Supplier<Result<?>>)field.get(r);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return runningMap.get(supplier);
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
        Set<Event<?>> runningSet=runningMap.values().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        log.info("线程池【{}】\n正在运行任务数量{}", executorServiceName, runningSet.size());
        runningSet.forEach(event ->
                log.info("运行任务信息{}\n", event.toString())
        );
    }
}
