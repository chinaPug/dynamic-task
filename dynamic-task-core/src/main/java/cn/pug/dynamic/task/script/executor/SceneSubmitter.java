package cn.pug.dynamic.task.script.executor;

import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

import java.util.concurrent.CompletableFuture;

/**
 * 提交任务到指定线程池并返回结果的能力
 */
public interface SceneSubmitter {
    CompletableFuture<Result<?>> execute(Event<?> event, SceneService<?,?> sceneService);
}
