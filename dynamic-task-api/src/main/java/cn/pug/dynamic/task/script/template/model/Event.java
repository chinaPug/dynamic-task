package cn.pug.dynamic.task.script.template.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Event {
    protected String taskId;
    protected String identifyVal;
    protected String scriptVersion;
} 