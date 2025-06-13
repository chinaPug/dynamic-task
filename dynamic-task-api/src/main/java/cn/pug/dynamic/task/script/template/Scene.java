package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.model.InputWrapper;
import cn.pug.dynamic.task.script.template.model.OutputWrapper;

@FunctionalInterface
public interface Scene<T,R> {

    OutputWrapper<R> action(InputWrapper<T> inputWrapper);

}
