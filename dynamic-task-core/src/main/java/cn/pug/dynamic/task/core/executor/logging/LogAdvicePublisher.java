package cn.pug.dynamic.task.core.executor.logging;

import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;


public class LogAdvicePublisher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher=applicationEventPublisher;
    }

    @Data
    @ToString
    @AllArgsConstructor
    public class LogAdviceEvent{
        private InputWrapper<?> inputWrapper;
        private OutputWrapper<?> outputWrapper;
        private String logPath;

        void publish(){
            applicationEventPublisher.publishEvent(this);
        }
    }

}
