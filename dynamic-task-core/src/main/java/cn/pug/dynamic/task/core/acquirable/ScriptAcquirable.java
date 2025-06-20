package cn.pug.dynamic.task.core.acquirable;


import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;

/**
 * 脚本获取能力
 */
public interface ScriptAcquirable {
    SceneService<?, ?> getSceneService(InputWrapper<?> inputWrapper);
}
