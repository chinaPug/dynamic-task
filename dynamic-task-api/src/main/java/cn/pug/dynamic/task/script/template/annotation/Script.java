package cn.pug.dynamic.task.script.template.annotation;

import java.lang.annotation.*;

/**
 * 程序的主入口
 * 你可以在一个包里定义多个入口，这样会让你打包起来更方便。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Script {
    String description() default "";
    String auth() default "";
}