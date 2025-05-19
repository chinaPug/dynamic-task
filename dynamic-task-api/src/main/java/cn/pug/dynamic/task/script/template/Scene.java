package cn.pug.dynamic.task.script.template;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Scene<T ,R> {
    CompletableFuture<T> action(R event);
}
