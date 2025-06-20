package cn.pug.dynamic.task.core.executor;

import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;

import java.util.concurrent.CompletableFuture;

/**
 * 提交任务到指定线程池并返回结果的能力
 */
public interface SceneSubmitter {
    CompletableFuture<OutputWrapper<?>> execute(InputWrapper<?> inputWrapper, SceneService<?, ?> sceneService);
}
