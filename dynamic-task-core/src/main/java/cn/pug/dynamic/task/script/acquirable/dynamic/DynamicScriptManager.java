package cn.pug.dynamic.task.script.acquirable.dynamic;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import cn.pug.dynamic.task.constant.TaskCodeMsg;
import cn.pug.dynamic.task.exception.PredicateException;
import cn.pug.dynamic.task.script.acquirable.ScriptManager;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.model.InputWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class DynamicScriptManager implements ScriptManager {
    // identifyVal->软引用封装类
    private final Map<String, SceneServiceSoftReference> sceneMap = new ConcurrentHashMap<>(64);
    private final ReferenceQueue<Map.Entry<String, SceneService<?,?>>> referenceQueue = new ReferenceQueue<>();
    private final DynamicTaskProperties properties;

    public DynamicScriptManager(DynamicTaskProperties properties) {
        this.properties = properties;
    }


    private void cleanupReferences() {
        try {
            SceneServiceSoftReference ref;
            while ((ref = (SceneServiceSoftReference)referenceQueue.poll()) != null) {
                final SceneServiceSoftReference finalRef = ref;
                sceneMap.entrySet().removeIf(entry -> entry.getValue() == finalRef);
                log.warn("{}脚本由于内存不足被回收",finalRef.identifyVal);
            }
        } catch (Exception e) {
            log.error("脚本回收出现问题", e);
        }
    }

    public SceneService<?,?> getSceneService(InputWrapper<?> inputWrapper) {
        // 清除垃圾
        cleanupReferences();
        String identifyVal = inputWrapper.getIdentifyVal();
        String scriptVersion = inputWrapper.getScriptVersion();
        log.debug("正在获取场景，标识值：{}，版本：{}", identifyVal, scriptVersion);
        // 根据identifyVal获取软引用封装类
        SoftReference<Map.Entry<String, SceneService<?,?>>> ref = sceneMap.get(identifyVal);
        //如果润引用封装类为null，则说明该类没被加载过或者已经被卸载
        Map.Entry<String, SceneService<?,?>> entry = ref != null ? ref.get() : null;
        if (entry != null) {
            // 获取当前版本号
            String currentVersion = entry.getKey();
            // 对比需求版本
            switch (scriptVersion.compareTo(currentVersion)) {
                case 1:
                    log.debug("需要更新到新版本，正在卸载旧版本");
                    // 卸载旧版本
                    unloadSceneService(inputWrapper);
                    // 注册新版本
                    registerSceneService(inputWrapper);
                    // 再次根据identifyVal获取软引用封装类
                    ref = sceneMap.get(identifyVal);
                    entry = Objects.requireNonNull(ref.get());
                case 0:
                    log.debug("在缓存中找到匹配的场景版本");
                    return entry.getValue();
                default:
                    log.warn("所需版本 {} 低于已加载版本 {}", scriptVersion, currentVersion);
                    throw new PredicateException(TaskCodeMsg.SCRIPT_VERSION_ERROR);
            }
        } else {
            log.debug("缓存中未找到场景，正在注册新场景");
            registerSceneService(inputWrapper);
            ref = sceneMap.get(identifyVal);
            entry = ref != null ? ref.get() : null;
            if (entry == null) {
                throw new RuntimeException("Failed to load new scene");
            }
            return entry.getValue();
        }
    }

    @Override
    public synchronized void registerSceneService(InputWrapper<?> inputWrapper) {
        // 清除垃圾
        cleanupReferences();
        String identifyVal = inputWrapper.getIdentifyVal();
        String scriptVersion = inputWrapper.getScriptVersion();
        // 结合该方法是同步方法使用
        if (sceneMap.containsKey(identifyVal)) {
            return;
        }
        String jarName = identifyVal.concat("-").concat(scriptVersion).concat(".jar");
        URL jarUrl;
        SceneService<?,?> scene;
        Class<? extends SceneService<?,?>> clazz = null;
        log.debug("正在注册场景，JAR包：{}", jarName);

        File localJar = new File(this.properties.getLocalJarPath().concat("/").concat(jarName));
        if (localJar.exists()) {
            try {
                log.debug("正在从本地路径加载JAR包：{}", localJar.getAbsolutePath());
                jarUrl = localJar.toURI().toURL();
                clazz = DynamicScriptClassLoader.loadJarFromUrl(jarUrl);
            } catch (Exception e) {
                log.error("从本地JAR包加载场景失败：{}", jarName, e);
                throw new RuntimeException("从本地JAR包加载场景失败：" + jarName, e);
            }
        } else {
            throw new RuntimeException("获取JAR包失败");
        }
        try {
            scene = clazz.newInstance();
        } catch (Exception e) {
            log.error("实例化场景对象失败：{}", clazz.getName(), e);
            throw new RuntimeException("实例化场景对象失败：" + clazz.getName(), e);
        }
        
        Map.Entry<String, SceneService<?,?>> entry = new AbstractMap.SimpleEntry<>(scriptVersion, scene);
        sceneMap.put(identifyVal, new SceneServiceSoftReference(entry, referenceQueue));
    }

    @Override
    public void unloadSceneService(InputWrapper<?> inputWrapper) {
        String identifyVal = inputWrapper.getIdentifyVal();
        log.debug("正在卸载场景，标识值：{}", identifyVal);
        sceneMap.remove(identifyVal);
        log.debug("场景卸载完成");
    }


    private static class SceneServiceSoftReference extends SoftReference<Map.Entry<String, SceneService<?,?>>> {
        private final String identifyVal;
        public SceneServiceSoftReference(Map.Entry<String, SceneService<?,?>> referent, ReferenceQueue<? super Map.Entry<String, SceneService<?,?>>> q) {
            super(referent, q);
            // 这里需要用new String否则属于强引用了封装类对象，就不会被回收
            identifyVal= new String(referent.getKey());
        }
    }
} 