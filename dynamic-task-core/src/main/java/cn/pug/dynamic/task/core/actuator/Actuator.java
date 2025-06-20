package cn.pug.dynamic.task.core.actuator;


import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;

import java.util.concurrent.CompletableFuture;

public interface Actuator {
    //入口
    CompletableFuture<OutputWrapper<?>> submit(InputWrapper<?> inputWrapper);
}
