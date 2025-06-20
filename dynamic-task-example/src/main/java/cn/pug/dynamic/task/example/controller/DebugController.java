package cn.pug.dynamic.task.example.controller;

import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.core.actuator.Actuator;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import cn.pug.script.api.pojo.dto.Event;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class DebugController implements ApplicationContextAware {

    @Autowired
    private Actuator actuator;

    private ApplicationContext applicationContext;


    @PostMapping("event")
    public OutputWrapper<?> submit(@RequestBody InputWrapper<?> inputWrapper) {
        return actuator.submit(inputWrapper).join();
    }

    @GetMapping("test")
    public CompletableFuture<OutputWrapper<?>> test() {
        DebugController debugController=applicationContext.getBean(DebugController.class);
        return debugController.service();
    }
    @Async
    public CompletableFuture<OutputWrapper<?>> service(){
        System.out.println("test"+Thread.currentThread().getName());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }
}
