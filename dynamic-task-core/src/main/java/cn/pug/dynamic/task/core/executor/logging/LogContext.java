package cn.pug.dynamic.task.core.executor.logging;

import cn.hutool.core.io.FileUtil;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.common.api.model.OutputWrapper;
import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.File;


@Slf4j
public class LogContext {
    static final String TASK_ID_PARAM_KEY = "taskId-identifyVal";
    static final String LOG_GROUP_KEY = "log-group";
    private static LogAdvicePublisher logAdvicePublisher;

    public LogContext(LogAdvicePublisher logAdvicePublisher){
        LogContext.logAdvicePublisher=logAdvicePublisher;
    }

    private static final TransmittableThreadLocal<InputWrapper<?>> INPUT_WRAPPER_HODLER=new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<OutputWrapper<?>> OUTPUT_WRAPPER_HODLER=new TransmittableThreadLocal<>();

    public static <T> void setInputWrapper(InputWrapper<T> inputWrapper){
        INPUT_WRAPPER_HODLER.set(inputWrapper);
        log.debug("设置入参包装类：inputWrapper={}",inputWrapper.toString());
        MDC.put(TASK_ID_PARAM_KEY,inputWrapper.getTaskId().concat("-").concat(inputWrapper.getIdentifyVal()));
        log.debug("设置日志上下文: taskId-identifyVal={}", MDC.get(TASK_ID_PARAM_KEY));
    }

    public static <T> void setOutputWrapper(OutputWrapper<T> outputWrapper){
        OUTPUT_WRAPPER_HODLER.set(outputWrapper);
        log.debug("设置出参包装类：outputWrapper={}",outputWrapper.toString());
    }

    /**
     * 如果你想自定义日志文件名，重写这个方法即可
     */
    public static void setLogGroup(){
        MDC.put(LOG_GROUP_KEY, Thread.currentThread().getName().concat("-").concat(MDC.get(TASK_ID_PARAM_KEY)));
        log.debug("设置日志上下文: log-group={}", MDC.get(LOG_GROUP_KEY));
    }

    /**
     * 获取当前业务参数
     * @return 业务参数
     */
    public static InputWrapper<?> getInputWrapper() {
        return INPUT_WRAPPER_HODLER.get();
    }

    /**
     * 清除日志上下文
     */
    public static void clear() {
        LogContext.logAdvicePublisher.new LogAdviceEvent(INPUT_WRAPPER_HODLER.get(),
                OUTPUT_WRAPPER_HODLER.get(),
                FileUtil.getAbsolutePath(System.getProperty("user.dir")+File.separator+ThreadInputFileAppender.logDir+ File.separator+getLogFileName())).publish();
        INPUT_WRAPPER_HODLER.remove();
        // 清除MDC
        MDC.remove(TASK_ID_PARAM_KEY);
        MDC.remove(LOG_GROUP_KEY);
        log.debug("清除日志上下文");
    }
    /**
     * 获取当前线程的日志文件名
     * @return 日志文件名
     */
    public static String getLogFileName() {
        String logGroup = getLogGroup();
        return logGroup != null ? logGroup + ".log" : null;
    }


    public static String getLogGroup(){
        return MDC.get(LOG_GROUP_KEY);
    }
}
