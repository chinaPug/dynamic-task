package cn.pug.dynamic.task.script.acquirable;


import cn.pug.dynamic.task.script.template.model.Event;


/**
 * 脚本版本管理能力
 */
public interface ScriptManager extends ScriptAcquirable {
    void registerSceneService(Event<?> event);
    void unloadSceneService(Event<?> event);
}
