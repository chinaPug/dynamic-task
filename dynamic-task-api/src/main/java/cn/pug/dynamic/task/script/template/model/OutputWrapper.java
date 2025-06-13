package cn.pug.dynamic.task.script.template.model;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class OutputWrapper<T> {
    protected String taskId;
    protected T data;

    public OutputWrapper(InputWrapper<?> inputWrapper,T data){
        this.taskId=inputWrapper.getTaskId();
        this.data=data;
    }
} 