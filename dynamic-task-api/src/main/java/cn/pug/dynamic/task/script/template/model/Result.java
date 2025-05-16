package cn.pug.dynamic.task.script.template.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result<T> {
    protected String taskId;
    protected boolean success;
    protected TaskCodeMsg taskCodeMsg;
    protected T data;

    public static Result success(String taskId, Object data) {
        return Result.builder()
                .taskId(taskId)
                .success(true)
                .data(data)
                .build();
    }

    public static Result success(String taskId){
        return Result.builder()
                .taskId(taskId)
                .success(true)
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