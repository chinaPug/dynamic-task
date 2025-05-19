package cn.pug.dynamic.task.script.template;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@FunctionalInterface
public interface Scene<T ,R> {
    CompletableFuture<T> action(R event);
}