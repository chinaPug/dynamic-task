package cn.pug.dynamic.task.example.controller;

import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
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

@RestController(value = "debug")
public class DebugController implements ApplicationContextAware {

    @Autowired
    private Actuator actuator;

    private ApplicationContext applicationContext;


    @PostMapping("event")
    public Result<?> submit(@RequestBody Event<?> event) {
        return actuator.submit(event).join();
    }

    @GetMapping("test")
    public CompletableFuture<Result<?>> test() {
        DebugController debugController=applicationContext.getBean(DebugController.class);
        return debugController.service();
    }
    @Async
    public CompletableFuture<Result<?>> service(){
        System.out.println("test"+Thread.currentThread().getName());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }
}
