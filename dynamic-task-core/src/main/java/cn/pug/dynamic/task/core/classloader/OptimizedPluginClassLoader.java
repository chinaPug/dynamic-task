package cn.pug.dynamic.task.core.classloader;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化的插件类加载器
 * 
 * 解决内存泄漏问题，支持类加载器正确关闭
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class OptimizedPluginClassLoader extends URLClassLoader implements Closeable {

    private final String pluginId;
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public OptimizedPluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (closed) {
            throw new IllegalStateException("类加载器已关闭: " + pluginId);
        }

        // 先检查缓存
        Class<?> clazz = loadedClasses.get(name);
        if (clazz != null) {
            return clazz;
        }

        // 调用父类加载
        clazz = super.findClass(name);
        
        // 缓存已加载的类
        loadedClasses.put(name, clazz);
        
        return clazz;
    }

    /**
     * 安全关闭类加载器，释放资源
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        
        closed = true;
        
        // 清理类缓存
        loadedClasses.clear();
        
        // 清理 ThreadLocal（防止内存泄漏）
        cleanupThreadLocals();
        
        // 调用父类关闭
        super.close();
        
        log.info("插件类加载器 [{}] 已关闭，资源已释放", pluginId);
    }

    /**
     * 清理 ThreadLocal 引用，防止内存泄漏
     */
    private void cleanupThreadLocals() {
        try {
            // 获取当前线程的 ThreadLocalMap
            Thread thread = Thread.currentThread();
            java.lang.reflect.Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalMap = threadLocalsField.get(thread);

            if (threadLocalMap != null) {
                java.lang.reflect.Field tableField = threadLocalMap.getClass().getDeclaredField("table");
                tableField.setAccessible(true);
                Object[] table = (Object[]) tableField.get(threadLocalMap);

                if (table != null) {
                    for (Object entry : table) {
                        if (entry != null) {
                            // 检查是否引用了当前类加载器
                            java.lang.reflect.Field valueField = entry.getClass().getDeclaredField("value");
                            valueField.setAccessible(true);
                            Object value = valueField.get(entry);
                            
                            if (value != null && value.getClass().getClassLoader() == this) {
                                // 清除引用
                                valueField.set(entry, null);
                                log.debug("清理 ThreadLocal 引用: {}", value.getClass().getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理 ThreadLocal 失败: {}", e.getMessage());
        }
    }

    /**
     * 强制 GC，建议调用
     */
    public static void suggestGC() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    public boolean isClosed() {
        return closed;
    }

    public String getPluginId() {
        return pluginId;
    }
}
