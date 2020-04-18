package com.sjm.core.util;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;


public class Reflection {

    public static IClass forClass(Class<?> clazz) {
        return IClassImpl.valueOf(clazz);
    }

    public static IClass forName(String name) {
        return IClassImpl.valueOf(name);
    }

    public static IType forType(Type type) {
        return ITypeImpl.valueOf(type);
    }

    public interface Base {
        public String getName();

        public IClass getIClass();

        public IType getIType();

        public int getModifier();

        public Annotations getAnnotations();

        public IClass getDeclaringIClass();
    }

    public interface IType {
        public static enum TypeFlag {
            Class, // Class类型
            Param, // 带泛型参数
            Array, // 数组类型
            Variable, // 泛型变量
            Question,// 问号类型
        }

        public TypeFlag getFlag();

        public Class<?> getClazz();

        public List<IType> getParams();// 泛型参数

        public IType getComponent();// 数组成员

        public String getName();// 泛型变量名

        public int getBound();// 0：无边界 ，1：extends，2：super

        public Type getType();// 获取原始Type

        public IClass getIClass();
    }

    public interface Annotations {
        public Map<Class<? extends Annotation>, Annotation> getAnnotationsMap();

        public <A extends Annotation> A getAnnotation(Class<A> type);

        public IReflection getReflect();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    public @interface IReflection {
        public String name()

        default "";

        public boolean ignore()

        default false;

        public Class<? extends Getter> getter()

        default Getter.class;

        public Class<? extends Setter> setter()

        default Setter.class;

        public Class<? extends Creator> creator()

        default Creator.class;
    }

    public interface IClass extends Base {
        public Class<?> getClazz();

        public IType getSuper();

        public List<IType> getSupers();

        public List<IType> getSupersImplements();

        public Map<String, IField> getFieldMap();

        public Map<String, IField> getSupersMemberFieldMap();

        public Map<MethodInfo, IMethod> getPrimaryMethodMap();

        public Map<MethodInfo, IMethod> getSupersPrimaryMethodMap();

        public Map<String, IMethod> getMethodMap();

        public Map<String, IMethod> getSupersMethodMap();

        public Map<String, IClass> getClassMap();

        public IClass getArrayClass();

        public boolean isExtendsOf(Class<?> cls);

        public boolean isInstance(Object obj);

        public IClass getWrapperClass();

        public IClass getPrimitiveClass();

        public Map<CharSequence, Getter> getGetterMap();

        public Getter getGetter(CharSequence name);

        public Map<CharSequence, Setter> getSetterMap();

        public Setter getSetter(CharSequence name);

        public Creator getCreator();

        public void setAttribute(String key, Object value);

        public <T> T getAttribute(String key);

        public List<IType> getTypeParameters();

        public List<IType> getITypeParamMapper(Class<?> clazz);

        public List<IType> getITypeParamMapper(Class<?> clazz, List<IType> types);
    }
    public static abstract class Creator {
        public abstract Object newInstance(Object... args);

        public Object newInstance() {
            return newInstance(Lists.emptyObjectArray);
        }

        public Object newInstance(Object arg) {
            return newInstance(new Object[] {arg});
        }
    }
    public interface IGetter {
        public Object get(Object obj);
    }
    public interface ISetter {
        public void set(Object obj, Object value);
    }
    public interface Getter extends Base, IGetter {
    }
    public interface Setter extends Base, ISetter {
    }
    public interface IField extends Getter, Setter {
    }
    public interface IMethod extends Base {
        public Object invoke(Object obj, Object... args);

        public List<IType> getParamITypes();

        public List<IClass> getParamIClasses();

        public List<IClass> getParamWrapperIClasses();

        public MethodInfo getMethodInfo();
    }

    public static class MethodInfo {
        private String name;
        private Class<?>[] types;

        public MethodInfo(String name, Class<?>... types) {
            this.name = name;
            this.types = types;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ Arrays.hashCode(types);
        }

        @Override
        public boolean equals(Object o) {
            MethodInfo info;
            return o instanceof MethodInfo && (info = (MethodInfo) o).name.equals(name)
                    && Arrays.equals(types, info.types);
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append(name).append('(').appends(types, ",").append(')')
                    .toString();
        }
    }

    public static class ITypes {
        public static final IType Cint = forClass(int.class);
        public static final IType Clong = forClass(long.class);
        public static final IType Cchar = forClass(char.class);
        public static final IType Cshort = forClass(short.class);
        public static final IType Cboolean = forClass(boolean.class);
        public static final IType Cbyte = forClass(byte.class);
        public static final IType Cfloat = forClass(float.class);
        public static final IType Cdouble = forClass(double.class);
        public static final IType Cvoid = forClass(void.class);
        public static final IType CInteger = forClass(Integer.class);
        public static final IType CLong = forClass(Long.class);
        public static final IType CCharacter = forClass(Character.class);
        public static final IType CShort = forClass(Short.class);
        public static final IType CBoolean = forClass(Boolean.class);
        public static final IType CByte = forClass(Byte.class);
        public static final IType CFloat = forClass(Float.class);
        public static final IType CDouble = forClass(Double.class);
        public static final IType CVoid = forClass(Void.class);
        public static final IType CStrng = forClass(String.class);
        public static final IType CObject = forClass(Object.class);

        public static final IType VT = forVariable("T");
        public static final IType VS = forVariable("S");
        public static final IType VD = forVariable("D");
        public static final IType VK = forVariable("K");
        public static final IType VV = forVariable("V");
        public static final IType VE = forVariable("E");
        public static final IType VP = forVariable("P");
        public static final IType VQ = forVariable("Q");

        @SuppressWarnings("unchecked")
        public static final IType QSingle = forQuestion(0, Lists.emptyList);

        public static IType forClass(Class<?> clazz) {
            return new ITypeImpl(IType.TypeFlag.Class, clazz, null, null, null, 0);
        }

        public static IType forVariable(String name, int bound, List<IType> params) {
            return new ITypeImpl(IType.TypeFlag.Variable, Object.class, params, null, name, bound);
        }

        public static IType forVariable(String name) {
            return forVariable(name, 0, null);
        }

        public static IType forVariableExtends(String name, IType type) {
            return forVariable(name, 1, Arrays.asList(type));
        }

        public static IType forVariableSuper(String name, IType type) {
            return forVariable(name, 2, Arrays.asList(type));
        }

        public static IType forQuestion(int bound, List<IType> params) {
            return new ITypeImpl(IType.TypeFlag.Question, Object.class, params, null, null, bound);
        }

        public static IType forQuestion() {
            return QSingle;
        }

        public static IType forQuestionExtends(IType type) {
            return forQuestion(1, Arrays.asList(type));
        }

        public static IType forQuestionSuper(IType type) {
            return forQuestion(2, Arrays.asList(type));
        }

        public static IType forArray(IType component) {
            return new ITypeImpl(IType.TypeFlag.Array, Util.getArrayClass(component.getClazz(), 1),
                    null, component, null, 0);
        }

        public static IType forParam(Class<?> clazz, List<IType> params) {
            return new ITypeImpl(IType.TypeFlag.Param, clazz, params, null, null, 0);
        }

