package cn.pug.dynamic.task.script.executor;

import cn.pug.dynamic.task.script.executor.impl.ExecutorServiceWrapper;
import cn.pug.dynamic.task.script.template.model.Event;

/**
 * 根据event选择对应线程池的能力
 */
public interface ExecutorLoaderBalance {
    ExecutorServiceWrapper choose(Event<?> event) ;
}
