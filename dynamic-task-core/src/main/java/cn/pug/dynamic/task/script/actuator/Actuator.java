package cn.pug.dynamic.task.script.actuator;


import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

import java.util.concurrent.CompletableFuture;

public interface Actuator {
    //入口
    CompletableFuture<Result<?>> submit(Event<?> event);
}
