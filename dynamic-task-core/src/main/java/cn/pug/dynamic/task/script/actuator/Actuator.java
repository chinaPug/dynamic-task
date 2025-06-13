package cn.pug.dynamic.task.script.actuator;


import cn.pug.dynamic.task.script.template.model.InputWrapper;
import cn.pug.dynamic.task.script.template.model.OutputWrapper;

import java.util.concurrent.CompletableFuture;

public interface Actuator {
    //入口
    CompletableFuture<OutputWrapper<?>> submit(InputWrapper<?> inputWrapper);
}
