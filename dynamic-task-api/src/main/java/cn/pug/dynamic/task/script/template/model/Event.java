package cn.pug.dynamic.task.script.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event<T> {
    protected String taskId;
    protected String identifyVal;
    protected String scriptVersion;
    protected T data;
} 