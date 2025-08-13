package cn.pug.dynamic.task.core.acquirable;

import java.io.File;

@FunctionalInterface
public interface JarLoader {
    File loadJar(String scriptUrl);
}
