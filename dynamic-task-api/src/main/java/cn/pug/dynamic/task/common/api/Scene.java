package cn.pug.dynamic.task.common.api;

import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;

@FunctionalInterface
public interface Scene<T,R> {

    OutputWrapper<R> action(InputWrapper<T> inputWrapper);

}
