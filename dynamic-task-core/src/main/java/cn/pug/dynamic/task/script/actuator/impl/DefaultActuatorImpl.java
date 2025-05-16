package cn.pug.dynamic.task.script.actuator.impl;

import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.script.template.Scene;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public final class DefaultActuatorImpl implements Actuator,ScriptAcquirable {
    private final ScriptAcquirable scriptAcquirable;

    public DefaultActuatorImpl(ScriptAcquirable scriptAcquirable) {
        this.scriptAcquirable = scriptAcquirable;
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
    public CompletableFuture<Result> submit(Event event) {
        // 判断输入是不是空
        if (event == null) {
            log.error("任务【{}】——event为null", "null");
            return CompletableFuture.completedFuture(Result.error("null", TaskCodeMsg.EVENT_IS_NULL));
        }
        if (!isValidEvent(event)){
            log.error("任务【{}】——输入参数不合法", event.getTaskId()==null?"null":event.getTaskId());
            return CompletableFuture.completedFuture(Result.error(event.getTaskId()==null?"null":event.getTaskId(), TaskCodeMsg.EVENT_PARAM_ERROR));
        }
        Scene scene;
        try {
            scene = scriptAcquirable.getScene(event);
        } catch (Exception e) {
            log.error("任务【{}】——获取Scene执行异常", event.getTaskId(), e);
            return CompletableFuture.completedFuture(Result.error(event.getTaskId(), TaskCodeMsg.GET_SCRIPT_ERROR));
        }
        if (scene == null) {
            return CompletableFuture.completedFuture(Result.error(event.getTaskId(), TaskCodeMsg.SCRIPT_NOT_FOUND));
        }
        CompletableFuture future = scene.action(event);
        return future.handle((result, e) -> {
            if (Objects.nonNull(e)) {
                log.error("任务【{}】——action函数执行异常", event.getTaskId(), e);
            }
            return result;
        });
    }

    /**
     * 根据identifyVal找到对应的bean实例
     * @return Scene
     */
    @Override
    public Scene getScene(Event event) {
        return scriptAcquirable.getScene(event);
    }
}
