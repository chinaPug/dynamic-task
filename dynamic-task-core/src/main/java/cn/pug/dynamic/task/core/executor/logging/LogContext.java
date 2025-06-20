package cn.pug.dynamic.task.core.executor.logging;

import cn.pug.dynamic.task.common.api.model.InputWrapper;
import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class LogContext {
    static final String TASK_ID_PARAM_KEY = "taskId-identifyVal";
    static final String LOG_GROUP_KEY = "logGroup";

    private static final TransmittableThreadLocal<InputWrapper<?>> INPUT_WRAPPER_HODLER=new TransmittableThreadLocal<>();

    public static <T> void setInputWrapper(InputWrapper<T> inputWrapper){
        INPUT_WRAPPER_HODLER.set(inputWrapper);
        MDC.put(TASK_ID_PARAM_KEY,inputWrapper.getTaskId().concat("-").concat(inputWrapper.getIdentifyVal()));
        log.debug("设置日志上下文: taskId-identifyVal={}", MDC.get(TASK_ID_PARAM_KEY));
    }

    public static void setLogGroup(){
        MDC.put(LOG_GROUP_KEY, Thread.currentThread().getName().concat("-").concat(MDC.get(TASK_ID_PARAM_KEY)));
        log.debug("设置日志上下文: taskId-identifyVal={}", MDC.get(TASK_ID_PARAM_KEY));
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
