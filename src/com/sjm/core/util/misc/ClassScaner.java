package com.sjm.core.util.misc;

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

public class ClassScaner {
    static final Logger logger = LoggerFactory.getLogger(ClassScaner.class);

    public static void scanClasses(String[] pkgs, boolean recursive, Consumer<Class<?>> consumer) {
        ClassLoader loader = ClassScaner.class.getClassLoader();
        scanClassNames(pkgs, recursive, name -> {
            try {
                consumer.accept(Class.forName(name, false, loader));
            } catch (Throwable e) {
                logger.debug(e.getMessage(), e);
            }
        });

    }

    public static void scanClassNames(String[] pkgs, boolean recursive, Consumer<String> consumer) {
        ClassLoader loader = ClassScaner.class.getClassLoader();
        try {
            if (Platform.isAndroid())
                scanAndroid(loader, pkgs, recursive, consumer);
            else
                scanPC(loader, pkgs, recursive, consumer);
        } catch (Throwable ee) {
            logger.debug(ee.getMessage(), ee);
        }
    }

    private static void scanPC(ClassLoader loader, String[] pkgs, boolean recursive,
            Consumer<String> consumer) throws Exception {
        List<URL> jarUrls = new ArrayList<>();
        for (String pkg : pkgs) {
            Enumeration<URL> res = loader.getResources(pkg.replace('.', '/'));
            while (res.hasMoreElements()) {
                URL url = res.nextElement();
                switch (url.getProtocol()) {
                    case "jar":
                        jarUrls.add(url);
                        break;
                    case "file":
                        scanFile(new File(url.getPath()), recursive, consumer, pkg);
                        break;
                    default:
                        throw new IllegalArgumentException(url.toString());
                }
            }
        }
        if (!jarUrls.isEmpty())
            scanJar(jarUrls, recursive, consumer);
    }

    private static void scanAndroid(ClassLoader loader, String[] pkgs, boolean recursive,
            Consumer<String> consumer) throws Exception {
        pkgs = Arrays.copyOf(pkgs, pkgs.length);
        for (int i = 0; i < pkgs.length; i++)
            pkgs[i] += '.';
        Object pathList =
                getField(Class.forName("dalvik.system.BaseDexClassLoader"), loader, "pathList");
        Object[] dexElements = (Object[]) getField(pathList.getClass(), pathList, "dexElements");
        for (Object dexElement : dexElements) {
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
                        consumer.accept(entryName);
                        break;
                    }
                }
            }
        }
    }

    static class JarConnAndPkgs {
        public JarFile jarFile;
        public List<String> pkgPathList = new ArrayList<>();
    }

    private static void scanJar(List<URL> urls, boolean recursive, Consumer<String> consumer)
            throws Exception {
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
                String entryName = jar.getName();
                for (String pkgPath : pkgPaths) {
                    if (entryName.startsWith(pkgPath) && entryName.endsWith(".class")
                            && (recursive || entryName.indexOf('/', pkgPath.length()) == -1)) {
                        consumer.accept(entryName.substring(0, entryName.lastIndexOf('.'))
                                .replace("/", "."));
                        break;
                    }
                }
            }
        }
    }

    private static void scanFile(File file, boolean recursive, Consumer<String> consumer,
            String packageOrClass) {
        if (file.isFile()) {
            if (packageOrClass.endsWith(".class")) {
                String name =
                        packageOrClass.substring(0, packageOrClass.length() - ".class".length());
                if (!name.equals("package-info"))
                    consumer.accept(name);
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles())
                if (recursive || f.isFile())
                    scanFile(f, recursive, consumer, packageOrClass + "." + f.getName());
        }
    }

    private static Object getField(Class<?> clazz, Object obj, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
