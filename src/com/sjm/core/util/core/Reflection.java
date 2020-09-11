package com.sjm.core.util.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * 反射缓存工具类
 */
public class Reflection {
    public static final Reflection INSTANCE = new Reflection(Cache.newInstance());

    private Cache cache;

    public Reflection(Cache cache) {
        this.cache = cache;
    }

    public Class<?>[] getSupers(Class<?> clazz) {
        return cache.get2_2_1(Util::getSupers, "getSupers", clazz);
    }

    public Class<?>[] getInterfaces(Class<?> clazz) {
        return cache.get2_2_1(Class::getInterfaces, "getInterfaces", clazz);
    }

    public Type[] getGenericInterfaces(Class<?> clazz) {
        return cache.get2_2_1(Class::getGenericInterfaces, "getGenericInterfaces", clazz);
    }

    public Class<?>[] getSupersAndInterfaces(Class<?> clazz) {
        return cache.get3_2_2(Impl::getSupersAndInterfaces, "getSupersAndInterfaces", clazz, this);
    }

    public Method[] getMethods(Class<?> clazz) {
        return cache.get2_2_1(Class::getMethods, "getMethods", clazz);
    }

    public Map<String, Method[]> getMethodsMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getMethodsMap, "getMethodsMap", clazz, this);
    }

    public Method[] getDeclaredMethods(Class<?> clazz) {
        return cache.get2_2_1(Util::getDeclaredMethods, "getDeclaredMethods", clazz);
    }

    public Map<String, Method[]> getDeclaredMethodsMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getDeclaredMethodsMap, "getDeclaredMethodsMap", clazz, this);
    }

    public Method findMethod(Method[] methods, Class<?>... params) {
        return Impl.findMethod(methods, params, this);
    }

    public Method dynamicFindMethod(Method[] methods, Object[] args) {
        return Impl.dynamicFindMethod(methods, args, this);
    }

    public Class<?>[] getParameterTypes(Method method) {
        return cache.get2_2_1(Method::getParameterTypes, "getParameterTypes", method);
    }

    public Type[] getGenericParameterTypes(Method method) {
        return cache.get2_2_1(Method::getGenericParameterTypes, "getGenericParameterTypes", method);
    }

    public Field[] getFields(Class<?> clazz) {
        return cache.get2_2_1(Class::getFields, "getFields", clazz);
    }

    public Map<String, Field[]> getFieldsMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getFieldsMap, "getFieldsMap", clazz, this);
    }

    public Field[] getDeclaredFields(Class<?> clazz) {
        return cache.get2_2_1(Util::getDeclaredFields, "getDeclaredFields", clazz);
    }

    public Map<String, Field> getDeclaredFieldsMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getDeclaredFieldsMap, "getDeclaredFieldsMap", clazz, this);
    }

    public Class<?> getClassByName(String name) {
        return cache.get3_2_2(Impl::getClassByName, "getClassByName", name, this);
    }

    public Class<?> getArrayClass(Class<?> clazz, int n) {
        return cache.get4_3_3(Impl::getArrayClass, "getArrayClass", clazz, n, this);
    }

    public Class<?> getRawType(Type type) {
        return cache.get3_2_2(Impl::getRawType, "getRawType", type, this);
    }

    public <T> TypeVariable<Class<T>>[] getTypeParameters(Class<T> clazz) {
        return cache.get2_2_1(Class::getTypeParameters, "getTypeParameters", clazz);
    }

    public Class<?>[] getExtendsPath(Class<?> clazz, Class<?> superClass) {
        return cache.get4_3_3(Impl::getExtendsPath, "getExtendsPath", clazz, superClass, this);
    }

    public Type getGenericParent(Class<?> clazz, Class<?> parent) {
        return cache.get4_3_3(Impl::getGenericParent, "getGenericParent", clazz, parent, this);
    }

    public Type[] getTypeMapping(Class<?> clazz, Class<?> superClass) {
        return cache.get4_3_3(Impl::getTypeMapping, "getTypeMapping", clazz, superClass, this);
    }

    public Type[] getGenericTypeMapping(Type type, Class<?> superClass) {
        return cache.get4_3_3(Impl::getGenericTypeMapping, "getGenericTypeMapping", type,
                superClass, this);
    }

    public Map<String, GetterInfo> getGettersMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getGettersMap, "getGettersMap", clazz, this);
    }

    public Map<String, SetterInfo> getSettersMap(Class<?> clazz) {
        return cache.get3_2_2(Impl::getSettersMap, "getSettersMap", clazz, this);
    }

    public Type calculateGenericType(Type type, Class<?> superClass, Type calculateType) {
        return Reflection.Util.replaceVariable(calculateType, getTypeParameters(superClass),
                getGenericTypeMapping(type, superClass));
    }

    public Cache cache() {
        return cache;
    }

    public interface Getter {
        public Object get(Object target) throws Exception;
    }

    public interface Setter {
        public void set(Object target, Object value) throws Exception;
    }

    public static class GetterInfo {
        public Getter getter;
        public String name;
        public Type type;
        public Member member;

        public GetterInfo(Getter getter, String name, Type type, Member member) {
            this.getter = getter;
            this.name = name;
            this.type = type;
            this.member = member;
        }
    }

    public static class SetterInfo {
        public Setter setter;
        public String name;
        public Type type;
        public Member member;

        public SetterInfo(Setter setter, String name, Type type, Member member) {
            this.setter = setter;
            this.name = name;
            this.type = type;
            this.member = member;
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Private Classes >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    static class Impl {
        public static Class<?>[] getSupersAndInterfaces(Class<?> clazz, Reflection r) {
            Set<Class<?>> set = new HashSet<>();
            Class<?>[] supers = r.getSupers(clazz);
            for (Class<?> sup : supers) {
                set.add(sup);
                addInterfaces(sup, set, r);
            }
            return set.toArray(new Class<?>[set.size()]);
        }

        private static void addInterfaces(Class<?> clazz, Set<Class<?>> set, Reflection r) {
            if (set.add(clazz)) {
                Class<?>[] supers = r.getInterfaces(clazz);
                for (Class<?> sup : supers) {
                    addInterfaces(sup, set, r);
                }
            }
        }

        public static Map<String, Method[]> getMethodsMap(Class<?> clazz, Reflection r) {
            return Util.groupby(r.getMethods(clazz), Method::getName, Method[]::new);
        }

        public static Map<String, Method[]> getDeclaredMethodsMap(Class<?> clazz, Reflection r) {
            return Util.groupby(r.getDeclaredMethods(clazz), Method::getName, Method[]::new);
        }

        public static Method findMethod(Method[] methods, Class<?>[] params, Reflection r) {
            if (methods != null)
                for (Method method : methods)
                    if (Arrays.equals(params, r.getParameterTypes(method)))
                        return method;
            return null;
        }

        public static Method dynamicFindMethod(Method[] methods, Object[] args, Reflection r) {
            if (methods != null)
                for (Method method : methods)
                    if (matchArgs(r.getParameterTypes(method), args))
                        return method;
            return null;
        }

        private static boolean matchArgs(Class<?>[] type, Object[] args) {
            if (type.length != args.length)
                return false;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null && !type[i].isInstance(arg)) {
                    return false;
                }
            }
            return true;
        }

        public static Map<String, Field[]> getFieldsMap(Class<?> clazz, Reflection r) {
            return Util.groupby(r.getFields(clazz), Field::getName, Field[]::new);
        }

        public static Map<String, Field> getDeclaredFieldsMap(Class<?> clazz, Reflection r) {
            return Util.groupbyOne(r.getDeclaredFields(clazz), Field::getName);
        }

        public static Class<?> getArrayClass(Class<?> clazz, int n, Reflection r) {
            while (clazz.isArray()) {
                clazz = clazz.getComponentType();
                n++;
            }
            if (n == 0)
                return clazz;
            else if (n == 1) {
                return Array.newInstance(clazz, 0).getClass();
            } else {
                return Array.newInstance(r.getArrayClass(clazz, n - 1), 0).getClass();
            }
        }

        public static Class<?> getClassByName(String name, Reflection r) {
            int i = 0;
            for (; name.charAt(i) == '['; i++);
            if (i != 0) {
                switch (name.charAt(i)) {
                    case 'L':
                        return r.getArrayClass(
                                Util.getClass(name.substring(i + 1, name.length() - 1)), i);
                    case 'I':
                        return r.getArrayClass(int.class, i);
                    case 'J':
                        return r.getArrayClass(long.class, i);
                    case 'C':
                        return r.getArrayClass(char.class, i);
                    case 'S':
                        return r.getArrayClass(short.class, i);
                    case 'B':
                        return r.getArrayClass(byte.class, i);
                    case 'Z':
                        return r.getArrayClass(boolean.class, i);
                    case 'F':
                        return r.getArrayClass(float.class, i);
                    case 'D':
                        return r.getArrayClass(double.class, i);
                    default:
                        throw new IllegalArgumentException(name);
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
                        return Util.getClass(name);
                }
            }
        }

        public static Class<?> getRawType(Type type, Reflection r) {
            if (type instanceof Class) {
                return (Class<?>) type;
            } else if (type instanceof GenericArrayType) {
                GenericArrayType gat = (GenericArrayType) type;
                return r.getArrayClass(r.getRawType(gat.getGenericComponentType()), 1);
            } else if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            } else if (type instanceof TypeVariable || type instanceof WildcardType) {
                return Object.class;
            }
            throw new UnsupportedOperationException();
        }

        public static Class<?>[] getExtendsPath(Class<?> clazz, Class<?> superClass, Reflection r) {
            if (!superClass.isAssignableFrom(clazz))
                throw new RuntimeException();
            Class<?>[] supers = r.getSupers(clazz);
            if (superClass.isInterface()) {
                List<Class<?>> classes = new ArrayList<>();
                L0: for (Class<?> su : supers) {
                    classes.add(su);
                    for (Class<?> tmpInter : r.getInterfaces(su)) {
                        if (searchAndAddInterfaces(tmpInter, superClass, classes, r)) {
                            break L0;
                        }
                    }
                }
                return classes.toArray(new Class<?>[classes.size()]);
            } else {
                return Arrays.copyOfRange(supers, 0, Util.indexOfIdentity(supers, superClass) + 1);
            }
        }

        private static boolean searchAndAddInterfaces(Class<?> inter, Class<?> target,
                List<Class<?>> classes, Reflection r) {
            if (target.isAssignableFrom(inter)) {
                classes.add(inter);
                if (inter == target)
                    return true;
                for (Class<?> tmpInter : r.getInterfaces(inter)) {
                    if (searchAndAddInterfaces(tmpInter, target, classes, r))
                        return true;
                }
            }
            return false;
        }

        public static Type getGenericParent(Class<?> clazz, Class<?> parent, Reflection r) {
            if (clazz.getSuperclass() == parent)
                return clazz.getGenericSuperclass();
            Class<?>[] interfaces = r.getInterfaces(clazz);
            Type[] genericInterfaces = r.getGenericInterfaces(clazz);
            for (int i = 0; i < interfaces.length; i++)
                if (interfaces[i] == parent)
                    return genericInterfaces[i];
            throw new RuntimeException();
        }

        public static Type[] getTypeMapping(Class<?> clazz, Class<?> superClass, Reflection r) {
            Class<?>[] path = r.getExtendsPath(clazz, superClass);
            TypeVariable<?>[] types = r.getTypeParameters(superClass);
            if (path.length < 2)
                return types;
            if (types.length == 0)
                return new Type[0];
            Object tmpResult = getReplaceTarget(
                    r.getGenericParent(path[path.length - 2], path[path.length - 1]));
            if (tmpResult == Object.class) {
                Type[] result = new Type[types.length];
                Arrays.fill(result, Object.class);
                return result;
            }
            Type[] result = (Type[]) tmpResult;
            for (int i = path.length - 3; i >= 0; i--) {
                Object dst = getReplaceTarget(r.getGenericParent(path[i], path[i + 1]));
                TypeVariable<?>[] src = r.getTypeParameters(path[i + 1]);
                Type[] tmp = Util.replaceParamsVariable(result, src, dst);
                if (tmp == null)
                    break;
                result = tmp;
            }
            return result;
        }

        private static Object getReplaceTarget(Type type) {
            if (type instanceof ParameterizedType) {
                return ((ParameterizedType) type).getActualTypeArguments();
            } else if (type instanceof Class) {
                return Object.class;
            } else
                throw new UnsupportedOperationException();
        }

        public static Type[] getGenericTypeMapping(Type type, Class<?> superClass, Reflection r) {
            Object dst = getReplaceTarget(type);
            Class<?> clazz = r.getRawType(type);
            Type[] mapping = r.getTypeMapping(clazz, superClass);
            Type[] tmp = Util.replaceParamsVariable(mapping, r.getTypeParameters(clazz), dst);
            return tmp != null ? tmp : mapping;
        }

        public static Map<String, GetterInfo> getGettersMap(Class<?> clazz, Reflection r) {
            Method[] methods = r.getMethods(clazz);
            Map<String, GetterInfo> result = new HashMap<>();
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers))
                    continue;
                String methodName = method.getName();
                if (methodName.length() <= 3 || !methodName.startsWith("get")
                        || methodName.equals("getClass"))
                    continue;
                Class<?>[] paramTypes = r.getParameterTypes(method);
                if (paramTypes.length != 0)
                    continue;
                String name = getMethodGetterOrSetterName(methodName);
                if (result.containsKey(name))
                    continue;
                result.put(name, new GetterInfo(target -> method.invoke(target), name,
                        method.getGenericReturnType(), method));
            }
            Field[] fields = r.getFields(clazz);
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers))
                    continue;
                String name = field.getName();
                if (result.containsKey(name))
                    continue;
                result.put(name, new GetterInfo(target -> field.get(target), name,
                        field.getGenericType(), field));
            }
            return result;
        }

        public static Map<String, SetterInfo> getSettersMap(Class<?> clazz, Reflection r) {
            Method[] methods = r.getMethods(clazz);
            Map<String, SetterInfo> result = new HashMap<>();
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers))
                    continue;
                String methodName = method.getName();
                if (methodName.length() <= 3 || !methodName.startsWith("set"))
                    continue;
                Type[] paramTypes = r.getGenericParameterTypes(method);
                if (paramTypes.length != 1)
                    continue;
                String name = getMethodGetterOrSetterName(methodName);
                if (result.containsKey(name))
                    continue;
                result.put(name, new SetterInfo((target, value) -> method.invoke(target, value),
                        name, paramTypes[0], method));
            }
            Field[] fields = r.getFields(clazz);
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers))
                    continue;
                String name = field.getName();
                if (result.containsKey(name))
                    continue;
                result.put(name, new SetterInfo((target, value) -> field.set(target, value), name,
                        field.getGenericType(), field));
            }
            return result;
        }

        private static String getMethodGetterOrSetterName(String methodName) {
            char[] buf = new char[methodName.length() - 3];
            buf[0] = Character.toLowerCase(methodName.charAt(3));
            methodName.getChars(4, methodName.length(), buf, 1);
            return new String(buf);
        }
    }

    public static class Util {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static <K, T> Map<K, T[]> groupby(T[] arr, Function<T, K> keyMapper,
                IntFunction<T[]> arrayAllocator) {
            Map<K, Object> map = new HashMap<>();
            for (T value : arr) {
                K key = keyMapper.apply(value);
                List<T> list = (List<T>) map.get(key);
                if (list == null)
                    map.put(key, list = new ArrayList<>());
                list.add(value);
            }
            for (Map.Entry<K, Object> e : map.entrySet()) {
                List<T> list = (List<T>) e.getValue();
                e.setValue(list.toArray(arrayAllocator.apply(list.size())));
            }
            return (Map) map;
        }

        public static <K, T> Map<K, T> groupbyOne(T[] arr, Function<T, K> keyMapper) {
            Map<K, T> map = new HashMap<>();
            for (T value : arr)
                map.put(keyMapper.apply(value), value);
            return map;
        }

        public static Class<?>[] getSupers(Class<?> clazz) {
            List<Class<?>> list = new ArrayList<>();
            do {
                list.add(clazz);
                clazz = clazz.getSuperclass();
            } while (clazz != null);
            return list.toArray(new Class<?>[list.size()]);
        }

        public static Method[] getDeclaredMethods(Class<?> clazz) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods)
                method.setAccessible(true);
            return methods;
        }

        public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
            Constructor<?>[] cons = clazz.getDeclaredConstructors();
            for (Constructor<?> con : cons)
                con.setAccessible(true);
            return cons;
        }

        public static Field[] getDeclaredFields(Class<?> clazz) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields)
                field.setAccessible(true);
            return fields;
        }

        public static Class<?> getClass(String className) {
            try {
                return Class.forName(className, false,
                        Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public static int indexOfIdentity(Object[] arr, Object value) {
            for (int i = 0; i < arr.length; i++)
                if (arr[i] == value)
                    return i;
            return -1;
        }

        public static Type replaceVariable(Type type, TypeVariable<?>[] src, Object dst) {
            if (type instanceof GenericArrayType) {
                Type result = replaceVariable(((GenericArrayType) type).getGenericComponentType(),
                        src, dst);
                if (result != type)
                    return Types.newGenericArrayType(result);
            } else if (type instanceof ParameterizedType) {
                ParameterizedType that = (ParameterizedType) type;
                Type[] newParams = replaceParamsVariable(that.getActualTypeArguments(), src, dst);
                if (newParams != null)
                    return Types.newParameterizedType(that.getRawType(), newParams);
            } else if (type instanceof TypeVariable<?>) {
                TypeVariable<?> that = (TypeVariable<?>) type;
                String name = that.getName();
                for (int i = 0; i < src.length; i++) {
                    if (name.equals(src[i].getName())) {
                        if (dst instanceof Type)
                            return (Type) dst;
                        else
                            return ((Type[]) dst)[i];
                    }
                }
                Type[] newParams = replaceParamsVariable(that.getBounds(), src, dst);
                if (newParams != null)
                    return Types.newTypeVariable(name, newParams);
            } else if (type instanceof WildcardType) {
                WildcardType that = (WildcardType) type;
                Type[] lowerBounds = that.getLowerBounds();
                Type[] upperBounds = that.getUpperBounds();
                Type[] bounds = lowerBounds.length == 0 ? upperBounds : lowerBounds;
                Type[] newParams = replaceParamsVariable(bounds, src, dst);
                if (newParams != null)
                    return lowerBounds.length == 0 ? Types.newWildcardType(newParams, lowerBounds)
                            : Types.newWildcardType(upperBounds, newParams);
            }
            return type;
        }

        public static Type[] replaceParamsVariable(Type[] params, TypeVariable<?>[] src,
                Object dst) {
            if (params == null)
                return null;
            Type[] newParams = null;
            for (int i = 0; i < params.length; i++) {
                Type st = params[i];
                Type dt = replaceVariable(st, src, dst);
                if (st != dt) {
                    if (newParams == null)
                        newParams = new Type[params.length];
                    newParams[i] = dt;
                }
            }
            return newParams;
        }
    }

    /**
     * 用于动态生成几种泛型Type
     */
    public static class Types {
        public static GenericArrayType newGenericArrayType(Type genericComponentType) {
            return new GenericArrayType() {
                @Override
                public Type getGenericComponentType() {
                    return genericComponentType;
                }

                @Override
                public String toString() {
                    return new MyStringBuilder().append(genericComponentType.getTypeName())
                            .append("[]").toString();
                }

                @Override
                public int hashCode() {
                    return genericComponentType.hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                    return this == obj || obj instanceof GenericArrayType && genericComponentType
                            .equals(((GenericArrayType) obj).getGenericComponentType());
                }
            };
        }

        public static ParameterizedType newParameterizedType(Type rawType,
                Type... actualTypeArguments) {
            return new ParameterizedType() {
                @Override
                public Type getRawType() {
                    return rawType;
                }

                @Override
                public Type[] getActualTypeArguments() {
                    return actualTypeArguments;
                }

                @Override
                public String toString() {
                    return new MyStringBuilder().append(rawType.getTypeName()).append("<")
                            .appends(actualTypeArguments, Type::getTypeName, ",").append(">")
                            .toString();
                }

                @Override
                public int hashCode() {
                    return rawType.hashCode() ^ Arrays.hashCode(actualTypeArguments);
                }

                @Override
                public boolean equals(Object obj) {
                    ParameterizedType that;
                    return this == obj || obj instanceof ParameterizedType
                            && rawType == (that = (ParameterizedType) obj).getRawType()
                            && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
                }

                @Override
                public Type getOwnerType() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public static TypeVariable<?> newTypeVariable(String name, Type[] bounds) {
            return new TypeVariable<GenericDeclaration>() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Type[] getBounds() {
                    return bounds;
                }

                @Override
                public String toString() {
                    return name;
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                    return this == obj || obj instanceof TypeVariable<?>
                            && name.equals(((TypeVariable<?>) obj).getName());
                }

                @Override
                public AnnotatedType[] getAnnotatedBounds() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Annotation[] getAnnotations() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Annotation[] getDeclaredAnnotations() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public GenericDeclaration getGenericDeclaration() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public static WildcardType newWildcardType(Type[] upperBounds, Type[] lowerBounds) {
            return new WildcardType() {
                @Override
                public Type[] getUpperBounds() {
                    return upperBounds;
                }

                @Override
                public Type[] getLowerBounds() {
                    return lowerBounds;
                }

                @Override
                public String toString() {
                    Type[] bounds;
                    MyStringBuilder sb = new MyStringBuilder();
                    if (lowerBounds.length == 0) {
                        if (upperBounds.length == 0 || Object.class == upperBounds[0])
                            return "?";
                        bounds = upperBounds;
                        sb = new MyStringBuilder("? extends ");
                    } else {
                        bounds = lowerBounds;
                        sb = new MyStringBuilder("? super ");
                    }
                    return sb.<Type>appends(bounds, (value, s) -> s.append(value.getTypeName()),
                            " & ").toString();
                }

                @Override
                public int hashCode() {
                    return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
                }

                @Override
                public boolean equals(Object obj) {
                    WildcardType that;
                    return this == obj || obj instanceof WildcardType
                            && Arrays.equals(upperBounds,
                                    (that = (WildcardType) obj).getUpperBounds())
                            && Arrays.equals(lowerBounds, that.getLowerBounds());
                }
            };
        }
    }
}
