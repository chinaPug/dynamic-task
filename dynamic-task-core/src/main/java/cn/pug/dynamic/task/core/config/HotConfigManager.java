package cn.pug.dynamic.task.core.config;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 热配置管理器
 * 
 * 监听配置文件变化，自动重新加载
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class HotConfigManager {

    private final String configPath;
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private final Map<String, Consumer<Object>> listeners = new ConcurrentHashMap<>();
    private WatchService watchService;
    private volatile boolean running = false;

    public HotConfigManager(String configPath) {
        this.configPath = configPath;
    }

    /**
     * 启动配置监听
     */
    public void start() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(configPath).getParent();
            
            if (path != null) {
                path.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
                
                running = true;
                loadConfig();
                
                // 启动监听线程
                Thread watchThread = new Thread(this::watchConfig, "HotConfig-Watcher");
                watchThread.setDaemon(true);
                watchThread.start();
                
                log.info("热配置管理器已启动，监听: {}", configPath);
            }
        } catch (IOException e) {
            log.error("启动配置监听失败: {}", e.getMessage());
        }
    }

    /**
     * 停止配置监听
     */
    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭配置监听失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 监听配置文件变化
     */
    private void watchConfig() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    if (changed.toString().equals(Paths.get(configPath).getFileName().toString())) {
                        log.info("配置文件发生变化，重新加载...");
                        Thread.sleep(100); // 等待文件写入完成
                        loadConfig();
                        notifyListeners();
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 加载配置
     */
    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try {
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                log.warn("配置文件不存在: {}", configPath);
                return;
            }

            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Map<String, Object> newConfig = yaml.load(fis);
                if (newConfig != null) {
                    configCache.clear();
                    configCache.putAll(newConfig);
                    log.info("配置已加载，共 {} 个配置项", configCache.size());
                }
            }
        } catch (IOException e) {
            log.error("加载配置失败: {}", e.getMessage());
        }
    }

    /**
     * 获取配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = configCache.get(key);
        if (value == null) {
            // 支持嵌套配置，如 "dynamic.task.enable"
            String[] parts = key.split("\\.");
            Map<String, Object> current = configCache;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    return defaultValue;
                }
            }
            value = current.get(parts[parts.length - 1]);
        }
        
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("配置类型不匹配: {} = {}", key, value);
            return defaultValue;
        }
    }

    /**
     * 注册配置变更监听器
     */
    public void addListener(String key, Consumer<Object> listener) {
        listeners.put(key, listener);
    }

    /**
     * 通知监听器
     */
    private void notifyListeners() {
        listeners.forEach((key, listener) -> {
            try {
                Object value = get(key, null);
                if (value != null) {
                    listener.accept(value);
                }
            } catch (Exception e) {
                log.error("通知监听器失败 [{}]: {}", key, e.getMessage());
            }
        });
    }

    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfig() {
        return new ConcurrentHashMap<>(configCache);
    }
}
