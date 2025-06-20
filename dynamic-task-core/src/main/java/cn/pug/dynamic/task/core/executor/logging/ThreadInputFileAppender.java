package cn.pug.dynamic.task.core.executor.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.FileAppender;
import cn.pug.dynamic.task.core.DynamicTaskProperties;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NoArgsConstructor
public class ThreadInputFileAppender extends AppenderBase<ILoggingEvent> {
    // 日志根目录
    protected static String logDir;;
    // 格式化
    protected static String pattern;

    protected static boolean debug;

    private final Map<String, FileAppender<ILoggingEvent>> appenderMap=new ConcurrentHashMap<>();


    public ThreadInputFileAppender(DynamicTaskProperties dynamicTaskProperties){
        logDir=dynamicTaskProperties.getLogConfig().getLogDir();
        pattern=dynamicTaskProperties.getLogConfig().getPattern();
        debug=dynamicTaskProperties.getLogConfig().isDebug();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String logGroup = MDC.get(LogContext.LOG_GROUP_KEY);
        if (logGroup == null||(!debug&&!event.getLevel().isGreaterOrEqual(Level.INFO))) {
            // 如果没有设置logGroup，使用默认appender
            return;
        }
        FileAppender<ILoggingEvent> appender = getOrCreateAppender(logGroup);
        if (appender != null) {
            appender.doAppend(event);
        }
    }

    private FileAppender<ILoggingEvent> getOrCreateAppender(String logGroup) {
        return appenderMap.computeIfAbsent(logGroup, this::createAppender);
    }

    private FileAppender<ILoggingEvent> createAppender(String logGroup) {
        try {
            // 确保日志目录存在
            File dir = new File(logDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileAppender<ILoggingEvent> appender = new FileAppender<>();
            appender.setName("thread-business-" + logGroup);
            appender.setContext(getContext());

            // 设置文件路径
            String filePath = logDir + File.separator + logGroup + ".log";
            appender.setFile(filePath);

            // 设置编码器
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(getContext());
            encoder.setPattern(pattern);
            encoder.start();
            appender.setEncoder(encoder);

            appender.start();

            log.info("创建线程业务日志appender: {}", filePath);
            return appender;

        } catch (Exception e) {
            log.error("创建线程业务日志appender失败: {}", logGroup, e);
            return null;
        }
    }

    @Override
    public void stop() {
        // 停止所有子appender
        appenderMap.values().forEach(FileAppender::stop);
        appenderMap.clear();
        super.stop();
    }
}
