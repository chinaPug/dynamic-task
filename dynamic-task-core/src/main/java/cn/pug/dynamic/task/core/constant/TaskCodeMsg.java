package cn.pug.dynamic.task.core.constant;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class TaskCodeMsg implements Serializable {
    protected Integer code;
    protected String message;

    public static TaskCodeMsg SUCCESS = new TaskCodeMsg(0, "成功");
    public static TaskCodeMsg UNKNOWN_ERROR = new TaskCodeMsg(-1, "未知错误");
    public static TaskCodeMsg SCRIPT_NOT_FOUND = new TaskCodeMsg(-2, "任务所属脚本未找到");
    public static TaskCodeMsg EVENT_IS_NULL = new TaskCodeMsg(-3, "Event事件入参是空");
    public static TaskCodeMsg GET_SCRIPT_ERROR = new TaskCodeMsg(-4, "获取脚本失败");
    public static TaskCodeMsg EVENT_PARAM_ERROR = new TaskCodeMsg(-5, "Event参数不合法");
    public static TaskCodeMsg CLASS_LOAD_ERROR = new TaskCodeMsg(-6, "类加载失败");

    public static TaskCodeMsg SCRIPT_VERSION_ERROR=new TaskCodeMsg(-7,"所需版本过低，请检查任务是否过期");
    public static TaskCodeMsg SCRIPT_DEFINITION_ERROR=new TaskCodeMsg(-7,"脚本定义出错，请检查定义是否规范");


}