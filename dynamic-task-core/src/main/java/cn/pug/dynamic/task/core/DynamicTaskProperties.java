package cn.pug.dynamic.task.core;

import cn.pug.dynamic.task.core.constant.DynamicTaskConst;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = DynamicTaskConst.MAIN_PROPERTIES_PREFIX)
public class DynamicTaskProperties {
    private String localJarPath;
    private boolean enable = false;
    private boolean enabledBanner = true;
    private List<ExecutorConfig> executor = new ArrayList<>();
    private LogConfig logConfig;
    @Data
    public static class ExecutorConfig {
        private String name;
        private Integer corePoolSize;
        private Integer maxPoolSize;
        private Integer queueCapacity=1000;
        private Integer keepAliveSeconds=60;
        private String taskRejectedPolicy="DEFAULT";
    }

    @Data
    public static class LogConfig{
        private String logDir="logs/dynamic-task";
        private String pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n[File:%file Line:%line]%n";
        private boolean debug=false;
    }
}