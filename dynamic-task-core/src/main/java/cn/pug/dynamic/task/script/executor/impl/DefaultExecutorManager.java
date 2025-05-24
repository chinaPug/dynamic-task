package cn.pug.dynamic.task.script.executor.impl;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import cn.pug.dynamic.task.script.executor.ExecutorManager;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Arrays;
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
    private final static String defaultExecutorName = "default-dynamic-task-executor";

    public DefaultExecutorManager(int corePoolSize, int maximumPoolSize, long keepAliveTime, int queueCapacity, DynamicTaskProperties dynamicTaskProperties) {
        ExecutorService defaultExecutorService = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new DefaultThreadFactory(defaultExecutorName.concat("-")),
                new ThreadPoolExecutor.AbortPolicy()
        );
        registerExecutor(defaultExecutorName, defaultExecutorService);
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
     * @param event
     * @return
     */
    @Override
    public ExecutorServiceWrapper choose(Event<?> event) {
        String chooseExecutorServiceName;
        // 移除默认线程池后随机选择线程池
        if (executorServiceMap.size() == 1) {
            chooseExecutorServiceName = defaultExecutorName;
        } else {
            chooseExecutorServiceName = executorServiceMap.keySet().stream().filter(name -> !name.equals(defaultExecutorName)).collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.get(new Random().nextInt(list.size()))
            ));
        }
        // 这里是防止并发时线程池被注销，从而获取失败的情况。获取失败时，默认线程池被选择
        return executorServiceMap.getOrDefault(chooseExecutorServiceName, executorServiceMap.get(defaultExecutorName));
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

    static class DefaultRejectExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.info("默认拒绝策略");
            //触发拒绝策略的时候，把任务交给默认的线程池来做
            ExecutorServiceWrapper executorServiceWrapper= executorServiceMap.values().stream().filter(wrapper -> wrapper.executorService.equals(executor)).findFirst().orElse(null);
            if (executorServiceWrapper==null){
                log.warn("未找到对应的线程池，可能被卸载");
            }
            else {
                Map.Entry<Event<?>, SceneService<?, ?>> entry = executorServiceWrapper.getEntryAndRemove(r);
                Event<?> event=entry.getKey();
                SceneService<?, ?> sceneService=entry.getValue();
                ExecutorServiceWrapper defaultExecutorServiceWrapper = executorServiceMap.get(defaultExecutorName);
                log.info("正在将任务{}提交到默认线程池",event.toString());
                defaultExecutorServiceWrapper.submit(event, sceneService);
            }
        }
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
            case "DEFAULT":
                return new DefaultRejectExecutionHandler();
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
                                    executorConfig.getQueueCapacity()==0?new SynchronousQueue<>():new LinkedBlockingQueue<>(executorConfig.getQueueCapacity()),
                                    new DefaultThreadFactory(executorConfig.getName()),
                                    getRejectedExecutionHandler(executorConfig.getTaskRejectedPolicy())
                            )
                    );
                }
        );
    }
}
