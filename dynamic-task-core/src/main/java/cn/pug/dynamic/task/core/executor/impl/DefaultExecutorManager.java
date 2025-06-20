package cn.pug.dynamic.task.core.executor.impl;

import cn.pug.dynamic.task.core.config.DynamicTaskProperties;
import cn.pug.dynamic.task.core.executor.ExecutorManager;
import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class DefaultExecutorManager implements ExecutorManager {
    private DynamicTaskProperties dynamicTaskProperties;
    // 线程池名->线程池包装类
    private final static Map<String, ExecutorServiceWrapper> executorServiceMap = new ConcurrentHashMap<>();

    public DefaultExecutorManager(DynamicTaskProperties dynamicTaskProperties) {
        this.dynamicTaskProperties = dynamicTaskProperties;
    }

    @Override
    public void registerExecutor(String executorServiceName, ExecutorService executorService) {
        log.info("正在注册线程池：{}", executorServiceName);
        executorServiceMap.put(
                executorServiceName,
                new ExecutorServiceWrapper(
                        executorServiceName,
                        executorService
                )
        );
    }

    @Override
    public void unregisterExecutor(String executorServiceName, boolean shutdownNow) {
        log.info("正在注销线程池：{}", executorServiceName);
        ExecutorServiceWrapper executorServiceWrapper = executorServiceMap.remove(executorServiceName);
        if (shutdownNow) {
            log.info("正在立即关闭线程池：{}", executorServiceName);
            executorServiceWrapper.shutdownNow();
        } else {
            log.info("全部任务执行完后，关闭线程池：{}", executorServiceName);
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
    public ExecutorServiceWrapper choose(InputWrapper<?> inputWrapper) {
        String chooseExecutorServiceName;
        chooseExecutorServiceName = executorServiceMap.keySet().stream().collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.get(new Random().nextInt(list.size()))
        ));
        // 这里是防止并发时线程池被注销，从而获取失败的情况。获取失败时，默认线程池被选择
        return executorServiceMap.get(chooseExecutorServiceName);
    }

    /**
     * 将生产资料和制作方式提交到线程池运行，并异步返回结果
     *
     * @param inputWrapper
     * @param sceneService
     * @return
     */
    @Override
    public CompletableFuture<OutputWrapper<?>> execute(InputWrapper<?> inputWrapper, SceneService<?, ?> sceneService) {
        ExecutorServiceWrapper executorServiceWrapper = choose(inputWrapper);
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
                log.info("线程【{}】异常捕获：{}", t.getName(), e.getMessage());
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
        log.info("初始化线程池");
        List<DynamicTaskProperties.ExecutorConfig> executorConfigList = dynamicTaskProperties.getExecutor();
        executorConfigList.forEach(
                executorConfig -> {
                    registerExecutor(
                            executorConfig.getName(),
                            new ThreadPoolExecutor(
                                    executorConfig.getCorePoolSize(),
                                    executorConfig.getMaxPoolSize(),
                                    executorConfig.getKeepAliveSeconds(),
                                    TimeUnit.SECONDS,
                                    executorConfig.getQueueCapacity()==0?new SynchronousQueue<>():new LinkedBlockingQueue<>(executorConfig.getQueueCapacity()),
                                    new DefaultThreadFactory(executorConfig.getName()),
                                    getRejectedExecutionHandler(executorConfig.getTaskRejectedPolicy())
                            )
                    );
                }
        );
    }
}
