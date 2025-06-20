package cn.pug.dynamic.task.core.exception;

import cn.pug.dynamic.task.core.constant.TaskCodeMsg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class PredicateException extends RuntimeException {
    private TaskCodeMsg taskCodeMsg;
}
