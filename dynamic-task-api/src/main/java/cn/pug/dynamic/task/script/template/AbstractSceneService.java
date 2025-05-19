package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.exception.PredicateException;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractSceneService<T,R> implements SceneService<T,R>{
    @Override
    public final CompletableFuture<Result<?>> action(Event<?> event){
        log.info("任务【{}】——开始执行", event.getTaskId());
        SceneService<T,R> sceneService=(input)->{
            T output=flow(input);
            log.info("任务【{}】——执行完成", event.getTaskId());
            return output;
        };
        return sceneService.action(event);
    }
}
