package cn.pug.dynamic.task.core.version;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件版本管理器
 * 
 * 支持版本回滚、历史版本管理
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class PluginVersionManager {

    private final String backupDir;
    private final Map<String, List<VersionHistory>> versionHistoryMap = new ConcurrentHashMap<>();
    private final Map<String, String> currentVersions = new ConcurrentHashMap<>();

    public PluginVersionManager(String backupDir) {
        this.backupDir = backupDir;
        initBackupDir();
    }

    private void initBackupDir() {
        try {
            Files.createDirectories(Paths.get(backupDir));
            log.info("版本备份目录: {}", backupDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建备份目录: " + backupDir, e);
        }
    }

    /**
     * 记录当前版本（用于回滚）
     */
    public void recordVersion(String pluginId, String version, String jarPath) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("%s_%s_%s.jar", pluginId, version, timestamp);
            Path backupPath = Paths.get(backupDir, backupFileName);

            // 备份当前版本
            Files.copy(Paths.get(jarPath), backupPath, StandardCopyOption.REPLACE_EXISTING);

            // 记录版本历史
            VersionHistory history = new VersionHistory();
            history.setPluginId(pluginId);
            history.setVersion(version);
            history.setBackupPath(backupPath.toString());
            history.setTimestamp(LocalDateTime.now());
            history.setJarPath(jarPath);

            versionHistoryMap.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(history);
            currentVersions.put(pluginId, version);

            log.info("记录插件 [{}] 版本 [{}] 备份", pluginId, version);

            // 只保留最近10个版本
            cleanupOldVersions(pluginId);

        } catch (IOException e) {
            log.error("备份版本失败: {}", e.getMessage());
            throw new RuntimeException("版本备份失败", e);
        }
    }

    /**
     * 回滚到上一个版本
     */
    public VersionHistory rollback(String pluginId) {
        List<VersionHistory> histories = versionHistoryMap.get(pluginId);
        if (histories == null || histories.size() < 2) {
            throw new RollbackException("没有可回滚的历史版本: " + pluginId);
        }

        // 获取上一个版本
        VersionHistory current = histories.get(histories.size() - 1);
        VersionHistory previous = histories.get(histories.size() - 2);

        try {
            // 恢复上一个版本
            Path targetPath = Paths.get(current.getJarPath());
            Path backupPath = Paths.get(previous.getBackupPath());

            Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 更新当前版本
            currentVersions.put(pluginId, previous.getVersion());

            log.info("插件 [{}] 回滚到版本 [{}]", pluginId, previous.getVersion());

            return previous;

        } catch (IOException e) {
            log.error("回滚失败: {}", e.getMessage());
            throw new RollbackException("回滚失败: " + e.getMessage(), e);
        }
    }

    /**
     * 回滚到指定版本
     */
    public VersionHistory rollbackTo(String pluginId, String targetVersion) {
        List<VersionHistory> histories = versionHistoryMap.get(pluginId);
        if (histories == null) {
            throw new RollbackException("没有版本历史: " + pluginId);
        }

        VersionHistory target = histories.stream()
                .filter(h -> h.getVersion().equals(targetVersion))
                .findFirst()
                .orElseThrow(() -> new RollbackException("版本不存在: " + targetVersion));

        try {
            VersionHistory current = histories.get(histories.size() - 1);
            Path targetPath = Paths.get(current.getJarPath());
            Path backupPath = Paths.get(target.getBackupPath());

            Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            currentVersions.put(pluginId, targetVersion);

            log.info("插件 [{}] 回滚到指定版本 [{}]", pluginId, targetVersion);

            return target;

        } catch (IOException e) {
            log.error("回滚失败: {}", e.getMessage());
            throw new RollbackException("回滚失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取版本历史
     */
    public List<VersionHistory> getVersionHistory(String pluginId) {
        return versionHistoryMap.getOrDefault(pluginId, Collections.emptyList());
    }

    /**
     * 获取当前版本
     */
    public String getCurrentVersion(String pluginId) {
        return currentVersions.get(pluginId);
    }

    /**
     * 清理旧版本（只保留最近10个）
     */
    private void cleanupOldVersions(String pluginId) {
        List<VersionHistory> histories = versionHistoryMap.get(pluginId);
        if (histories == null || histories.size() <= 10) {
            return;
        }

        // 删除旧版本
        List<VersionHistory> toRemove = histories.subList(0, histories.size() - 10);
        for (VersionHistory history : toRemove) {
            try {
                Files.deleteIfExists(Paths.get(history.getBackupPath()));
                log.debug("删除旧版本备份: {}", history.getBackupPath());
            } catch (IOException e) {
                log.warn("删除旧版本备份失败: {}", e.getMessage());
            }
        }

        // 更新列表
        histories.subList(0, histories.size() - 10).clear();
    }

    @Data
    public static class VersionHistory {
        private String pluginId;
        private String version;
        private String jarPath;
        private String backupPath;
        private LocalDateTime timestamp;
    }

    public static class RollbackException extends RuntimeException {
        public RollbackException(String message) {
            super(message);
        }

        public RollbackException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
