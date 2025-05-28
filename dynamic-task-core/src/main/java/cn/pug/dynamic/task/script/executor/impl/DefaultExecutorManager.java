package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import cn.pug.dynamic.task.script.executor.ExecutorManager;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
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
    public void registerExecutor(String executorServiceName, ThreadPoolExecutor threadPoolExecutor) {
        log.info("正在注册线程池：{}", executorServiceName);
        ExecutorServiceWrapper oldVal=executorServiceMap.putIfAbsent(
                executorServiceName,
                new ExecutorServiceWrapper(
                        executorServiceName,
                        threadPoolExecutor
                )
        );
        if (oldVal!=null){
            log.warn("线程池【{}】已存在，使用第一次定义", executorServiceName);
        }
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
     * @param event
     * @return
     */
    @Override
    public ExecutorServiceWrapper choose(Event<?> event) {
        String chooseExecutorServiceName;
        // 移除默认线程池后随机选择线程池
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
     * @param event
     * @param sceneService
     * @return
     */
    @Override
    public CompletableFuture<Result<?>> execute(Event<?> event, SceneService<?, ?> sceneService) {
        ExecutorServiceWrapper executorServiceWrapper = choose(event);
        return executorServiceWrapper.submit(event, sceneService);
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
                log.warn("Unknown rejection policy: {}, using default ABORT policy", policy);
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
                                    executorConfig.getQueueCapacity() == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(executorConfig.getQueueCapacity()),
                                    new DefaultThreadFactory(executorConfig.getName()),
                                    getRejectedExecutionHandler(executorConfig.getTaskRejectedPolicy())
                            )
                    );
                }
        );
    }
}
