package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Scene {
    Result<?> action(Event<?> event);
}
