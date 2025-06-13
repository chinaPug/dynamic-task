package cn.pug.dynamic.task.script.template.model;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class InputWrapper<T> {
    protected String taskId;
    protected String identifyVal;
    protected String scriptVersion;
    protected T data;
}