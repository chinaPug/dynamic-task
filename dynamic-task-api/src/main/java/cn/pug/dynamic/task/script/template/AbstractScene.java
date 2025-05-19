package cn.pug.dynamic.task.script.template;

import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;

public abstract class AbstractScene<T,R> implements Scene<Result<?>, Event<?>>{
    public abstract T flow(R data);
}
