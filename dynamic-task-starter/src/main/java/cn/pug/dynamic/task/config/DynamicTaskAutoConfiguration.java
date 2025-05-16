package cn.pug.dynamic.task.config;

import cn.pug.dynamic.task.ApplicationContextHolder;
import cn.pug.dynamic.task.DynamicTaskBannerPrinter;
import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.actuator.impl.DefaultActuatorImpl;
import cn.pug.dynamic.task.constant.DynamicTaskConst;
import cn.pug.dynamic.task.script.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.script.acquirable.dynamic.DynamicScriptAcquirable;
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
        return new DynamicScriptAcquirable(properties);
    }

    @Bean
    @ConditionalOnMissingBean(Actuator.class)
    public Actuator actuator(ScriptAcquirable scriptAcquirable) {
        return new DefaultActuatorImpl(scriptAcquirable);
    }

    @Bean
    public DynamicTaskBannerPrinter dynamicTaskBannerPrinter() {
        return new DynamicTaskBannerPrinter(properties);
    }

    @Bean
    public ApplicationContextHolder DynamicTaskApplicationContextHolder() {
        return new ApplicationContextHolder();
    }
} 