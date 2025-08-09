package cn.pug.dynamic.task.core.acquirable.dynamic;

import cn.pug.dynamic.task.core.constant.TaskCodeMsg;
import cn.pug.dynamic.task.core.exception.PredicateException;
import cn.pug.dynamic.task.common.api.Scene;
import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.annotation.Script;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class DynamicScriptClassLoader extends ClassLoader {
    private boolean used = false;
    private Map<String, byte[]> classBytesMap = new HashMap<>();
    private boolean mainClassTag=false;
    private JarFile jarFile;
    private Class<?> mainClass;
    private DynamicScriptClassLoader(JarFile jarFile) throws Exception {
        super(Thread.currentThread().getContextClassLoader());
        this.jarFile = jarFile;
        init(jarFile);
    }


    private void clean(){
        classBytesMap.clear();
    }
    private void init(JarFile jarFile) throws Exception {
        if (used){
            throw new RuntimeException("该加载器不可二次使用");
        }
        used= true;
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
                    String className = DynamicScriptClassLoader.getClassName(entry.getName());
                    classBytesMap.put(className, classBytes);
                    log.debug("从JarFile中获取类名:【{}】 ", className);
                } catch (IOException e) {
                    throw new RuntimeException("从JarFile中获取字节码失败", e);
                }
            }
        }
    }



    public static Class<? extends SceneService<?, ?>> loadJarFromUrl(URL jarUrl) throws Exception {
        try {
            // 使用 JarFile 读取 JAR 文件（支持本地文件路径或远程流）
            try (JarFile jarFile = new JarFile(jarUrl.getPath())) {
                DynamicScriptClassLoader classLoader = new DynamicScriptClassLoader(jarFile);
                classLoader.loadAllClass();
                return (Class<? extends SceneService<?, ?>>) classLoader.mainClass;
            }
        } catch (Exception e) {
            log.error("加载类失败: ", e);
            throw (e instanceof PredicateException) ? e : new PredicateException(TaskCodeMsg.CLASS_LOAD_ERROR);
        }
    }

    private void loadAllClass(){
        classBytesMap.keySet().forEach(className -> {
            try {
                super.loadClass(className);
            } catch (ClassNotFoundException e) {
                log.error("加载类:【{}】失败: ",className, e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        Class<?> targetClass;
        log.debug("父类加载器加载失败，交由本层加载器加载: " + className);
        try {
            byte[] classBytes = classBytesMap.get(className);
            targetClass = defineClass(className, classBytes, 0, classBytes.length);
            // 判断类是否继承自 Scene且添加了 Script 注解
            if (Scene.class.isAssignableFrom(targetClass) && targetClass.getAnnotation(Script.class) != null) {
                if (mainClassTag) {
                    log.error("包：【{}】中存在多个标注了Script注解的类，无法定位唯一主类", jarFile.getName());
                    throw new PredicateException(TaskCodeMsg.SCRIPT_DEFINITION_ERROR);
                }
                mainClassTag = true;
                mainClass= targetClass;
            }
        } catch (Exception ex) {
            log.error("加载类失败: " + className, ex);
            throw new PredicateException(TaskCodeMsg.CLASS_LOAD_ERROR);
        }
        return targetClass;
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
