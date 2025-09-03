package cn.pug.dynamic.task.core.acquirable.dynamic;

import cn.pug.dynamic.task.core.DynamicTaskProperties;
import cn.pug.dynamic.task.core.acquirable.JarLoader;
import cn.pug.dynamic.task.core.constant.TaskCodeMsg;
import cn.pug.dynamic.task.core.exception.PredicateException;
import cn.pug.dynamic.task.core.acquirable.ScriptManager;
import cn.pug.dynamic.task.common.api.SceneService;
import cn.pug.dynamic.task.common.api.model.InputWrapper;
import cn.pug.dynamic.task.core.util.VersionComparator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;


@Slf4j
public class DynamicScriptManager implements ScriptManager {
    private  Cache<String, Map.Entry<String, SceneService<?,?>>> sceneCache;
    private  DynamicTaskProperties properties;
    private JarLoader jarLoader;
    private Comparator<String> versionComparator = new VersionComparator();
    public DynamicScriptManager(DynamicTaskProperties properties) {
        init(properties);
    }
    
    public DynamicScriptManager(DynamicTaskProperties properties, JarLoader jarLoader) {
        this.jarLoader = jarLoader;
        init(properties);
    }

    private void init(DynamicTaskProperties properties){
        this.properties = properties;
        this.sceneCache = Caffeine.newBuilder()
                .softValues() // 使用软引用存储值
                .removalListener((String key, Map.Entry<String, SceneService<?,?>> value, RemovalCause cause) -> {
                    log.debug("脚本{}被移除，原因{}", key, cause);
                })
                .build();
    }


    @Override
    public SceneService<?,?> getSceneService(InputWrapper<?> inputWrapper) {
        String identifyVal = inputWrapper.getIdentifyVal();
        String scriptVersion = inputWrapper.getScriptVersion();
        log.debug("正在获取场景，标识值：{}，版本：{}", identifyVal, scriptVersion);
        // 根据identifyVal获取
        //如果为null，则说明该类没被加载过或者已经被卸载
        Map.Entry<String, SceneService<?,?>> entry = sceneCache.getIfPresent(identifyVal);
        //如果润引用封装类为null，则说明该类没被加载过或者已经被卸载
        if (entry != null) {
            // 获取当前版本号
            String currentVersion = entry.getKey();
            // 对比需求版本
            switch (versionComparator.compare(scriptVersion,currentVersion)) {
                case 1:
                    log.debug("需要更新到新版本，正在卸载旧版本");
                    // 卸载旧版本
                    unloadSceneService(inputWrapper);
                    // 注册新版本
                    registerSceneService(inputWrapper);
                    // 再次根据identifyVal获取软引用封装类
                    entry = sceneCache.getIfPresent(identifyVal);
                    if (entry == null) {
                        throw new RuntimeException("无法加载scene");
                    }
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
            entry = sceneCache.getIfPresent(identifyVal);
            if (entry == null) {
                throw new RuntimeException("无法加载scene");
            }
            return entry.getValue();
        }
    }

    @Override
    public synchronized void registerSceneService(InputWrapper<?> inputWrapper) {
        String identifyVal = inputWrapper.getIdentifyVal();
        String scriptVersion = inputWrapper.getScriptVersion();
        // 结合该方法是同步方法使用
        if (sceneCache.asMap().containsKey(identifyVal)) {
            return;
        }
        String jarName = identifyVal.concat("-").concat(scriptVersion).concat(".jar");
        URL jarUrl;
        SceneService<?,?> scene;
        Class<? extends SceneService<?,?>> clazz;
        log.debug("正在注册场景，JAR包：{}", jarName);
        File jarFile;
        if (jarLoader!=null){
            jarFile = jarLoader.loadJar(inputWrapper.getScriptUrl());
        }else {
            jarFile = new File(this.properties.getLocalJarPath().concat("/").concat(jarName));
        }
        if (jarFile.exists()) {
            try {
                log.debug("正在从本地路径加载JAR包：{}", jarFile.getAbsolutePath());
                jarUrl = jarFile.toURI().toURL();
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
        sceneCache.put(identifyVal,entry);
    }

    @Override
    public void unloadSceneService(InputWrapper<?> inputWrapper) {
        String identifyVal = inputWrapper.getIdentifyVal();
        log.debug("正在卸载场景，标识值：{}", identifyVal);
        sceneCache.invalidate(identifyVal);
        log.debug("场景卸载完成");
    }


} 