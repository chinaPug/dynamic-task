package cn.pug.dynamic.task.core.executor.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import cn.hutool.core.util.StrUtil;
import cn.pug.dynamic.task.core.DynamicTaskProperties;
import cn.pug.dynamic.task.core.util.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.bootstrap.BootstrapImportSelectorConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;


@Slf4j
public class LoggingListener implements GenericApplicationListener {

    private static final String CLASSPATH_PREFIX="classpath:";
    private static final String LOGBACK_LOCATION="classpath:dynamic-task-logback.xml";


    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        Class<?> type = resolvableType.getRawClass();
        if (type != null) {
            return ApplicationStartedEvent.class.isAssignableFrom(type);
        }
        return false;
    }

    /**
     * 处理应用事件的方法
     * 该方法主要用于检查事件源是否为SpringApplication实例，并且其来源是否为BootstrapImportSelectorConfiguration
     * 如果条件满足，则不执行任何操作；否则，将加载配置
     *
     * @param applicationEvent 应用事件，包含事件的源对象
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        try {
            // 尝试加载Spring Cloud的配置类，以确认是否在Spring Cloud环境中
            Class.forName("org.springframework.cloud.bootstrap.BootstrapImportSelectorConfiguration");

            // 获取事件源的类类型
            Class<?> type = applicationEvent.getSource().getClass();

            // 检查事件源是否为SpringApplication类的实例
            if (SpringApplication.class.isAssignableFrom(type)) {
                SpringApplication application = (SpringApplication) applicationEvent.getSource();

                // 获取所有事件源
                Set<Object> sources = application.getAllSources();

                // 如果只有一个事件源且为BootstrapImportSelectorConfiguration，则不执行任何操作
                if (sources.size() == 1 && sources.contains(BootstrapImportSelectorConfiguration.class)) {
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            // 如果找不到类，则记录警告日志，表明这不是一个Spring Cloud应用
            log.warn("日志在非spring cloud项目中初始化");
        }

        // 加载配置，无论是在Spring Cloud环境中还是非Spring Cloud环境中
        loadConfiguration();
    }


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 24;
    }

    public void loadConfiguration() {
        try {
            LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
            new ContextInitializer(loggerContext).configureByResource(getResourceUrl(LOGBACK_LOCATION));
        } catch (Exception e) {
            throw new RuntimeException("无法初始化logback配置文件：" + LOGBACK_LOCATION, e);
        }
    }

    /**
     * 根据资源路径获取资源的URL
     *
     * @param resource 资源路径，可以是类路径资源或文件系统中的文件
     * @return 资源的URL
     * @throws IOException 如果资源不存在或无法转换为URL时抛出此异常
     */
    public static URL getResourceUrl(String resource) throws IOException {
        // 检查资源路径是否以类路径前缀开头
        if (resource.startsWith(CLASSPATH_PREFIX)) {
            // 去除类路径前缀，获取实际资源路径
            String path = resource.substring(CLASSPATH_PREFIX.length());
            // 获取当前线程的类加载器
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // 使用类加载器获取资源的URL，如果类加载器为null，则使用系统类加载器
            URL url = (classLoader != null ? classLoader.getResource(path) : ClassLoader.getSystemResource(path));
            // 如果URL为null，表示资源不存在，抛出FileNotFoundException异常
            if (url == null) {
                throw new FileNotFoundException("资源【" + resource + "】不存在！");
            }
            // 返回资源的URL
            return url;
        }

        // 尝试将资源路径直接转换为URL
        try {
            return new URL(resource);
        } catch (MalformedURLException ex) {
            // 如果转换失败，表示资源路径不是有效的URL，将其视为文件路径并转换为File对象，再转换为URL
            return new File(resource).toURI().toURL();
        }
    }

}
