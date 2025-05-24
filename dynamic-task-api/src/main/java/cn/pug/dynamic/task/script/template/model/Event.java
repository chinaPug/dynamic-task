package cn.pug.dynamic.task.script.template.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Event<T> {
    protected String taskId;
    protected String identifyVal;
    protected String scriptVersion;
    protected T data;
}