package cn.pug.dynamic.task.script.acquirable.dynamic;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import cn.pug.dynamic.task.script.acquirable.ScriptManager;
import cn.pug.dynamic.task.script.template.SceneService;
import cn.pug.dynamic.task.script.template.annotation.Script;
import cn.pug.dynamic.task.script.template.model.Event;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class DynamicScriptManager implements ScriptManager {
    private final Map<String, SoftReference<Map.Entry<String, SceneService<?,?>>>> sceneMap = new ConcurrentHashMap<>(64);
    private final ReferenceQueue<Map.Entry<String, SceneService<?,?>>> referenceQueue = new ReferenceQueue<>();
    private final DynamicTaskProperties properties;

    public DynamicScriptManager(DynamicTaskProperties properties) {
        this.properties = properties;
    }


    private void cleanupReferences() {
        try {
            Reference<? extends Map.Entry<String, SceneService<?,?>>> ref;
            while ((ref = referenceQueue.poll()) != null) {
                final Reference<? extends Map.Entry<String, SceneService<?,?>>> finalRef = ref;
                // Find and remove the entry from sceneMap
                sceneMap.entrySet().removeIf(entry -> entry.getValue() == finalRef);
                log.info("Cleaned up garbage collected scene reference");
            }
        } catch (Exception e) {
            log.error("Error during reference cleanup", e);
        }
    }

    public SceneService<?,?> getSceneService(Event<?> event) {
        // 清除垃圾
        cleanupReferences();
        String identifyVal = event.getIdentifyVal();
        String scriptVersion = event.getScriptVersion();
        log.info("正在获取场景，标识值：{}，版本：{}", identifyVal, scriptVersion);
        
        SoftReference<Map.Entry<String, SceneService<?,?>>> ref = sceneMap.get(identifyVal);
        Map.Entry<String, SceneService<?,?>> entry = ref != null ? ref.get() : null;
        
        if (entry != null) {
            String currentVersion = entry.getKey();
            switch (scriptVersion.compareTo(currentVersion)) {
                case 1:
                    log.info("需要更新到新版本，正在卸载旧版本");
                    unloadSceneService(event);
                    registerSceneService(event);
                    ref = sceneMap.get(identifyVal);
                    entry = ref != null ? ref.get() : null;
                    if (entry == null) {
                        throw new RuntimeException("Failed to load new version");
                    }
                case 0:
                    log.info("在缓存中找到匹配的场景版本");
                    return entry.getValue();
                default:
                    log.warn("所需版本 {} 低于已加载版本 {}", scriptVersion, currentVersion);
                    throw new RuntimeException("所需版本低于已加载版本");
            }
        } else {
            log.info("缓存中未找到场景，正在注册新场景");
            registerSceneService(event);
            ref = sceneMap.get(identifyVal);
            entry = ref != null ? ref.get() : null;
            if (entry == null) {
                throw new RuntimeException("Failed to load new scene");
            }
            return entry.getValue();
        }
    }

    @Override
    public synchronized void registerSceneService(Event<?> event) {
        String identifyVal = event.getIdentifyVal();
        String scriptVersion = event.getScriptVersion();
        if (sceneMap.containsKey(identifyVal)) {
            return;
        }
        String jarName = identifyVal + "-" + scriptVersion + ".jar";
        URL jarUrl;
        SceneService<?,?> scene;
        Class<? extends SceneService<?,?>> clazz = null;
        log.info("正在注册场景，JAR包：{}", jarName);

        File localJar = new File(this.properties.getLocalJarPath() + "/" + jarName);
        if (localJar.exists()) {
            try {
                log.info("正在从本地路径加载JAR包：{}", localJar.getAbsolutePath());
                jarUrl = localJar.toURI().toURL();
                clazz = DynamicScriptClassLoader.loadJarFromUrl(jarUrl);
            } catch (Exception e) {
                log.error("从本地JAR包加载场景失败：{}", jarName, e);
                throw new RuntimeException("从本地JAR包加载场景失败：" + jarName, e);
            }
        } else {
            try {
                log.info("本地JAR包不存在，尝试从远程加载");
                String remotePath = properties.getRemoteJarUrl() + "/" + jarName;
            } catch (Exception e) {
                log.error("从远程JAR包加载场景失败：{}", jarName, e);
                throw new RuntimeException("从远程JAR包加载场景失败：" + jarName, e);
            }
        }
        try {
            scene = clazz.newInstance();
            Script script = clazz.getAnnotation(Script.class);
            if (!Objects.equals(script.version(), event.getScriptVersion())) {
                log.warn("主类{}版本{}与jar包版本{}不一致，但将以jar包版本为主，其为诉求版本", clazz.getName(), script.version(), event.getScriptVersion());
            }
        } catch (Exception e) {
            log.error("实例化场景对象失败：{}", clazz.getName(), e);
            throw new RuntimeException("实例化场景对象失败：" + clazz.getName(), e);
        }
        
        Map.Entry<String, SceneService<?,?>> entry = new AbstractMap.SimpleEntry<>(scriptVersion, scene);
        sceneMap.put(identifyVal, new SoftReference<>(entry, referenceQueue));
    }

    @Override
    public void unloadSceneService(Event<?> event) {
        String identifyVal = event.getIdentifyVal();
        log.info("正在卸载场景，标识值：{}", identifyVal);
        sceneMap.remove(identifyVal);
        log.info("场景卸载完成");
    }

} 