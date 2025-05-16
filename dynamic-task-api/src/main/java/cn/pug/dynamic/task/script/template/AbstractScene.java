package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.exception.PredicateException;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractScene<T,R>  implements Scene<Result<?>,Event<?>>{

    @Override
    public CompletableFuture<Result<?>> action(Event<?> event){
        Result<?> result;
        log.info("任务【{}】——开始执行", event.getTaskId());
        try {
            result=Result.success(event.getTaskId(),flow((R) event.getData()));
        }catch (Exception e){
            log.error("任务【{}】——执行异常", event.getTaskId(), e);
            if (e instanceof PredicateException){
                result=Result.error(event.getTaskId(),((PredicateException) e).getTaskCodeMsg());
            }else {
                result = Result.error(event.getTaskId(), TaskCodeMsg.UNKNOWN_ERROR);
            }
        }
        log.info("任务【{}】——执行完成", event.getTaskId());
        return CompletableFuture.completedFuture(result);
    }

    public abstract T flow(R data);
}
