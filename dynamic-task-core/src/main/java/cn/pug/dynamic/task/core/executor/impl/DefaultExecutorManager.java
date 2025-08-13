package cn.pug.dynamic.task.core.executor.impl;

import cn.pug.dynamic.task.core.DynamicTaskProperties;
import cn.pug.dynamic.task.core.executor.ExecutorLoaderBalance;
import cn.pug.dynamic.task.core.executor.ExecutorManager;
import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class DefaultExecutorManager implements ExecutorManager {
    private final DynamicTaskProperties dynamicTaskProperties;
    // 线程池名->线程池包装类
    private final Map<String, ExecutorServiceWrapper> executorMap = new ConcurrentHashMap<>();
    private final ExecutorLoaderBalance executorLoaderBalance;
    public DefaultExecutorManager(DynamicTaskProperties dynamicTaskProperties) {
        this.dynamicTaskProperties = dynamicTaskProperties;
        this.executorLoaderBalance = new RandomExecutorLoaderBalance();
    }
    public DefaultExecutorManager(DynamicTaskProperties dynamicTaskProperties, ExecutorLoaderBalance executorLoaderBalance) {
        this.dynamicTaskProperties = dynamicTaskProperties;
        this.executorLoaderBalance = executorLoaderBalance;
    }
    @Override
    public void registerExecutor(String threadPoolTaskExecutorName, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        log.debug("正在注册线程池：{}", threadPoolTaskExecutorName);
        executorMap.put(
                threadPoolTaskExecutorName,
                new ExecutorServiceWrapper(
                        threadPoolTaskExecutorName,
                        threadPoolTaskExecutor
                )
        );
    }

    @Override
    public void unregisterExecutor(String threadPoolTaskExecutorName, boolean shutdownNow) {
        log.debug("正在注销线程池：{}", threadPoolTaskExecutorName);
        ExecutorServiceWrapper executorServiceWrapper = executorMap.remove(threadPoolTaskExecutorName);
        if (shutdownNow) {
            log.debug("正在立即关闭线程池：{}", threadPoolTaskExecutorName);
            executorServiceWrapper.shutdownNow();
        } else {
            log.debug("全部任务执行完后，关闭线程池：{}", threadPoolTaskExecutorName);
            executorServiceWrapper.shutdown();
        }
    }


    /**
     * 默认是随机选择线程池
     *
     * @param inputWrapper
     * @return
     */
    @Override
    public ExecutorServiceWrapper choose(InputWrapper<?> inputWrapper,Map<String, ExecutorServiceWrapper> executorMap) {
        return executorLoaderBalance.choose(inputWrapper,executorMap);
    }


    @Override
    public CompletableFuture<OutputWrapper<?>> execute(InputWrapper<?> inputWrapper, SceneService<?, ?> sceneService) {
        ExecutorServiceWrapper executorServiceWrapper = choose(inputWrapper,executorMap);
        if (executorServiceWrapper == null){
            log.error("未找到可用的线程池");
            return CompletableFuture.completedFuture(new OutputWrapper<>(inputWrapper,null));
        }
        log.debug("任务【{}】——使用线程池【{}】", inputWrapper.getTaskId(), executorServiceWrapper.getThreadPoolTaskExecutorName());
        return executorServiceWrapper.submit(inputWrapper, sceneService);
    }



    static class DefaultThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefixName;
        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        public DefaultThreadFactory(String prefixName) {
            this.prefixName = prefixName;
            this.uncaughtExceptionHandler = new DefaultUncaughtExceptionHandler();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            String threadName = prefixName.concat("-") + counter.getAndIncrement();
            thread.setName(threadName);
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        }


        private static class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.debug("线程【{}】异常捕获：{}", t.getName(), e.getMessage());
            }
        }
    }

    private static RejectedExecutionHandler getRejectedExecutionHandler(String policy) {
        switch (policy) {
            case "CALLER_RUNS":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "ABORT":
                return new ThreadPoolExecutor.AbortPolicy();
            case "DISCARD":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                log.warn("未知拒绝策略或者未设置拒绝策略: {}, 使用默认拒绝策略： ABORT policy", policy);
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }

    @PostConstruct
    public void init() {
        log.debug("初始化线程池");
        List<DynamicTaskProperties.ExecutorConfig> executorConfigList = dynamicTaskProperties.getExecutor();
        executorConfigList.forEach(
                executorConfig -> {
                    registerExecutor(
                            executorConfig.getName(),
                            new ThreadPoolTaskExecutorMdcWrapper(
                                    executorConfig.getCorePoolSize(),
                                    executorConfig.getMaxPoolSize(),
                                    executorConfig.getKeepAliveSeconds(),
                                    executorConfig.getQueueCapacity(),
                                    new DefaultThreadFactory(executorConfig.getName()),
                                    getRejectedExecutionHandler(executorConfig.getTaskRejectedPolicy())
                            )
                    );
                }
        );
    }


    private static class RandomExecutorLoaderBalance implements ExecutorLoaderBalance {
        @Override
        public ExecutorServiceWrapper choose(InputWrapper<?> inputWrapper, Map<String, ExecutorServiceWrapper> executorMap) {
            String chooseThreadPoolTaskExecutorName;
            chooseThreadPoolTaskExecutorName = executorMap.keySet().stream().collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.get(new Random().nextInt(list.size()))
            ));
            // 这里是防止并发时线程池被注销，从而获取失败的情况。获取失败时，默认线程池被选择
            return executorMap.get(chooseThreadPoolTaskExecutorName);
        }
    }
}