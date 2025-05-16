package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

import java.util.concurrent.CompletableFuture;

public interface Scene<T ,R> {
    CompletableFuture<T> action(R event);
}