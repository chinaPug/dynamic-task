package cn.pug.dynamic.task.core.dependency;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件依赖管理器
 * 
 * 管理插件之间的依赖关系，支持依赖解析和加载顺序控制
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class PluginDependencyManager {

    private final Map<String, PluginInfo> plugins = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();

    /**
     * 注册插件
     */
    public void registerPlugin(String pluginId, String version, List<String> dependencies) {
        PluginInfo info = new PluginInfo(pluginId, version, dependencies);
        plugins.put(pluginId, info);
        dependencyGraph.put(pluginId, new HashSet<>(dependencies));
        log.info("注册插件: {} v{}", pluginId, version);
    }

    /**
     * 解析加载顺序（拓扑排序）
     */
    public List<String> resolveLoadOrder() throws CircularDependencyException {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String pluginId : plugins.keySet()) {
            if (!visited.contains(pluginId)) {
                dfs(pluginId, visited, visiting, result);
            }
        }

        Collections.reverse(result);
        log.info("插件加载顺序: {}", result);
        return result;
    }

    private void dfs(String pluginId, Set<String> visited, Set<String> visiting, List<String> result) 
            throws CircularDependencyException {
        visiting.add(pluginId);

        Set<String> deps = dependencyGraph.getOrDefault(pluginId, Collections.emptySet());
        for (String dep : deps) {
            if (visiting.contains(dep)) {
                throw new CircularDependencyException("检测到循环依赖: " + pluginId + " -> " + dep);
            }
            if (!visited.contains(dep)) {
                if (!plugins.containsKey(dep)) {
                    throw new MissingDependencyException("缺少依赖插件: " + dep + " (被 " + pluginId + " 依赖)");
                }
                dfs(dep, visited, visiting, result);
            }
        }

        visiting.remove(pluginId);
        visited.add(pluginId);
        result.add(pluginId);
    }

    /**
     * 检查是否可以卸载插件
     */
    public boolean canUnload(String pluginId) {
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            if (entry.getValue().contains(pluginId)) {
                log.warn("插件 {} 被 {} 依赖，无法卸载", pluginId, entry.getKey());
                return false;
            }
        }
        return true;
    }

    /**
     * 卸载插件
     */
    public void unregisterPlugin(String pluginId) {
        plugins.remove(pluginId);
        dependencyGraph.remove(pluginId);
        log.info("注销插件: {}", pluginId);
    }

    @Data
    public static class PluginInfo {
        private final String pluginId;
        private final String version;
        private final List<String> dependencies;
    }

    public static class CircularDependencyException extends Exception {
        public CircularDependencyException(String message) {
            super(message);
        }
    }

    public static class MissingDependencyException extends RuntimeException {
        public MissingDependencyException(String message) {
            super(message);
        }
    }
}
