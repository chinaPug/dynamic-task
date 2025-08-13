package cn.pug.dynamic.task.example.listener;

import cn.pug.dynamic.task.core.executor.logging.LogAdvicePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEventListener {

    @EventListener(LogAdvicePublisher.LogAdviceEvent.class)
    public void onLogAdviceEvent(LogAdvicePublisher.LogAdviceEvent logAdviceEvent){
        log.debug("监听器消息：{}",logAdviceEvent.toString());
    }
}
