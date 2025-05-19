package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.exception.PredicateException;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SceneService<T,R> extends Scene<Result<?>, Event<?>>{
    default CompletableFuture<Result<?>> action(Event<?> event) {
        Result<?> result;
        try {
            result = Result.success(event.getTaskId(), flow((R) event.getData()));
        } catch (Exception e) {
            if (e instanceof PredicateException) {
                result = Result.error(event.getTaskId(), ((PredicateException) e).getTaskCodeMsg());
            } else {
                result = Result.error(event.getTaskId(), TaskCodeMsg.UNKNOWN_ERROR);
            }
        }
        return CompletableFuture.completedFuture(result);
    }

    T flow(R data);
}
