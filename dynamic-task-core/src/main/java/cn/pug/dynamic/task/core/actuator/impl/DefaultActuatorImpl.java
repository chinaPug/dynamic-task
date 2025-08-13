package cn.pug.dynamic.task.core.actuator.impl;

import cn.pug.dynamic.task.core.constant.TaskCodeMsg;
import cn.pug.dynamic.task.core.exception.PredicateException;
import cn.pug.dynamic.task.core.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.core.actuator.Actuator;
import cn.pug.dynamic.task.core.executor.ExecutorManager;
import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
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

    private boolean isValidEvent(InputWrapper inputWrapper) {
        return inputWrapper != null
                && inputWrapper.getTaskId() != null && !inputWrapper.getTaskId().isEmpty()
                && inputWrapper.getIdentifyVal() != null && !inputWrapper.getIdentifyVal().isEmpty()
                && inputWrapper.getScriptVersion() != null && !inputWrapper.getScriptVersion().isEmpty();
    }

    /**
     * 提交入口
     *
     * @param inputWrapper
     * @return
     */
    @Override
    public CompletableFuture<OutputWrapper<?>> submit(InputWrapper<?> inputWrapper) {
        /*Event检验**/
        if (!isValidEvent(inputWrapper)) {
            log.error("任务【{}】——输入参数不合法", inputWrapper.getTaskId() == null ? "null" : inputWrapper.getTaskId());
            throw new PredicateException(TaskCodeMsg.EVENT_PARAM_ERROR);
        }
        /**/

        /*获取Scene实例**/
        SceneService<?, ?> sceneService;
        try {
            sceneService = scriptAcquirable.getSceneService(inputWrapper);
        } catch (Exception e) {
            log.error("任务【{}】——获取Scene执行异常", inputWrapper.getTaskId(), e);
            throw new PredicateException(TaskCodeMsg.GET_SCRIPT_ERROR);
        }
        /**/

        /*提交到线程池，并返回结果**/
        CompletableFuture<? extends OutputWrapper<?>> completableFuture = executorManager.execute(inputWrapper, sceneService);
        return completableFuture.handle((result, e) -> {
            if (e != null) {
                log.error("任务【{}】——action函数执行异常", inputWrapper.getTaskId(), e);
            }
            return result;
        });
        /**/
    }

}
