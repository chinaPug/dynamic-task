package cn.pug.dynamic.task.script.acquirable.dynamic;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import cn.pug.dynamic.task.script.template.Scene;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.annotation.Script;
import cn.pug.dynamic.task.script.template.exception.PredicateException;
import cn.pug.dynamic.task.script.template.model.TaskCodeMsg;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class DynamicScriptClassLoader extends URLClassLoader {
    private static DynamicTaskProperties properties;
    private static String remotePath;

    private DynamicScriptClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }


    public static Class<? extends SceneService<?, ?>> loadJarFromUrl(URL jarUrl) {
        Class<?> targetClass = null;
        try (DynamicScriptClassLoader classLoader = new DynamicScriptClassLoader(new URL[]{jarUrl}, DynamicScriptClassLoader.class.getClassLoader())) {
            // 使用 JarFile 读取 JAR 文件（支持本地文件路径或远程流）
            try (JarFile jarFile = new JarFile(jarUrl.getPath())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        // 读取类文件字节码
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            byte[] classBytes = new byte[(int) entry.getSize()];
                            int bytesRead = is.read(classBytes);
                            if (bytesRead != classBytes.length) {
                                throw new IOException("读取类文件不完整: " + entry.getName());
                            }
                            // 获取类名
                            String className = classLoader.getClassName(entry.getName());
                            log.info("尝试加载类: " + className);
                            // 定义类到 JVM 中
                            Class<?> clazz;
                            // 遵循双亲委派
                            try {
                                log.info("抛给父类加载器加载类: " + className);
                                classLoader.getParent().loadClass(className);
                            } catch (ClassNotFoundException e) {
                                log.info("父类加载器加载失败，交由本层加载器加载: " + className);
                                try {
                                    clazz = classLoader.defineClass(className, classBytes, 0, classBytes.length);
                                    // 判断类是否继承自 Scene且添加了 Script 注解
                                    if (Scene.class.isAssignableFrom(clazz) && clazz.getAnnotation(Script.class) != null) {
                                        targetClass = clazz;
                                    }
                                } catch (Exception ex) {
                                    log.error("加载类失败: " + className, ex);
                                    throw new PredicateException(TaskCodeMsg.CLASS_LOAD_ERROR);
                                }
                            }
                            log.info("已加载类: " + className);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("加载类失败: ", e);
            throw new PredicateException(TaskCodeMsg.CLASS_LOAD_ERROR);
        }
        return (Class<? extends SceneService<?, ?>>) targetClass;
    }


    /**
     * 根据 JAR 条目名称获取类的全限定名。
     *
     * @param entryName JAR 条目名称
     * @return 类的全限定名
     */
    private static String getClassName(String entryName) {
        return entryName.replace("/", ".").replace(".class", "");
    }
}
