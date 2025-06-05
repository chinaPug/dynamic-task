package cn.pug.dynamic.task.example.controller;

import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.template.model.Event;
import cn.pug.dynamic.task.script.template.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController(value = "debug")
public class DebugController {

    @Autowired
    private Actuator actuator;


    @PostMapping("event")
    public Result<?> submit(@RequestBody Event<?> event) {
        return actuator.submit(event).join();
    }
    public static void main(String[] args) {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
    }

}