        public static IType forParam(Class<?> clazz, IType... params) {
            return forParam(clazz, Lists.from(params));
        }
    }
    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    public static class Util {
        public static Method getMethod(Class<?> cls, String name, Class<?>... params) {
            try {
                Method method = cls.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Method[] getMethods(Class<?> cls) {
            try {
                Method[] methods = cls.getDeclaredMethods();
                int len = methods.length;
                for (int i = 0; i < len; i++)
                    methods[i].setAccessible(true);
                return methods;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Constructor<?> getConstructor(Class<?> cls, Class<?>... params) {
            try {
                Constructor<?> con = cls.getDeclaredConstructor(params);
                con.setAccessible(true);
                return con;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Constructor<?>[] getConstructors(Class<?> cls) {
            try {
                Constructor<?>[] cons = cls.getDeclaredConstructors();
                int len = cons.length;
                for (int i = 0; i < len; i++)
                    cons[i].setAccessible(true);
                return cons;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Field getField(Class<?> cls, String name) {
            try {
                Field field = cls.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Field[] getFields(Class<?> cls) {
            try {
                Field[] fields = cls.getDeclaredFields();
                int len = fields.length;
                for (int i = 0; i < len; i++) {
                    fields[i].setAccessible(true);
                }
                return fields;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(cls.toString());
            }
        }

        public static Class<?>[] getClasses(Class<?> cls) {
            try {
                return cls.getDeclaredClasses();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static Class<?> getClass(String className) {
            try {
                return Class.forName(className, false,
                        Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(className);
            }
        }

        public static Class<?> getArrayClass(Class<?> cls, int n) {
            IClass cc = Reflection.forClass(cls);
            for (int i = 0; i < n; i++)
                cc = cc.getArrayClass();
            return cc.getIType().getClazz();
        }

        public static boolean isChildClass(Class<?> child, Class<?> parent) {
            return Reflection.forClass(child).isExtendsOf(parent);
        }

        public static InputStream getResource(Class<?> cls, String pack, String name) {
            try {
                URL url = cls.getProtectionDomain().getCodeSource().getLocation();
                String path = url.getPath();
                pack = pack.replace('.', '/');
                if (path.endsWith(".jar")) {
                    url = new URL("jar:file:" + path + "!/" + pack + "/" + name);
                } else {
                    url = new URL("file:" + path + "/" + pack + "/" + name);
                }
                return url.openStream();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static InputStream getResource(Class<?> cls, String name) {
            return getResource(cls, cls.getPackage().getName(), name);
        }

        public static InputStream getResource(String name) {
            Class<?> cls = getClass(Thread.currentThread().getStackTrace()[2].getClassName());
            return getResource(cls, name);
        }

        public static Object getValue(Object obj, IClass clazz, CharSequence name) {
            Getter gt = clazz.getGetter(name);
            return gt == null ? null : gt.get(obj);
        }

        public static void setValue(Object obj, Object value, IClass clazz, CharSequence name) {
            Setter st = clazz.getSetter(name);
            if (st != null)
                st.set(obj, Converters.convert(value, st.getIType().getClazz()));
        }

        public static final List<Class<?>> PrimitiveClasses =
                Lists.from(new Class<?>[] {int.class, long.class, char.class, short.class,
                        byte.class, boolean.class, float.class, double.class});

        public static final List<Class<?>> WrapperClasses =
                Lists.from(new Class<?>[] {Integer.class, Long.class, Character.class, Short.class,
                        Byte.class, Boolean.class, Float.class, Double.class});


        public static Class<?> toWrapperClass(Class<?> clazz) {
            return Lists.get(WrapperClasses, null,
                    Lists.indexOf(PrimitiveClasses, null, clazz, -1, -1), clazz);
        }

        public static Class<?> toPrimitiveClass(Class<?> clazz) {
            return Lists.get(PrimitiveClasses, null,
                    Lists.indexOf(WrapperClasses, null, clazz, -1, -1), clazz);
        }

        public static final HashSet<Class<?>> BaseTypes;
        static {
            BaseTypes = new HashSet<>();
            BaseTypes.addAll(PrimitiveClasses);
            BaseTypes.addAll(WrapperClasses);
        }

        public static boolean isBaseType(Class<?> clazz) {
            return BaseTypes.contains(clazz);
        }

        public static Filter<Integer> hasModifier(final int mod) {
            return new Filter<Integer>() {
                @Override
                public boolean accept(Integer value) {
                    return (value & mod) != 0;
                }
            };
        }

        public static <A extends Annotation> Filter<Base> hasAnatation(final Class<A> type) {
            return new Filter<Base>() {
                @Override
                public boolean accept(Base value) {
                    return value.getAnnotations().getAnnotation(type) != null;
                }
            };
        }

        public static final Filter<Integer> IsPublic = hasModifier(Modifier.PUBLIC);
        public static final Filter<Integer> IsProtected = hasModifier(Modifier.PROTECTED);
        public static final Filter<Integer> IsPrivate = hasModifier(Modifier.PRIVATE);
        public static final Filter<Integer> IsAbstract = hasModifier(Modifier.ABSTRACT);
        public static final Filter<Integer> IsStatic = hasModifier(Modifier.STATIC);
        public static final Filter<Integer> IsFinal = hasModifier(Modifier.FINAL);
        public static final Filter<Integer> IsTransient = hasModifier(Modifier.TRANSIENT);
        public static final Filter<Integer> IsVolatile = hasModifier(Modifier.VOLATILE);
        public static final Filter<Integer> IsSynchronized = hasModifier(Modifier.SYNCHRONIZED);
        public static final Filter<Integer> IsNative = hasModifier(Modifier.NATIVE);
        public static final Filter<Integer> IsStrictfp = hasModifier(Modifier.STRICT);
        public static final Filter<Integer> IsInterface = hasModifier(Modifier.INTERFACE);

        public static final Filter<Integer> IsPublicStatic =
                hasModifier(Modifier.PUBLIC | Modifier.STATIC);

        public static final Filter<Integer> IsPublicMember =
                Filters.and(IsPublic, Filters.not(IsStatic));

        private static final InvocationHandler DefaultInvocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.getDefaultValue();
            }
        };

        @SuppressWarnings("unchecked")
        public static <A extends Annotation> A getDefaultAnnotation(Class<A> type) {
            return (A) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                    DefaultInvocationHandler);
        }

        public static String getReflectionName(IMethod method) {
            String result = "";
            int pLen = method.getParamITypes().size();
            IReflection r = method.getAnnotations().getReflect();
            if (pLen == 0) {
                String name = checkName(method);
                if (r == null || r.name().isEmpty())
                    if (name.startsWith("get"))
                        result = getFieldName(name, 3);
                    else if (name.startsWith("is"))
                        result = getFieldName(name, 2);
            } else if (pLen == 1) {
                String name = checkName(method);
                if (r == null || r.name().isEmpty())
                    if (name.startsWith("set"))
                        result = getFieldName(name, 3);
            }
            return result;
        }

        public static String getReflectionName(IField field) {
            return checkName(field);
        }

        private static String checkName(Base base) {
            IReflection r = base.getAnnotations().getReflect();
            if (r != null && r.ignore()
                    || r == null && !Util.IsPublicMember.accept(base.getModifier()))
                return "";
            String name = r == null ? "" : r.name();
            return name.isEmpty() ? base.getName() : name;
        }

        private static String getFieldName(String name, int prefixLen) {
            int len = name.length() - prefixLen;
            if (len <= 0)
                return "";
            char[] chars = new char[len];
            name.getChars(prefixLen, name.length(), chars, 0);
            chars[0] = Strings.toLowCase(chars[0]);
            return new String(chars);
        }

        public static List<IMethod> getInnerMethods(IMethod method) {
            if (method instanceof MultiMethod)
                return ((MultiMethod) method).methods;
            return Lists.emptyList();
        }

        public static Method getRealMethod(IMethod method) {
            if (method instanceof ReflectMethod)
                return ((ReflectMethod) method).method;
            return null;
        }

        public static Object invokeDynamic(Converters.ConverterContext ctx, Object obj,
                String method, Object... args) {
            Reflection.IClass clazz = Reflection.forClass(obj.getClass());
            Reflection.IMethod imethod = clazz.getSupersMethodMap().get(method);
            if (imethod == null)
                throw new RuntimeException(String.format("The method is not exists：%s", method));
            List<Reflection.IMethod> methodList = Reflection.Util.getInnerMethods(imethod);
            int count = 0;
            for (Reflection.IMethod imet : methodList) {
                if (imet.getParamITypes().size() == args.length) {
                    imethod = imet;
                    count++;
                }
            }
            if (count != 1)
                throw new RuntimeException(String.format("Cannot call method with the same name：%s",
                        imethod.getName()));
            List<IType> types = imethod.getParamITypes();
            for (int i = 0; i < args.length; i++)
                args[i] = ctx.getConverter(types.get(i).getClazz()).convert(args[i]);
            return imethod.invoke(obj, args);
        }

        public static Object invokeDynamic(Object obj, String method, Object... args) {
            return invokeDynamic(Converters.DEFAULT_CONTEXT, obj, method, args);
        }

        public static List<String> getClassList(String packageName) {
            return getClassList(packageName, true);
        }

        public static List<String> getClassList(String packageName, boolean childPackage) {
            List<String> fileNames = null;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String packagePath = packageName.replace(".", "/");
            URL url = loader.getResource(packagePath);
            if (url != null) {
                String type = url.getProtocol();
                if (type.equals("file")) {
                    fileNames = getClassNameByFile(url.getPath(), packageName, childPackage);
                } else if (type.equals("jar")) {
                    fileNames = getClassNameByJar(url.getPath(), childPackage);
                }
            } else {
                fileNames = getClassNameByJars(((URLClassLoader) loader).getURLs(), packagePath,
                        childPackage);
            }
            return fileNames;
        }

        private static List<String> getClassNameByFile(String filePath, String packageName,
                boolean childPackage) {
            List<String> myClassName = new ArrayList<String>();
            File file = new File(filePath);
            File[] childFiles = file.listFiles();
            for (File childFile : childFiles) {
                if (childFile.isDirectory()) {
                    if (childPackage) {
                        myClassName.addAll(getClassNameByFile(childFile.getPath(),
                                packageName + "." + childFile.getName(), childPackage));
                    }
                } else {
                    String childFileName = childFile.getName();
                    if (childFileName.endsWith(".class")) {
                        childFileName = childFileName.substring(0, childFileName.indexOf('.'));
                        myClassName.add(packageName + "." + childFileName);
                    }
                }
            }
            return myClassName;
        }

        private static List<String> getClassNameByJar(String jarPath, boolean childPackage) {
            List<String> myClassName = new ArrayList<String>();
            String[] jarInfo = jarPath.split("!");
            String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
            String packagePath = jarInfo[1].substring(1);
            if (jarFilePath.endsWith(".jar"))
                try (JarFile jarFile = new JarFile(jarFilePath)) {
                    Enumeration<JarEntry> entrys = jarFile.entries();
                    while (entrys.hasMoreElements()) {
                        JarEntry jarEntry = entrys.nextElement();
                        String entryName = jarEntry.getName();
                        if (entryName.endsWith(".class")) {
                            if (childPackage) {
                                if (entryName.startsWith(packagePath)) {
                                    entryName = entryName.replace("/", ".").substring(0,
                                            entryName.lastIndexOf("."));
                                    myClassName.add(entryName);
                                }
                            } else {
                                int index = entryName.lastIndexOf("/");
                                String myPackagePath;
                                if (index != -1) {
                                    myPackagePath = entryName.substring(0, index);
                                } else {
                                    myPackagePath = entryName;
                                }
                                if (myPackagePath.equals(packagePath)) {
                                    entryName = entryName.replace("/", ".").substring(0,
                                            entryName.lastIndexOf("."));
                                    myClassName.add(entryName);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            return myClassName;
        }

        private static List<String> getClassNameByJars(URL[] urls, String packagePath,
                boolean childPackage) {
            List<String> myClassName = new ArrayList<String>();
            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    URL url = urls[i];
                    String urlPath = url.getPath();
                    if (urlPath.endsWith("classes/")) {
                        continue;
                    }
                    String jarPath = urlPath + "!/" + packagePath;
                    myClassName.addAll(getClassNameByJar(jarPath, childPackage));
                }
            }
            return myClassName;
        }

        public static Class<?> getClassByName(String name) {
            int an = 0;
            for (; name.charAt(an) == '['; an++);
            if (an != 0) {
                switch (name.charAt(an)) {
                    case 'L':
                        return getArrayClass(getClass(name.substring(an + 1, name.length() - 1)),
                                an);
                    case 'I':
                        return getArrayClass(int.class, an);
                    case 'J':
                        return getArrayClass(long.class, an);
                    case 'C':
                        return getArrayClass(char.class, an);
                    case 'S':
                        return getArrayClass(short.class, an);
                    case 'B':
                        return getArrayClass(byte.class, an);
                    case 'Z':
                        return getArrayClass(boolean.class, an);
                    case 'F':
                        return getArrayClass(float.class, an);
                    case 'D':
                        return getArrayClass(double.class, an);
                    default:
                        throw new IllegalArgumentException();
                }
            } else {
                switch (name) {
                    case "int":
                        return int.class;
                    case "long":
                        return long.class;
                    case "char":
                        return char.class;
                    case "short":
                        return short.class;
                    case "byte":
                        return byte.class;
                    case "boolean":
                        return boolean.class;
                    case "float":
                        return float.class;
                    case "double":
                        return double.class;
                    case "void":
                        return void.class;
                    default:
                        return getClass(name);
                }
            }
        }

        public static IType getComponent(IType type) {
            switch (type.getFlag()) {
                case Array:
                    return type.getComponent();
                default:
                    Reflection.IClass clazz = type.getIClass();
                    if (clazz.isExtendsOf(List.class)) {
                        switch (type.getFlag()) {
                            case Param:
                                List<IType> types =
                                        clazz.getITypeParamMapper(List.class, type.getParams());
                                return types.get(0);
                            default:
                                break;
                        }
                        return ITypes.CObject;
                    }
                    break;
            }
            return null;
        }

        public static <T> T getCachedAttribute(Reflection.IClass clazz, String key,
                Converter<T, Reflection.IClass> conv) {
            T result = clazz.getAttribute(key);
            if (result == null)
                clazz.setAttribute(key, result = conv.convert(clazz));
            return result;
        }

        public static <T> T getCachedAttribute(Class<?> clazz, String key,
                Converter<T, Reflection.IClass> conv) {
            return getCachedAttribute(Reflection.forClass(clazz), key, conv);
        }

        // 广度优先查找对象的属性，name和type可为空，level为深度
        @SuppressWarnings("unchecked")
        public static <T> T findAttribute(Object obj, String name, Class<T> type, int level) {
            List<Object> list1 = new ArrayList<>();
            List<Object> list2 = new ArrayList<>();
            list1.add(obj);
            for (int i = 0; i < level; i++) {
                for (Object o : list1) {
                    IClass clazz = Reflection.forClass(o.getClass());
                    for (Map.Entry<String, IField> e : clazz.getSupersMemberFieldMap().entrySet()) {
                        String key = e.getKey();
                        Object value = e.getValue().get(o);
                        if (value != null) {
                            if ((name == null || name.equals(key))
                                    && (type == null || type.isInstance(value)))
                                return (T) value;
                            if (!isBaseType(value.getClass()))
                                list2.add(value);
                        }
                    }
                }
                list1.clear();
                List<Object> tmp = list1;
                list1 = list2;
                list2 = tmp;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public static IType replaceVariable(IType type, List<IType> src, Object dst) {
            // dst ：List<IType> or IType
            switch (type.getFlag()) {
                case Array:
                    IType result = replaceVariable(type.getComponent(), src, dst);
                    if (result != type)
                        return ITypes.forArray(result);
                    break;
                case Param:
                    List<IType> newParams = replaceParamsVariable(type.getParams(), src, dst);
                    if (newParams != null)
                        return ITypes.forParam(type.getClazz(), newParams);
                    break;
                case Variable:
                    for (int i = 0; i < src.size(); i++) {
                        if (type.getName().equals(src.get(i).getName())) {
                            if (dst instanceof IType)
                                return (IType) dst;
                            else
                                return ((List<IType>) dst).get(i);
                        }
                    }
                    newParams = replaceParamsVariable(type.getParams(), src, dst);
                    if (newParams != null)
                        return ITypes.forVariable(type.getName(), type.getBound(), newParams);
                    break;
                case Question:
                    newParams = replaceParamsVariable(type.getParams(), src, dst);
                    if (newParams != null)
                        return ITypes.forQuestion(type.getBound(), newParams);
                    break;
                default:
                    break;
            }
            return type;
        }

        public static List<IType> replaceParamsVariable(List<IType> params, List<IType> src,
                Object dst) {
            if (params == null)
                return null;
            List<IType> newParams = null;
            for (int i = 0, len = params.size(); i < len; i++) {
                IType st = params.get(i);
                IType dt = replaceVariable(st, src, dst);
                if (st != dt) {
                    if (newParams == null)
                        newParams = new ArrayList<>(params);
                    newParams.set(i, dt);
                }
            }
            return newParams;
        }
    }

    static class ITypeImpl implements MyStringBuilder.AppendTo, IType {
        public static IType valueOf(Type type) {
            if (type instanceof Class)
                return IClassImpl.valueOf((Class<?>) type).getIType();
            return new ITypeImpl(type);
        }

        private TypeFlag flag;
        private Class<?> clazz;
        private List<IType> params;
        private IType component;
        private String name;
        private int bound;

        private Type type;
        private IClass iClass;

        private ITypeImpl(TypeFlag flag, Class<?> clazz, List<IType> params, IType component,
                String name, int bound) {
            this.flag = flag;
            this.clazz = clazz;
            this.params = params;
            this.component = component;
            this.name = name;
            this.bound = bound;
        }

        private ITypeImpl(Type type) {
            if (type instanceof Class) {
                Class<?> clz = (Class<?>) type;
                clazz = clz;
                if (clz.isArray()) {
                    flag = TypeFlag.Array;
                    component = ITypeImpl.valueOf(((Class<?>) type).getComponentType());
                } else {
                    flag = TypeFlag.Class;
                }
            } else if (type instanceof ParameterizedType) {
                flag = TypeFlag.Param;
                ParameterizedType pz = (ParameterizedType) type;
                clazz = (Class<?>) pz.getRawType();
                params = Lists.cache(
                        Lists.convert(Lists.from(pz.getActualTypeArguments()), ITypeImpl::valueOf));
            } else if (type instanceof TypeVariable) {
                flag = TypeFlag.Variable;
                TypeVariable<?> v = (TypeVariable<?>) type;
                clazz = Object.class;
                name = v.getName();
                Type[] bounds = v.getBounds();
                if (!(bounds.length == 1 && bounds[0] == Object.class)) {
                    bound = 1;
                    params = Lists.cache(Lists.convert(Lists.from(bounds), ITypeImpl::valueOf));
                }
            } else if (type instanceof WildcardType) {
                flag = TypeFlag.Question;
                WildcardType v = (WildcardType) type;
                clazz = Object.class;
                Type[] lb = v.getLowerBounds();
                Type[] ub = v.getUpperBounds();
                if (lb.length != 0) {
                    bound = 2;
                    params = Lists.cache(Lists.convert(Lists.from(lb), ITypeImpl::valueOf));
                } else if (!(ub.length == 1 && ub[0] == Object.class)) {
                    bound = 1;
                    params = Lists.cache(Lists.convert(Lists.from(ub), ITypeImpl::valueOf));
                }
            } else if (type instanceof GenericArrayType) {
                flag = TypeFlag.Array;
                GenericArrayType v = (GenericArrayType) type;
                component = ITypeImpl.valueOf(v.getGenericComponentType());
                clazz = Util.getArrayClass(component.getClazz(), 1);
            }
            this.type = type;
        }

        @Override
        public MyStringBuilder appendTo(MyStringBuilder sb) {
            switch (flag) {
                case Class:
                    sb.append(clazz.getName());
                    break;
                case Param:
                    sb.append(clazz.getName()).append('<').appends(params, ",").append('>');
                    break;
                case Array:
                    sb.append(component).append("[]");
                    break;
                case Variable:
                    sb.append(name);
                    if (bound == 1)
                        sb.append(" extends ").appends(params, "&");
                    else if (bound == 2)
                        sb.append(" super ").appends(params, "&");
                    break;
                case Question:
                    sb.append('?');
                    if (bound == 1)
                        sb.append(" extends ").appends(params, "&");
                    else if (bound == 2)
                        sb.append(" super ").appends(params, "&");
                    break;
            }
            return sb;
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append(this).toString();
        }

        @Override
        public TypeFlag getFlag() {
            return flag;
        }

        @Override
        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        public List<IType> getParams() {
            return params;
        }

        @Override
        public IType getComponent() {
            return component;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getBound() {
            return bound;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public synchronized IClass getIClass() {
            if (iClass == null)
                iClass = Reflection.forClass(clazz);
            return iClass;
        }
    }
    static class AnnotationsImpl implements Annotations {
        public static Annotations valueOf(Map<Class<? extends Annotation>, Annotation> annMap) {
            return new AnnotationsImpl(annMap);
        }

        private Map<Class<? extends Annotation>, Annotation> annMap;

        public AnnotationsImpl(Map<Class<? extends Annotation>, Annotation> annMap) {
            this.annMap = annMap;
        }

        @Override
        public Map<Class<? extends Annotation>, Annotation> getAnnotationsMap() {
            return annMap;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends Annotation> A getAnnotation(Class<A> type) {
            return (A) getAnnotationsMap().get(type);
        }

        static final IReflection DefaultIReflection = Util.getDefaultAnnotation(IReflection.class);

        private IReflection reflect = DefaultIReflection;

        @Override
        public synchronized IReflection getReflect() {
            if (reflect == DefaultIReflection)
                reflect = getAnnotation(IReflection.class);
            return reflect;
        }
    }
    static abstract class AbstractBase implements Base {
        protected abstract String getNameDirectly();

        protected abstract IType getReflectTypeDirectly();

        protected abstract int getModifierDirectly();

        protected abstract Annotation[] getAnnotationsDirectly();

        private String name;

        @Override
        public synchronized String getName() {
            if (name == null)
                name = getNameDirectly();
            return name;
        }

        private IClass clazz;

        @Override
        public synchronized IClass getIClass() {
            if (clazz == null)
                clazz = IClassImpl.valueOf(getIType().getClazz());
            return clazz;
        }

        private IType reflectType;

        @Override
        public synchronized IType getIType() {
            if (reflectType == null)
                reflectType = getReflectTypeDirectly();
            return reflectType;
        }

        private int modifier = -1;

        @Override
        public synchronized int getModifier() {
            if (modifier == -1)
                modifier = getModifierDirectly();
            return modifier;
        }

        private Annotations annotations;

        @Override
        public synchronized Annotations getAnnotations() {
            if (annotations == null)
                annotations = AnnotationsImpl.valueOf(Maps.groupByOne(
                        Lists.from(getAnnotationsDirectly()), Annotation::annotationType));
            return annotations;
        }

        private IClass declaringIClass;

        @Override
        public IClass getDeclaringIClass() {
            return declaringIClass;
        }

        public AbstractBase(IClass declaringIClass) {
            this.declaringIClass = declaringIClass;
        }
    }
    static class IClassImpl extends AbstractBase implements IClass {
        private static Map<Class<?>, IClassImpl> classCacheMap =
                new IdentityHashMap<Class<?>, IClassImpl>();

        public static synchronized IClassImpl valueOf(Class<?> cls) {
            IClassImpl clz = classCacheMap.get(cls);
            if (clz == null)
                classCacheMap.put(cls, clz = new IClassImpl(cls));
            return clz;
        }

        private static Map<String, IClassImpl> classCacheMapForName =
                new HashMap<String, IClassImpl>();

        public static synchronized IClassImpl valueOf(String name) {
            IClassImpl clz = classCacheMapForName.get(name);
            if (clz == null)
                classCacheMapForName.put(name, clz = IClassImpl.valueOf(Util.getClassByName(name)));
            return clz;
        }

        private IClassImpl(Class<?> clazz) {
            super(null);
            this.clazz = clazz;
        }

        @Override
        public String toString() {
            return "iclass " + clazz.getName();
        }

        private Class<?> clazz;

        @Override
        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        protected String getNameDirectly() {
            return clazz.getSimpleName();
        }

        @Override
        protected IType getReflectTypeDirectly() {
            return new ITypeImpl(clazz);
        }

        private List<IType> typeParameters;

        @Override
        public synchronized List<IType> getTypeParameters() {
            if (typeParameters == null)
                typeParameters =
                        Lists.cache(Lists.convert(clazz.getTypeParameters(), ITypeImpl::valueOf));
            return typeParameters;
        }

        @Override
        protected int getModifierDirectly() {
            return clazz.getModifiers();
        }

        @Override
        protected Annotation[] getAnnotationsDirectly() {
            return clazz.getDeclaredAnnotations();
        }

        private IType superIType;

        @Override
        public synchronized IType getSuper() {
            if (superIType == null) {
                Type sp = clazz.getGenericSuperclass();
                if (sp != null)
                    superIType = Reflection.forType(sp);
            }
            return superIType;
        }

        private List<IType> supers;

        @Override
        public synchronized List<IType> getSupers() {
            if (supers == null) {
                supers = new ArrayList<IType>();
                IType type = getIType();
                while (type != null) {
                    supers.add(type);
                    type = type.getIClass().getSuper();
                }
            }
            return supers;
        }

        private List<IType> supersImplements;

        @Override
        public synchronized List<IType> getSupersImplements() {
            if (supersImplements == null) {
                supersImplements = new ArrayList<IType>();
                if (getSuper() != null)
                    supersImplements.add(getSuper());
                supersImplements.addAll(Lists.convert(Lists.from(clazz.getGenericInterfaces()),
                        ITypeImpl::valueOf));
            }
            return supersImplements;
        }

        private Map<String, IField> fieldMap;

        @Override
        public synchronized Map<String, IField> getFieldMap() {
            if (fieldMap == null)
                fieldMap = Maps.groupByOne(
                        Lists.convert(Lists.from(Util.getFields(clazz)), IFieldImpl::valueOf),
                        Base::getName);
            return fieldMap;
        }

        private Map<String, IField> supersFieldMap;

        private synchronized Map<String, IField> getSupersFieldMap() {
            if (supersFieldMap == null)
                supersFieldMap = Maps.combine(Lists.convert(getSupers(),
                        Converters.link(IClass::getFieldMap, IType::getIClass)));
            return supersFieldMap;
        }

        private Map<String, IField> supersMemberFieldMap;

        @Override
        public synchronized Map<String, IField> getSupersMemberFieldMap() {
            if (supersMemberFieldMap == null)
                supersMemberFieldMap =
                        Maps.groupByOne(
                                Collections.filter(getSupersFieldMap().values(),
                                        b -> (b.getModifier() & Modifier.STATIC) == 0),
                                Base::getName);
            return supersMemberFieldMap;
        }

        private Map<MethodInfo, IMethod> primaryMethodMap;

        @Override
        public synchronized Map<MethodInfo, IMethod> getPrimaryMethodMap() {
            if (primaryMethodMap == null) {
                primaryMethodMap =
                        Maps.groupByOne(
                                Lists.combine(
                                        Lists.convert(Lists.from(Util.getMethods(clazz)),
                                                ReflectMethod::valueOf),
                                        Lists.convert(Lists.from(Util.getConstructors(clazz)),
                                                ConstructorMethod::valueOf)),
                                IMethod::getMethodInfo);
            }
            return primaryMethodMap;
        }

        private Map<String, IMethod> methodMap;

        @Override
        public synchronized Map<String, IMethod> getMethodMap() {
            if (methodMap == null)
                methodMap = Maps.groupByOne(Collections.convert(
                        Maps.groupBy(getPrimaryMethodMap().values(), Base::getName).values(),
                        MultiMethod::valueOf), Base::getName);
            return methodMap;
        }

        private Map<MethodInfo, IMethod> supersPrimaryMethodMap;

        @Override
        public synchronized Map<MethodInfo, IMethod> getSupersPrimaryMethodMap() {
            if (supersPrimaryMethodMap == null)
                supersPrimaryMethodMap = Maps.combine(
                        Lists.convert(getSupers(), cls -> cls.getIClass().getPrimaryMethodMap()));
            return supersPrimaryMethodMap;
        }

        private Map<String, IMethod> supersMethodMap;

        @Override
        public synchronized Map<String, IMethod> getSupersMethodMap() {
            if (supersMethodMap == null) {
                Collection<IMethod> methods = Maps
                        .groupByOne(getSupersPrimaryMethodMap().values(), IMethod::getMethodInfo)
                        .values();
                supersMethodMap = Maps.groupByOne(
                        Collections.convert(Maps.groupBy(methods, Base::getName).values(),
                                MultiMethod::valueOf),
                        Base::getName);
            }
            return supersMethodMap;
        }

        private Map<String, IClass> classMap;

        @Override
        public synchronized Map<String, IClass> getClassMap() {
            if (classMap == null)
                classMap = Maps.groupByOne(
                        Lists.convert(Lists.from(Util.getClasses(clazz)), IClassImpl::valueOf),
                        Base::getName);
            return classMap;
        }

        private IClass arrayClass;

        @Override
        public synchronized IClass getArrayClass() {
            if (arrayClass == null)
                arrayClass = Reflection.forClass(Array.newInstance(clazz, 0).getClass());
            return arrayClass;
        }

        private Map<Class<?>, List<IType>> extendsPathMap;

        @SuppressWarnings("unchecked")
        public Map<Class<?>, List<IType>> getExtendsPathMap() {
            if (extendsPathMap == null) {
                extendsPathMap = new HashMap<>();
                extendsPathMap.put(getClazz(), Lists.emptyList);
                for (IType t : getSupersImplements())
                    addExtendsPath(t, Lists.emptyList);
            }
            return extendsPathMap;
        }

        private void addExtendsPath(IType type, List<IType> path) {
            Class<?> cls = type.getClazz();
            List<IType> pt = extendsPathMap.get(cls);
            if (pt == null || path.size() < pt.size()) {
                path = new ArrayList<>(path);
                path.add(type);
                extendsPathMap.put(cls, path);
            }
            for (IType t : type.getIClass().getSupersImplements())
                addExtendsPath(t, path);
        }

        public List<IType> getITypeParamMapperWithoutCache(Class<?> clazz) {
            List<IType> path = getExtendsPathMap().get(clazz);
            if (path == null)
                throw new IllegalArgumentException();
            if (path.isEmpty())
                return getTypeParameters();
            int len = path.size();
            IType last = path.get(len - 1);
            switch (last.getFlag()) {
                case Param:
                    break;
                default:
                    List<IType> p = last.getIClass().getTypeParameters();
                    if (p.isEmpty())
                        return p;
                    return Lists.repeat(ITypes.CObject, p.size());
            }
            List<IType> result = new ArrayList<>(last.getParams());
            L0: for (int i = len - 2; i >= 0; i--) {
                IType type = path.get(i);
                List<IType> p1 = type.getIClass().getTypeParameters();// T T E
                List<IType> p2 = type.getParams();// F java.util.List<T> E
                switch (type.getFlag()) {
                    case Param:
                        for (int j = 0; j < result.size(); j++) {
                            result.set(j, Util.replaceVariable(result.get(j), p1, p2));
                        }
                        break;
                    default:
                        if (!p1.isEmpty()) {
                            for (int j = 0; j < result.size(); j++) {
                                result.set(j,
                                        Util.replaceVariable(result.get(j), p1, ITypes.CObject));
                            }
                        }
                        break L0;
                }
            }
            return result;
        }

        private Map<Class<?>, List<IType>> typeParamMapper;

        @Override
        public synchronized List<IType> getITypeParamMapper(Class<?> clazz) {
            if (typeParamMapper == null)
                typeParamMapper = new HashMap<>();
            List<IType> result = typeParamMapper.get(clazz);
            if (result == null)
                typeParamMapper.put(clazz, result = getITypeParamMapperWithoutCache(clazz));
            return result;
        }

        @Override
        public List<IType> getITypeParamMapper(Class<?> clazz, List<IType> types) {
            List<IType> mapper = getITypeParamMapper(clazz);
            List<IType> result = Util.replaceParamsVariable(mapper, getTypeParameters(), types);
            return result == null ? mapper : result;
        }

        @Override
        public boolean isExtendsOf(Class<?> cls) {
            return getExtendsPathMap().get(cls) != null;
        }

        @Override
        public boolean isInstance(Object obj) {
            return clazz.isInstance(obj);
        }

        private IClass wrapperClass;

        @Override
        public synchronized IClass getWrapperClass() {
            if (wrapperClass == null)
                wrapperClass = Reflection.forClass(Util.toWrapperClass(clazz));
            return wrapperClass;
        }

        private IClass primitiveClass;

        @Override
        public synchronized IClass getPrimitiveClass() {
            if (primitiveClass == null)
                primitiveClass = Reflection.forClass(Util.toPrimitiveClass(clazz));
            return primitiveClass;
        }

        private Maps.MyHashMap<CharSequence, Getter> getterMap;

        @Override
        public synchronized Map<CharSequence, Getter> getGetterMap() {
            if (getterMap == null) {
                getterMap = new Maps.MyHashMap<>();
                getterMap.setHashFunction(CharSequenceHashFunction);
                getterMap.setEqualsFunction(CharSequenceEqualsFunction);
                Maps.putAll(getterMap,
                        Collections.convert(
                                Collections.filter(getSupersFieldMap().values(),
                                        f -> !Util.getReflectionName(f).isEmpty()),
                                IFieldGetterAndSetter::valueOf),
                        Base::getName);
                Maps.putAll(getterMap,
                        Collections.convert(
                                Collections.filter(getSupersPrimaryMethodMap().values(),
                                        met -> met.getParamITypes().size() == 0
                                                && !Util.getReflectionName(met).isEmpty()
                                                && !met.getName().equals("getClass")),
                                IMethodGetter::valueOf),
                        Base::getName);
            }
            return getterMap;
        }

        @Override
        public Getter getGetter(CharSequence name) {
            return getGetterMap().get(name);
        }

        private Maps.MyHashMap<CharSequence, Setter> setterMap;

        @Override
        public synchronized Map<CharSequence, Setter> getSetterMap() {
            if (setterMap == null) {
                setterMap = new Maps.MyHashMap<>();
                setterMap.setHashFunction(CharSequenceHashFunction);
                setterMap.setEqualsFunction(CharSequenceEqualsFunction);
                Maps.putAll(setterMap,
                        Collections.convert(
                                Collections.filter(getSupersFieldMap().values(),
                                        f -> !Util.getReflectionName(f).isEmpty()),
                                IFieldGetterAndSetter::valueOf),
                        Base::getName);
                Maps.putAll(setterMap,
                        Collections.convert(
                                Collections.filter(getSupersPrimaryMethodMap().values(),
                                        met -> met.getParamITypes().size() == 1
                                                && !Util.getReflectionName(met).isEmpty()),
                                IMethodSetter::valueOf),
                        Base::getName);
            }
            return setterMap;
        }

        @Override
        public Setter getSetter(CharSequence name) {
            return getSetterMap().get(name);
        }

        private Creator creator;

        @Override
        public synchronized Creator getCreator() {
            if (creator == null) {
                creator = Creators.getDefault(clazz);
                if (creator == null) {
                    IReflection ref = getAnnotations().getReflect();
                    if (ref != null) {
                        Class<? extends Creator> ct = ref.creator();
                        if (ct != null)
                            creator = (Creator) IClassImpl.valueOf(ct).getCreator()
                                    .newInstance(Lists.emptyObjectArray);
                    }
                    if (creator == null)
                        creator = new DefaultCreator(this);
                }
            }
            return creator;
        }

        public static final Comparator<Base> CmpBaseName =
                Misc.convertComparator(Misc.STRING_COMPARATOR, Base::getName);

        static final Maps.HashFunction<CharSequence> CharSequenceHashFunction =
                new Maps.HashFunction<CharSequence>() {
                    @Override
                    public int hashCode(CharSequence value) {
                        int h = 0;
                        for (int i = 0, len = value.length(); i < len; i++) {
                            h = 31 * h + value.charAt(i);
                        }
                        return h;
                    }
                };
        static final Maps.EqualsFunction<CharSequence> CharSequenceEqualsFunction =
                new Maps.EqualsFunction<CharSequence>() {
                    @Override
                    public boolean equals(CharSequence o1, CharSequence o2) {
                        int n = o1.length();
                        if (n == o2.length()) {
                            int i = 0;
                            while (n-- != 0) {
                                if (o1.charAt(i) != o2.charAt(i))
                                    return false;
                                i++;
                            }
                            return true;
                        }
                        return false;
                    }
                };

        private Map<String, Object> attributeMap = new HashMap<>();

        @Override
        public void setAttribute(String key, Object value) {
            attributeMap.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getAttribute(String key) {
            return (T) attributeMap.get(key);
        }
    }
    static class IFieldImpl extends AbstractBase implements IField {
        public static IField valueOf(Field field) {
            return new IFieldImpl(field);
        }

        private Field field;

        public IFieldImpl(Field field) {
            super(IClassImpl.valueOf(field.getDeclaringClass()));
            this.field = field;
        }

        @Override
        protected String getNameDirectly() {
            return field.getName();
        }

        @Override
        protected IType getReflectTypeDirectly() {
            return ITypeImpl.valueOf(field.getGenericType());
        }

        @Override
        protected int getModifierDirectly() {
            return field.getModifiers();
        }

        @Override
        protected Annotation[] getAnnotationsDirectly() {
            return field.getDeclaredAnnotations();
        }

        @Override
        public Object get(Object obj) {
            try {
                return field.get(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Object obj, Object value) {
            try {
                field.set(obj, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append(getIType()).append(' ').append(getName())
                    .toString();
        }
    }
    static abstract class AbstractIMethod extends AbstractBase implements IMethod {
        public AbstractIMethod(IClass declaringIClass) {
            super(declaringIClass);
        }

        public abstract Object invoke(Object obj, Object... args);

        protected abstract List<IType> getParamITypesDirectly();

        private List<IType> paramITypes;

        @Override
        public synchronized List<IType> getParamITypes() {
            if (paramITypes == null)
                paramITypes = getParamITypesDirectly();
            return paramITypes;
        }

        private MethodInfo methodInfo;

        @Override
        public MethodInfo getMethodInfo() {
            if (methodInfo == null)
                methodInfo = new MethodInfo(getName(), (Class<?>[]) Lists
                        .toArray(Lists.convert(getParamITypes(), IType::getClazz), Class.class));
            return methodInfo;
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append(getIType()).append(' ').append(getName())
                    .append('(').appends(getParamITypes(), ",").append(')').toString();
        }
    }
    static class ReflectMethod extends AbstractIMethod {
        public static IMethod valueOf(Method method) {
            return new ReflectMethod(method);
        }

        private Method method;

        public ReflectMethod(Method method) {
            super(IClassImpl.valueOf(method.getDeclaringClass()));
            this.method = method;
        }

        @Override
        protected String getNameDirectly() {
            return method.getName();
        }

        @Override
        protected IType getReflectTypeDirectly() {
            return ITypeImpl.valueOf(method.getGenericReturnType());
        }

        @Override
        protected int getModifierDirectly() {
            return method.getModifiers();
        }

        @Override
        protected Annotation[] getAnnotationsDirectly() {
            return method.getDeclaredAnnotations();
        }

        @Override
        protected List<IType> getParamITypesDirectly() {
            return Lists.cache(Lists.convert(Lists.from(method.getGenericParameterTypes()),
                    ITypeImpl::valueOf));
        }

        @Override
        public Object invoke(Object obj, Object... args) {
            try {
                return method.invoke(obj, args);
            } catch (Exception e) {
                throw new RuntimeException(null,
                        e instanceof InvocationTargetException
                                ? ((InvocationTargetException) e).getTargetException()
                                : e);
            }
        }

        @Override
        public List<IClass> getParamIClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<IClass> getParamWrapperIClasses() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    static class ConstructorMethod extends AbstractIMethod {
        public static IMethod valueOf(Constructor<?> constructor) {
            return new ConstructorMethod(constructor);
        }

        private Constructor<?> constructor;

        public ConstructorMethod(Constructor<?> constructor) {
            super(IClassImpl.valueOf(constructor.getDeclaringClass()));
            this.constructor = constructor;
        }

        @Override
        protected String getNameDirectly() {
            return constructor.getDeclaringClass().getSimpleName();
        }

        @Override
        protected IType getReflectTypeDirectly() {
            return IClassImpl.valueOf(constructor.getDeclaringClass()).getIType();
        }

        @Override
        protected int getModifierDirectly() {
            return constructor.getModifiers();
        }

        @Override
        protected Annotation[] getAnnotationsDirectly() {
            return constructor.getDeclaredAnnotations();
        }

        @Override
        protected List<IType> getParamITypesDirectly() {
            return Lists.cache(Lists.convert(Lists.from(constructor.getGenericParameterTypes()),
                    ITypeImpl::valueOf));
        }

        @Override
        public Object invoke(Object obj, Object... args) {
            try {
                return constructor.newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException(null,
                        e instanceof InvocationTargetException
                                ? ((InvocationTargetException) e).getTargetException()
                                : e);
            }
        }

        @Override
        public List<IClass> getParamIClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<IClass> getParamWrapperIClasses() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    static class MultiMethod extends AbstractIMethod {
        public static IMethod valueOf(List<IMethod> methods) {
            return new MultiMethod(methods);
        }

        private List<IMethod> methods;

        public MultiMethod(List<IMethod> methods) {
            super(null);
            this.methods = methods;
            Lists.insertSort(methods, null, CmpMethodByParam, -1, -1);
        }

        @Override
        public Object invoke(Object obj, Object... args) {
            IMethod met = getMethod(args);
            if (met == null)
                throw new IllegalArgumentException(
                        "Method does not exist or parameter does not match:" + getName());
            return met.invoke(obj, args);
        }

        @Override
        protected List<IType> getParamITypesDirectly() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String getNameDirectly() {
            return methods.get(0).getName();
        }

        @Override
        protected IType getReflectTypeDirectly() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int getModifierDirectly() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Annotation[] getAnnotationsDirectly() {
            throw new UnsupportedOperationException();
        }

        public List<IMethod> getMethods() {
            return methods;
        }

        public IMethod getMethod(Class<?>... types) {
            return Lists.get(methods, null,
                    Lists.lastIndexOfFilter(methods, null, new ClassParamMacher(types), -1, -1),
                    null);
        }

        public IMethod getMethod(Object... params) {
            return Lists.get(methods, null,
                    Lists.lastIndexOfFilter(methods, null, new ObjectParamMacher(params), -1, -1),
                    null);
        }

        @Override
        public String toString() {
            return getName() + "(unknown)";
        }

        public static class ObjectParamMacher implements Filter<IMethod> {
            private Object[] params;

            public ObjectParamMacher(Object[] params) {
                this.params = params;
            }

            @Override
            public boolean accept(IMethod m) {
                List<IType> pt = m.getParamITypes();
                int len1 = params.length, len2 = pt.size();
                if (len1 != len2)
                    return false;
                for (int i = 0; i < len1; i++) {
                    Object o = params[i];
                    if (o != null && !pt.get(i).getIClass().getWrapperClass().isInstance(o))
                        return false;
                }
                return true;
            }
        }

        public static class ClassParamMacher implements Filter<IMethod> {
            private Class<?>[] params;

            public ClassParamMacher(Class<?>[] params) {
                this.params = params;
            }

            @Override
            public boolean accept(IMethod m) {
                List<IType> pt = m.getParamITypes();
                int len1 = params.length, len2 = pt.size();
                if (len1 != len2)
                    return false;
                for (int i = 0; i < len1; i++) {
                    if (!Reflection.forClass(params[i])
                            .isExtendsOf(pt.get(i).getIClass().getWrapperClass().getClazz()))
                        return false;
                }
                return true;
            }
        }

        public static final Comparator<IMethod> CmpMethodByParam = new Comparator<IMethod>() {
            @Override
            public int compare(IMethod m1, IMethod m2) {
                List<IType> t1 = m1.getParamITypes();
                List<IType> t2 = m2.getParamITypes();
                int len1 = t1.size(), len2 = t2.size();
                if (len1 != len2)
                    return len1 - len2;
                for (int i = 0; i < len1; i++) {
                    if (t1.get(i).getIClass().getWrapperClass()
                            .isExtendsOf(t2.get(i).getIClass().getWrapperClass().getClazz()))
                        return -1;
                }
                return 1;
            }
        };

        @Override
        public List<IClass> getParamIClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<IClass> getParamWrapperIClasses() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    static class FilterBase implements Base {
        private Base base;
        private String name;

        public FilterBase(Base base, String name) {
            this.base = base;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IClass getIClass() {
            return base.getIClass();
        }

        @Override
        public IType getIType() {
            return base.getIType();
        }

        @Override
        public int getModifier() {
            return base.getModifier();
        }

        @Override
        public Annotations getAnnotations() {
            return base.getAnnotations();
        }

        @Override
        public IClass getDeclaringIClass() {
            return base.getDeclaringIClass();
        }
    }
    static class IFieldGetterAndSetter extends FilterBase implements Getter, Setter {
        public static IFieldGetterAndSetter valueOf(IField field) {
            return new IFieldGetterAndSetter(field);
        }

        private IField field;

        public IFieldGetterAndSetter(IField field) {
            super(field, Util.getReflectionName(field));
            this.field = field;
        }

        @Override
        public Object get(Object obj) {
            return field.get(obj);
        }

        @Override
        public void set(Object obj, Object value) {
            field.set(obj, value);
        }

        @Override
        public String toString() {
            return field.toString();
        }
    }
    static class IMethodGetter extends FilterBase implements Getter {
        public static Getter valueOf(IMethod method) {
            return new IMethodGetter(method);
        }

        private IMethod method;

        public IMethodGetter(IMethod method) {
            super(method, Util.getReflectionName(method));
            this.method = method;
        }

        @Override
        public Object get(Object obj) {
            return method.invoke(obj);
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }
    static class IMethodSetter extends FilterBase implements Setter {
        public static Setter valueOf(IMethod method) {
            return new IMethodSetter(method);
        }

        private IMethod method;
        private IClass clazz;
        private IType type;

        public IMethodSetter(IMethod method) {
            super(method, Util.getReflectionName(method));
            this.method = method;
            this.clazz = method.getParamITypes().get(0).getIClass();
            this.type = method.getParamITypes().get(0);
        }

        @Override
        public void set(Object obj, Object value) {
            method.invoke(obj, value);
        }

        @Override
        public IClass getIClass() {
            return clazz;
        }

        @Override
        public IType getIType() {
            return type;
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }
    public static class DefaultCreator extends Creator {
        private IClass clazz;
        private IMethod cons;

        public DefaultCreator(IClass clazz) {
            this.clazz = clazz;
        }

        public DefaultCreator(Class<?> clazz) {
            this(IClassImpl.valueOf(clazz));
        }

        @Override
        public synchronized Object newInstance(Object... args) {
            if (cons == null) {
                cons = clazz.getMethodMap().get(clazz.getName());
                if (cons == null)
                    throw new RuntimeException();
            }
            return cons.invoke(null, args);
        }
    }
    public static class Creators {
        static final Map<Class<?>, Creator> creatorMap = new HashMap<>();

        public static Creator getDefault(Class<?> clazz) {
            return creatorMap.get(clazz);
        }

        static final Creator CreateArrayList = new DefaultCreator(ArrayList.class) {
            @Override
            public Object newInstance() {
                return new ArrayList<>();
            }
        };
        static final Creator CreateHashMap = new DefaultCreator(HashMap.class) {
            @Override
            public Object newInstance() {
                return new HashMap<>();
            }
        };
        static final Creator CreateHashSet = new DefaultCreator(HashSet.class) {
            @Override
            public Object newInstance() {
                return new HashSet<>();
            }
        };
        static final Creator CreateClass = new DefaultCreator(Class.class) {
            @Override
            public Object newInstance(Object arg) {
                if (arg instanceof String)
                    return Util.getClass((String) arg);
                return super.newInstance(arg);
            }
        };
        static final Creator CreateCalendar = new DefaultCreator(Calendar.class) {
            @Override
            public Object newInstance(Object arg) {
                if (arg instanceof Long) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis((long) arg);
                    return cal;
                }
                return super.newInstance(arg);
            }
        };
        static final Creator CreatePattern = new DefaultCreator(Pattern.class) {
            @Override
            public Object newInstance(Object arg) {
                if (arg instanceof String) {
                    return Pattern.compile((String) arg);
                }
                return super.newInstance(arg);
            }
        };
        static final Creator CreateCharset = new DefaultCreator(Charset.class) {
            @Override
            public Object newInstance(Object arg) {
                if (arg instanceof String) {
                    return Charset.forName((String) arg);
                }
                return super.newInstance(arg);
            }
        };
        static {
            creatorMap.put(Iterable.class, CreateArrayList);
            creatorMap.put(Collection.class, CreateArrayList);
            creatorMap.put(List.class, CreateArrayList);
            creatorMap.put(ArrayList.class, CreateArrayList);
            creatorMap.put(Map.class, CreateHashMap);
            creatorMap.put(HashMap.class, CreateHashMap);
            creatorMap.put(Set.class, CreateHashSet);
            creatorMap.put(HashSet.class, CreateHashSet);
            creatorMap.put(Class.class, CreateClass);
            creatorMap.put(Calendar.class, CreateCalendar);
            creatorMap.put(Pattern.class, CreatePattern);
            creatorMap.put(Charset.class, CreateCharset);
        }
    }
}
