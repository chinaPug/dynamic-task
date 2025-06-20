package cn.pug.dynamic.task.core.acquirable;


import cn.pug.dynamic.task.common.api.model.InputWrapper;


/**
 * 脚本版本管理能力
 */
public interface ScriptManager extends ScriptAcquirable {
    void registerSceneService(InputWrapper<?> inputWrapper);

    void unloadSceneService(InputWrapper<?> inputWrapper);
}
