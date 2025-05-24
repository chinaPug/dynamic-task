package cn.pug.dynamic.task.script.actuator.impl;

import cn.pug.dynamic.task.script.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.executor.ExecutorManager;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
@Slf4j
@Component
public final class DefaultActuatorImpl implements Actuator {
    private final ScriptAcquirable scriptAcquirable;
    private final ExecutorManager executorManager;

    public DefaultActuatorImpl(ScriptAcquirable scriptAcquirable, ExecutorManager executorManager) {
        this.scriptAcquirable = scriptAcquirable;
        this.executorManager = executorManager;
    }

    public boolean isValidEvent(Event event) {
        return !(event.getTaskId() == null || event.getTaskId().isEmpty()
                || event.getIdentifyVal() == null || event.getIdentifyVal().isEmpty()
                || event.getScriptVersion() == null || event.getScriptVersion().isEmpty());
    }

    /**
     * 提交入口
     * @param event
     * @return
     */
    @Override
    public CompletableFuture<Result<?>> submit(Event<?> event) {
        /*Event检验**/
        if (event == null) {
            log.error("任务【{}】——event为null", "null");
            return CompletableFuture.completedFuture(Result.error("null", TaskCodeMsg.EVENT_IS_NULL));
        }
        if (!isValidEvent(event)){
            log.error("任务【{}】——输入参数不合法", event.getTaskId()==null?"null":event.getTaskId());
            return CompletableFuture.completedFuture(Result.error(event.getTaskId()==null?"null":event.getTaskId(), TaskCodeMsg.EVENT_PARAM_ERROR));
        }
        /**/

        /*获取Scene实例**/
        SceneService<?,?> sceneService;
        try {
            sceneService = scriptAcquirable.getSceneService(event);;
        } catch (Exception e) {
            log.error("任务【{}】——获取Scene执行异常", event.getTaskId(), e);
            return CompletableFuture.completedFuture(Result.error(event.getTaskId(), TaskCodeMsg.GET_SCRIPT_ERROR));
        }
        /**/

        /*提交到线程池，并返回结果**/
        CompletableFuture<? extends Result<?>> completableFuture=executorManager.execute(event, sceneService);
        return completableFuture.handle((result, e) -> {
            if (e!=null) {
                log.error("任务【{}】——action函数执行异常", event.getTaskId(), e);
            }
            return result;
        });
        /**/
    }

}
