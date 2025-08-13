package cn.pug.dynamic.task.config;

import cn.pug.dynamic.task.core.DynamicTaskBannerPrinter;
import cn.pug.dynamic.task.core.DynamicTaskProperties;
import cn.pug.dynamic.task.core.acquirable.JarLoader;
import cn.pug.dynamic.task.core.constant.DynamicTaskConst;
import cn.pug.dynamic.task.core.acquirable.ScriptAcquirable;
import cn.pug.dynamic.task.core.acquirable.dynamic.DynamicScriptManager;
import cn.pug.dynamic.task.core.actuator.Actuator;
import cn.pug.dynamic.task.core.actuator.impl.DefaultActuatorImpl;
import cn.pug.dynamic.task.core.executor.ExecutorLoaderBalance;
import cn.pug.dynamic.task.core.executor.ExecutorManager;
import cn.pug.dynamic.task.core.executor.impl.DefaultExecutorManager;
import cn.pug.dynamic.task.core.executor.logging.LogAdvicePublisher;
import cn.pug.dynamic.task.core.executor.logging.LogContext;
import cn.pug.dynamic.task.core.executor.logging.ThreadInputFileAppender;
import cn.pug.dynamic.task.core.util.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.Resource;

@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicTaskProperties.class)
@ConditionalOnProperty(prefix = DynamicTaskConst.MAIN_PROPERTIES_PREFIX, name = "enable", havingValue = "true")
public class DynamicTaskAutoConfiguration {
    @Resource
    private DynamicTaskProperties properties;

    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }


    @Bean
    @ConditionalOnMissingBean(ScriptAcquirable.class)
    public ScriptAcquirable scriptAcquirable(@Autowired(required = false) JarLoader jarLoader) {
        log.debug("初始化动态脚本管理器，是否使用自定义Jar加载器: {}", jarLoader != null);
        if (jarLoader != null) {
            log.debug("使用自定义Jar加载器初始化DynamicScriptManager");
            return new DynamicScriptManager(properties, jarLoader);
        } else {
            log.debug("使用默认配置初始化DynamicScriptManager");
            return new DynamicScriptManager(properties);
        }
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
    public ExecutorManager executorManager(@Autowired(required = false) ExecutorLoaderBalance executorLoaderBalance) {
        log.debug("初始化动态脚本管理器，是否使用自定义线程池负载均衡器: {}", executorLoaderBalance != null);
        if (executorLoaderBalance != null) {
            log.debug("使用自定义线程池负载均衡器");
            return new DefaultExecutorManager(properties, executorLoaderBalance);
        } else {
            log.debug("使用默认的线程池负载均衡器");
            return new DefaultExecutorManager(properties);
        }
    }


    @Bean
    public LogAdvicePublisher logAdvicePublisher() {
        return new LogAdvicePublisher();
    }


    @Bean
    @ConditionalOnMissingBean(LogContext.class)
    public LogContext logContext(LogAdvicePublisher logAdvicePublisher) {
        return new LogContext(logAdvicePublisher);
    }
} 