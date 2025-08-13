package cn.pug.dynamic.task.core.executor;

import cn.pug.dynamic.task.core.executor.impl.ExecutorServiceWrapper;
import cn.pug.dynamic.task.common.api.model.InputWrapper;

import java.util.Map;

/**
 * 根据event选择对应线程池的能力
 */
public interface ExecutorLoaderBalance {
    ExecutorServiceWrapper choose(InputWrapper<?> inputWrapper, Map<String, ExecutorServiceWrapper> executorMap);
}
