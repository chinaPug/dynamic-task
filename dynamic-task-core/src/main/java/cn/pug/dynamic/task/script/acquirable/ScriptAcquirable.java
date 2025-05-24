package cn.pug.dynamic.task.script.acquirable;


import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

/**
 * 脚本获取能力
 */
public interface ScriptAcquirable {
    SceneService<?,?> getSceneService(Event<?> event);
}
