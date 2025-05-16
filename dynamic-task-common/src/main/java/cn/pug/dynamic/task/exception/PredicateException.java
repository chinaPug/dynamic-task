package cn.pug.dynamic.task.exception;




import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;

public class PredicateException extends RuntimeException {
    private TaskCodeMsg taskCodeMsg;

    public PredicateException(TaskCodeMsg taskCodeMsg) {
        super(taskCodeMsg.getMessage());
        this.taskCodeMsg = taskCodeMsg;
    }

}
