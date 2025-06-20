package cn.pug.dynamic.task.common.api;

import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @param <T> 定制化模板入参
 * @param <R> 定制化模板出参
 */
@Slf4j
public abstract class SceneService<T,R> implements Scene<T,R>{

    /**
     * 框架的处理逻辑
     * @param inputWrapper
     * @return
     */
    @Override
    public final OutputWrapper<R> action(InputWrapper<T> inputWrapper){
        return new OutputWrapper<R>(inputWrapper,todo(inputWrapper.getData()));
    }

    /**
     * 定制化模板处理逻辑
     * @param data
     * @return
     */
    abstract public R todo(T data);
}
