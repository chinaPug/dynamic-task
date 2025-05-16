package cn.pug.dynamic.task.config;

import cn.pug.dynamic.task.constant.DynamicTaskConst;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = DynamicTaskConst.MAIN_PROPERTIES_PREFIX)
public class DynamicTaskProperties {
    private String localJarPath;
    private String remoteJarUrl;
    private boolean enable = false;
    private boolean enabledBanner = true;
}