package cn.pug.dynamic.task.script.executor;

import cn.pug.dynamic.task.script.executor.impl.ExecutorServiceWrapper;
import cn.pug.dynamic.task.script.template.model.InputWrapper;

/**
 * 根据event选择对应线程池的能力
 */
public interface ExecutorLoaderBalance {
    ExecutorServiceWrapper choose(InputWrapper<?> inputWrapper);
}
