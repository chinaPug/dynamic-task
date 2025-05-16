package cn.pug.dynamic.task.script.acquirable;


import cn.pug.dynamic.task.script.template.Scene;
import cn.pug.dynamic.task.script.template.model.Event;

/**
 * 脚本获取能力
 */
public interface ScriptAcquirable {
    Scene getScene(Event event);
}
