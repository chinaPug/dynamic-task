package cn.pug.dynamic.task.script.template.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Script {
    String identifyVal() ;
    String description() default "";
    String version() default "1.0.0";
} 