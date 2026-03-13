package cn.pug.dynamic.task.core.health;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件健康检查管理器
 * 
 * 定期检查插件运行状态，自动重启异常插件
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class PluginHealthChecker {

    private final Map<String, PluginHealthStatus> healthStatusMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PluginHealth-Checker");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = false;
    private long checkIntervalMs = 30000; // 默认30秒检查一次
    private int maxFailures = 3; // 最大失败次数

    /**
     * 启动健康检查
     */
    public void start() {
        if (running) return;
        running = true;

        scheduler.scheduleAtFixedRate(this::checkAllPlugins, 
            checkIntervalMs, checkIntervalMs, TimeUnit.MILLISECONDS);
        
        log.info("插件健康检查已启动，检查间隔: {}ms", checkIntervalMs);
    }

    /**
     * 停止健康检查
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        log.info("插件健康检查已停止");
    }

    /**
     * 注册插件
     */
    public void registerPlugin(String pluginId, HealthCheckable checkable) {
        PluginHealthStatus status = new PluginHealthStatus();
        status.setPluginId(pluginId);
        status.setCheckable(checkable);
        status.setLastCheckTime(LocalDateTime.now());
        status.setHealthy(true);
        
        healthStatusMap.put(pluginId, status);
        log.info("注册插件健康检查: {}", pluginId);
    }

    /**
     * 注销插件
     */
    public void unregisterPlugin(String pluginId) {
        healthStatusMap.remove(pluginId);
        log.info("注销插件健康检查: {}", pluginId);
    }

    /**
     * 检查所有插件
     */
    private void checkAllPlugins() {
        if (!running) return;

        for (Map.Entry<String, PluginHealthStatus> entry : healthStatusMap.entrySet()) {
            String pluginId = entry.getKey();
            PluginHealthStatus status = entry.getValue();

            try {
                boolean healthy = status.getCheckable().checkHealth();
                status.setLastCheckTime(LocalDateTime.now());

                if (healthy) {
                    if (!status.isHealthy()) {
                        log.info("插件 [{}] 恢复健康", pluginId);
                    }
                    status.setHealthy(true);
                    status.setFailureCount(0);
                } else {
                    status.setFailureCount(status.getFailureCount() + 1);
                    log.warn("插件 [{}] 健康检查失败，第 {} 次", pluginId, status.getFailureCount());

                    if (status.getFailureCount() >= maxFailures) {
                        status.setHealthy(false);
                        log.error("插件 [{}] 健康检查失败超过 {} 次，标记为不健康", pluginId, maxFailures);
                        
                        // 触发自动重启
                        autoRestartPlugin(pluginId, status);
                    }
                }
            } catch (Exception e) {
                log.error("检查插件 [{}] 健康状态时异常: {}", pluginId, e.getMessage());
                status.setFailureCount(status.getFailureCount() + 1);
            }
        }
    }

    /**
     * 自动重启插件
     */
    private void autoRestartPlugin(String pluginId, PluginHealthStatus status) {
        log.info("尝试自动重启插件: {}", pluginId);
        try {
            status.getCheckable().restart();
            status.setFailureCount(0);
            status.setHealthy(true);
            status.setLastRestartTime(LocalDateTime.now());
            log.info("插件 [{}] 自动重启成功", pluginId);
        } catch (Exception e) {
            log.error("插件 [{}] 自动重启失败: {}", pluginId, e.getMessage());
        }
    }

    /**
     * 获取插件健康状态
     */
    public PluginHealthStatus getHealthStatus(String pluginId) {
        return healthStatusMap.get(pluginId);
    }

    /**
     * 获取所有健康状态
     */
    public Map<String, PluginHealthStatus> getAllHealthStatus() {
        return new ConcurrentHashMap<>(healthStatusMap);
    }

    /**
     * 健康检查接口
     */
    public interface HealthCheckable {
        boolean checkHealth();
        void restart() throws Exception;
    }

    @Data
    public static class PluginHealthStatus {
        private String pluginId;
        private HealthCheckable checkable;
        private volatile boolean healthy;
        private int failureCount;
        private LocalDateTime lastCheckTime;
        private LocalDateTime lastRestartTime;

        /**
         * 获取距离上次检查的时间（秒）
         */
        public long getSecondsSinceLastCheck() {
            if (lastCheckTime == null) return -1;
            return ChronoUnit.SECONDS.between(lastCheckTime, LocalDateTime.now());
        }
    }
}
