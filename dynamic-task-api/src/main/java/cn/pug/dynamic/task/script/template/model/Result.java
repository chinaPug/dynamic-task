package cn.pug.dynamic.task.script.template.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result {
    protected String taskId;
    protected boolean success;
    protected TaskCodeMsg taskCodeMsg;

    public static Result success(String taskId) {
        return Result.builder()
                .taskId(taskId)
                .success(true)
                .taskCodeMsg(TaskCodeMsg.SUCCESS)
                .build();
    }

    public static Result error(String taskId, TaskCodeMsg taskCodeMsg) {
        return Result.builder()
                .taskId(taskId)
                .success(false)
                .taskCodeMsg(taskCodeMsg)
                .build();
    }
} 