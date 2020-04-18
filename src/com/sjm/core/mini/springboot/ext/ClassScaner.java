package com.sjm.core.mini.springboot.ext;

import java.io.File;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;
import com.sjm.core.util.Platform;

public class ClassScaner {
    public static void main(String[] args) {
        ClassScaner.scan(new String[] {"com.sjm.common.json"}, true,
                clazz -> System.out.println(clazz));
    }

    static final Logger logger = LoggerFactory.getLogger(ClassScaner.class);

    public static void scan(String[] pkgs, boolean recursive, Consumer<Class<?>> consumer) {
        try {
            if (Platform.isAndroid()) {
                scanAndroid(pkgs, recursive, consumer);
            } else {
                scanPC(pkgs, recursive, consumer);
            }
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
    }

    private static void scanPC(String[] pkgs, boolean recursive, Consumer<Class<?>> consumer)
            throws Exception {
        ClassLoader classLoader = ClassScaner.class.getClassLoader();
        List<URL> jarUrls = new ArrayList<>();
        for (String pkg : pkgs) {
            Enumeration<URL> res = classLoader.getResources(pkg.replace('.', '/'));
            while (res.hasMoreElements()) {
                URL url = res.nextElement();
                try {
                    switch (url.getProtocol()) {
                        case "jar":
                            jarUrls.add(url);
                            break;
                        case "file":
                            scanFile(new File(url.getPath()), recursive, consumer, pkg,
                                    classLoader);
                            break;
                        default:
                            throw new IllegalArgumentException(url.toString());
                    }
                } catch (Exception e) {
                    logger.debug(e.getMessage(), e);
                }
            }
        }
        if (!jarUrls.isEmpty()) {
            scanJar(jarUrls, recursive, consumer, classLoader);
        }
    }


    private static void scanAndroid(String[] pkgs, boolean recursive, Consumer<Class<?>> consumer)
            throws Exception {
        pkgs = Arrays.copyOf(pkgs, pkgs.length);
        for (int i = 0; i < pkgs.length; i++) {
            pkgs[i] += '.';
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Object pathList = getField(Class.forName("dalvik.system.BaseDexClassLoader"), classLoader,
                "pathList");
        Object[] dexElements = (Object[]) getField(pathList.getClass(), pathList, "dexElements");
        for (Object dexElement : dexElements) {
            try {
                Object dexFile = getField(dexElement.getClass(), dexElement, "dexFile");
                if (dexFile == null)
                    continue;
                @SuppressWarnings("unchecked")
                Enumeration<String> entries = (Enumeration<String>) dexFile.getClass()
                        .getDeclaredMethod("entries").invoke(dexFile);
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement();
                    for (String pkg : pkgs) {
                        if (entryName.startsWith(pkg)
                                && (recursive || entryName.indexOf('.', pkg.length()) == -1)) {
                            try {
                                Class<?> clazz = Class.forName(entryName, false, classLoader);
                                consumer.accept(clazz);
                            } catch (Exception e) {
                                logger.debug(e.getMessage(), e);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            }
        }
    }

    static class JarConnAndPkgs {
        public JarFile jarFile;
        public List<String> pkgPathList = new ArrayList<>();
    }

    private static void scanJar(List<URL> urls, boolean recursive, Consumer<Class<?>> consumer,
            ClassLoader classLoader) throws Exception {
        Map<String, JarConnAndPkgs> conMap = new HashMap<>();
        for (URL url : urls) {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            String pkgPath = con.getEntryName();
            JarFile jarFile = con.getJarFile();
            JarConnAndPkgs cag = conMap.get(jarFile.getName());
            if (cag == null) {
                conMap.put(jarFile.getName(), cag = new JarConnAndPkgs());
                cag.jarFile = jarFile;
                cag.pkgPathList.add(pkgPath);
            } else {
                cag.pkgPathList.add(pkgPath);
            }
        }
        for (Map.Entry<String, JarConnAndPkgs> e : conMap.entrySet()) {
            JarConnAndPkgs cag = e.getValue();
            String[] pkgPaths = cag.pkgPathList.toArray(new String[cag.pkgPathList.size()]);
            Enumeration<JarEntry> jarEntries = cag.jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jar = jarEntries.nextElement();
                try {
                    String entryName = jar.getName();
                    for (String pkgPath : pkgPaths) {
                        if (entryName.startsWith(pkgPath) && entryName.endsWith(".class")
                                && (recursive || entryName.indexOf('/', pkgPath.length()) == -1)) {
                            int dotIndex = entryName.lastIndexOf('.');
                            String className = entryName.substring(0, dotIndex).replace("/", ".");
                            Class<?> clazz = Class.forName(className, false, classLoader);
                            consumer.accept(clazz);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    logger.debug(ex.getMessage(), ex);
                }
            }
        }
    }

    private static void scanFile(File file, boolean recursive, Consumer<Class<?>> consumer,
            String packageOrClass, ClassLoader classLoader) {
        if (file.isFile()) {
            try {
                int dotIndex = packageOrClass.lastIndexOf('.');
                String className = packageOrClass.substring(0, dotIndex);
                Class<?> clazz = Class.forName(className, false, classLoader);
                consumer.accept(clazz);
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (recursive || f.isFile()) {
                    scanFile(f, recursive, consumer, packageOrClass + "." + f.getName(),
                            classLoader);
                }
            }
        }
    }

    private static Object getField(Class<?> clazz, Object obj, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
