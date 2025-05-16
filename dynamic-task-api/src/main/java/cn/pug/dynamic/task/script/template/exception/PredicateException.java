package cn.pug.dynamic.task.script.template.exception;


import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class PredicateException extends RuntimeException {
    private TaskCodeMsg taskCodeMsg;
}
