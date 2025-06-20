package cn.pug.dynamic.task.core.executor.logging;

import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;


public class LogAdvicePublisher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher=applicationEventPublisher;
    }

    @Data
    public class LogAdviceEvent{
        private String logPath;

        public LogAdviceEvent(String logPath){
            this.logPath=logPath;
        }
        void publish(){
            applicationEventPublisher.publishEvent(this);
        }
    }

}
