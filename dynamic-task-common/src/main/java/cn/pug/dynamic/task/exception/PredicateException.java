package cn.pug.dynamic.task.exception;

import cn.pug.dynamic.task.constant.TaskCodeMsg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class PredicateException extends RuntimeException {
    private TaskCodeMsg taskCodeMsg;
}
