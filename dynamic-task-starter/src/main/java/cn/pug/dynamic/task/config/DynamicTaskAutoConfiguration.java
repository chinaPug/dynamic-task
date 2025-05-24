package cn.pug.dynamic.task.config;

import cn.pug.dynamic.task.DynamicTaskBannerPrinter;
import cn.pug.dynamic.task.constant.DynamicTaskConst;
import cn.pug.dynamic.task.script.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.script.acquirable.dynamic.DynamicScriptManager;
import cn.pug.dynamic.task.script.actuator.Actuator;
import cn.pug.dynamic.task.script.actuator.impl.DefaultActuatorImpl;
import cn.pug.dynamic.task.script.executor.ExecutorManager;
import cn.pug.dynamic.task.script.executor.impl.DefaultExecutorManager;
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
        //获取运行本机的cpu核心数
        int coreNum = Runtime.getRuntime().availableProcessors();
        //任务密集型初始化设置
        return new DefaultExecutorManager(coreNum, coreNum, 60L, 1000,properties);
    }
} 