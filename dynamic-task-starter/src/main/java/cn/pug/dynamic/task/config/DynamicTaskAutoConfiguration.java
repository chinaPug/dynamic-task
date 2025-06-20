package cn.pug.dynamic.task.config;

import cn.pug.dynamic.task.core.DynamicTaskBannerPrinter;
import cn.pug.dynamic.task.core.DynamicTaskProperties;
import cn.pug.dynamic.task.core.constant.DynamicTaskConst;
import cn.pug.dynamic.task.core.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.core.acquirable.dynamic.DynamicScriptManager;
import cn.pug.dynamic.task.core.actuator.Actuator;
import cn.pug.dynamic.task.core.actuator.impl.DefaultActuatorImpl;
import cn.pug.dynamic.task.core.executor.ExecutorManager;
import cn.pug.dynamic.task.core.executor.impl.DefaultExecutorManager;
import cn.pug.dynamic.task.core.executor.logging.LogAdvicePublisher;
import cn.pug.dynamic.task.core.executor.logging.LogContext;
import cn.pug.dynamic.task.core.executor.logging.ThreadInputFileAppender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(DynamicTaskProperties.class)
@ConditionalOnProperty(prefix = DynamicTaskConst.MAIN_PROPERTIES_PREFIX, name = "enable", havingValue = "true")
public class DynamicTaskAutoConfiguration {
    @Resource
    private DynamicTaskProperties properties;

    @Bean
    @ConditionalOnMissingBean(ScriptAcquirable.class)
    public ScriptAcquirable scriptAcquirable() {
        return new DynamicScriptManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean(Actuator.class)
    public Actuator actuator(ScriptAcquirable scriptAcquirable, ExecutorManager executorManager) {
        return new DefaultActuatorImpl(scriptAcquirable, executorManager);
    }

    @Bean
    public DynamicTaskBannerPrinter dynamicTaskBannerPrinter() {
        return new DynamicTaskBannerPrinter(properties);
    }



    @Bean
    @ConditionalOnMissingBean(ExecutorManager.class)
    public ExecutorManager executorManager() {
        return new DefaultExecutorManager(properties);
    }

    @Bean
    public ThreadInputFileAppender threadInputFileAppender(){return new ThreadInputFileAppender(properties);}

    @Bean
    public LogAdvicePublisher logAdvicePublisher(){
        return new LogAdvicePublisher();
    }


    @Bean
    @ConditionalOnMissingBean(LogContext.class)
    public LogContext logContext(LogAdvicePublisher logAdvicePublisher) {
        return new LogContext(logAdvicePublisher);
    }
} 