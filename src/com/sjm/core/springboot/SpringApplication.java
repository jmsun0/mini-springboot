package com.sjm.core.springboot;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;
import com.sjm.core.util.core.Converter;
import com.sjm.core.util.misc.ClassScaner;

public class SpringApplication {
    static final Logger logger = LoggerFactory.getLogger(SpringApplication.class);

    static {
        try {
            loadProperties();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void loadProperties() {
        ResourceBundle res = ResourceBundle.getBundle("application");
        Enumeration<String> keys = res.getKeys();
        Properties properties = System.getProperties();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!properties.containsKey(key))
                System.setProperty(key, res.getString(key));
        }
    }

    protected Map<Object, Object> beanMap = new HashMap<>();
    protected Set<BeanDefinition> beanSet = new LinkedHashSet<>();
    private List<String> packages = new ArrayList<>();
    private List<Object> targets = new ArrayList<>();
    private Map<Class<?>, List<Class<?>>> annotationClassesMap = new HashMap<>();
    private Map<Class<?>, ReflectionCacheData> reflectionCacheDataMap = new HashMap<>();
    private Predicate<Class<?>> scanFilter;
    private Set<BeanDefinition> extraBeanSet = new LinkedHashSet<>();
    private Set<BeanDefinition> initializingBeanSet = new LinkedHashSet<>();

    public SpringApplication(Object... sources) {
        for (Object source : sources)
            addSource(source);
    }

    public void run(String... args) {
        try {
            tryRun(args);
        } catch (SpringException e) {
            throw e;
        } catch (Exception e) {
            throw new SpringException(e);
        }
    }

    private void tryRun(String... args) throws Exception {
        ClassScaner.scanClasses(packages.toArray(new String[packages.size()]), true,
                this::addScanClass);
        List<Class<?>> componentClassList = annotationClassesMap.get(Component.class);
        if (componentClassList == null || componentClassList.isEmpty())
            throw new SpringException("no component was found");

        for (Class<?> componentClass : componentClassList) {
            registerComponentBean(componentClass);
            if (AnnotationBeanRegister.class.isAssignableFrom(componentClass))
                registerAnnotationBean(componentClass);
            registerMethodBeans(componentClass);
        }

        assignDependsOn();

        BeanDefinition appDef = new BeanDefinition();
        appDef.bean = this;
        appDef.type = SpringApplication.class;
        putBeanDefinition(appDef, false);

        if (targets.isEmpty()) {
            for (BeanDefinition def : beanSet)
                initBean(def);
        } else {
            for (Object target : targets)
                initBean(getBeanDefinition(target));
            beanSet = new LinkedHashSet<>();
            while (!extraBeanSet.isEmpty()) {
                beanSet.addAll(extraBeanSet);
                Set<BeanDefinition> extraBeanSetTmp = extraBeanSet;
                extraBeanSet = new LinkedHashSet<>();
                for (BeanDefinition def : extraBeanSetTmp)
                    initBean(def);
            }
            resetBeanMap();
        }

        for (BeanDefinition def : beanSet) {
            List<AutowiredFieldInfo> lazyAutowiredFields =
                    getReflectionCacheData(def.bean.getClass()).lazyAutowiredFields;
            if (!lazyAutowiredFields.isEmpty())
                for (AutowiredFieldInfo fieldInfo : lazyAutowiredFields)
                    fieldInfo.field.set(def.bean, getBean(fieldInfo.dep));
        }

        for (BeanDefinition def : beanSet)
            if (def.bean instanceof CommandLineRunner)
                ((CommandLineRunner) def.bean).run(args);

        logger.info("SpringApplication started OK");

        packages = null;
        targets = null;
        annotationClassesMap = null;
        reflectionCacheDataMap = null;
        extraBeanSet = null;
        initializingBeanSet = null;
    }

    public void addSource(Object source) {
        if (source instanceof Class<?>) {
            Class<?> clazz = (Class<?>) source;
            SpringBootApplication app = clazz.getAnnotation(SpringBootApplication.class);
            String[] pkgs = app.scanBasePackages();
            if (pkgs == null || pkgs.length == 0)
                packages.add(clazz.getPackage().getName());
            else {
                for (String pkg : pkgs)
                    packages.add(pkg);
            }
        } else if (source instanceof String) {
            packages.add((String) source);
        }
    }

    public void addTarget(Object target) {
        targets.add(target);
    }

    public void addComponent(Class<?> clazz) {
        addScanClass(Component.class, clazz);
    }

    public void setScanFilter(Predicate<Class<?>> scanFilter) {
        this.scanFilter = scanFilter;
    }

    private void addScanClass(Class<?> clazz) {
        if (scanFilter != null && !scanFilter.test(clazz))
            return;
        Annotation[] anns = clazz.getDeclaredAnnotations();
        if (anns != null && anns.length != 0)
            for (Annotation ann : anns)
                addScanClass(ann.annotationType(), clazz);
    }

    private void addScanClass(Class<?> annType, Class<?> clazz) {
        List<Class<?>> classes = annotationClassesMap.get(annType);
        if (classes == null)
            annotationClassesMap.put(annType, classes = new ArrayList<>());
        classes.add(clazz);
    }

    private void initBean(BeanDefinition def) throws Exception {
        try {
            initializingBeanSet.clear();
            tryInitBean(def);
        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder("Bean init fail\n");
            for (BeanDefinition bd : initializingBeanSet) {
                sb.append("-->").append(bd.type.getName()).append("\n");
            }
            logger.error(sb.toString());
            throw e;
        }
    }

    private void tryInitBean(BeanDefinition def) throws Exception {
        if (def.bean == null) {
            if (!initializingBeanSet.add(def))
                throw new SpringException("Circular dependency");
            if (def.dependsOn != null) {
                for (Object dep : def.dependsOn)
                    tryInitBean(getBeanDefinition(dep));
            }
            BeanDefinition factoryDef = getBeanDefinition(def.factoryName);
            tryInitBean(factoryDef);
            if ((def.bean = ((FactoryBean<?>) factoryDef.bean).getObject()) == null)
                throw new SpringException("The factory of bean[" + def.type + "] return null");
            beanPostProcess(def);
            initializingBeanSet.remove(def);
        }
    }

    private void beanPostProcess(BeanDefinition def) throws Exception {
        Object bean = def.bean;
        ReflectionCacheData data = getReflectionCacheData(bean.getClass());

        for (int i = 0; i < data.valueFields.size(); i++) {
            Field field = data.valueFields.get(i);
            Value val = field.getAnnotation(Value.class);
            String exp = val.value();
            int start = exp.indexOf('{');
            int end = exp.lastIndexOf('}');
            if (start == -1 || end <= start)
                throw new SpringException("Value expression error");
            int colon = exp.indexOf(':');
            String valueStr;
            if (colon == -1)
                valueStr = System.getProperty(exp.substring(start + 1, end));
            else
                valueStr = System.getProperty(exp.substring(start + 1, colon),
                        exp.substring(colon + 1, end));
            Object value = Converter.INSTANCE.convert(valueStr, field.getType());
            if (value != null)
                field.set(bean, value);
        }

        for (int i = 0; i < data.autowiredFields.size(); i++) {
            AutowiredFieldInfo fieldInfo = data.autowiredFields.get(i);
            BeanDefinition fieldDef = getBeanDefinition(fieldInfo.dep);
            tryInitBean(fieldDef);
            fieldInfo.field.set(bean, fieldDef.bean);
        }

        for (AutowiredMethodInfo methodInfo : data.autowiredMethods)
            methodInfo.method.invoke(bean, getBeans(methodInfo.paramsDeps));

        if (!targets.isEmpty()) {
            extraBeanSet.add(def);
            for (AutowiredFieldInfo fieldInfo : data.lazyAutowiredFields)
                extraBeanSet.add(getBeanDefinition(fieldInfo.dep));
        }
    }

    private static final Object[] Lazy_Assign_Depends_On = new Object[0];

    private void assignDependsOn() {
        for (BeanDefinition def : beanSet) {
            if (def.dependsOn != Lazy_Assign_Depends_On)
                continue;
            ReflectionCacheData data = getReflectionCacheData(def.type);
            List<AutowiredFieldInfo> autowiredFields = data.autowiredFields;
            List<AutowiredMethodInfo> autowiredMethods = data.autowiredMethods;
            DependsOn dep = def.type.getDeclaredAnnotation(DependsOn.class);
            String[] depValues = dep == null ? null : dep.value();
            if (autowiredFields.isEmpty() && autowiredMethods.isEmpty()
                    && (depValues == null || depValues.length == 0)) {
                def.dependsOn = null;
                continue;
            }
            Set<Object> depClasses = new HashSet<>();
            for (AutowiredFieldInfo fieldInfo : autowiredFields)
                depClasses.add(fieldInfo.dep);
            for (AutowiredMethodInfo methodInfo : autowiredMethods)
                for (Object paramsDep : methodInfo.paramsDeps)
                    depClasses.add(paramsDep);
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (depValues != null && depValues.length > 0) {
                for (String depValue : depValues) {
                    try {
                        Class<?> cls = Class.forName(depValue, false, loader);
                        if (beanMap.containsKey(cls))
                            depClasses.add(cls);
                    } catch (ClassNotFoundException ex) {
                        depValue += ".";
                        for (BeanDefinition bd : beanSet)
                            if (bd.type.getName().startsWith(depValue))
                                depClasses.add(bd.type);
                    }
                }
            }
            def.dependsOn = depClasses.toArray();
        }
    }

    private void registerComponentBean(Class<?> componentClass) {
        String factoryDefName = "SimpleFactoryBean_" + componentClass.getName();

        BeanDefinition factoryDef = new BeanDefinition();
        factoryDef.bean = new SimpleFactoryBean(componentClass);
        factoryDef.type = SimpleFactoryBean.class;
        putBeanDefinition(factoryDef, false);
        putBeanDefinition(factoryDefName, factoryDef);

        BeanDefinition def = new BeanDefinition();
        def.factoryName = factoryDefName;
        def.dependsOn = Lazy_Assign_Depends_On;
        def.type = componentClass;
        putBeanDefinition(def, componentClass.getAnnotation(Primary.class) != null);
        Component component = componentClass.getAnnotation(Component.class);
        if (component != null && !isEmpty(component.value()))
            putBeanDefinition(component.value(), def);
    }

    @SuppressWarnings("unchecked")
    private void registerAnnotationBean(Class<?> componentClass) throws Exception {
        Class<? extends Annotation> annType = null;
        Type[] inters = componentClass.getGenericInterfaces();
        if (inters.length != 0 && inters[0] instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) inters[0];
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 1 && types[0] instanceof Class)
                annType = (Class<? extends Annotation>) types[0];
        }
        if (annType == null)
            throw new SpringException("Missing annotation type");
        List<Class<?>> beanClasses = annotationClassesMap.get(annType);
        if (beanClasses == null)
            return;
        AnnotationBeanRegister<Annotation> register =
                (AnnotationBeanRegister<Annotation>) componentClass.newInstance();
        for (Class<?> beanClass : beanClasses) {
            Annotation ann = beanClass.getAnnotation(annType);
            AnnotationBeanDefinition annDef = register.register(ann, beanClass);

            if (!beanClass.isInterface())
                registerMethodBeans(beanClass);

            String factoryFactoryDefName = "AnnotationFactoryBean_" + beanClass.getName();

            BeanDefinition factoryFactoryDef = new BeanDefinition();
            factoryFactoryDef.bean = new AnnotationFactoryBean(componentClass, annDef);
            factoryFactoryDef.type = AnnotationFactoryBean.class;
            putBeanDefinition(factoryFactoryDef, false);
            putBeanDefinition(factoryFactoryDefName, factoryFactoryDef);

            String factoryDefName = "AnnotationFactoryBeanSub_" + beanClass.getName();

            BeanDefinition factoryDef = new BeanDefinition();
            factoryDef.factoryName = factoryFactoryDefName;
            factoryDef.dependsOn = Lazy_Assign_Depends_On;
            factoryDef.type = annDef.factoryClass;
            putBeanDefinition(factoryDef, false);
            putBeanDefinition(factoryDefName, factoryDef);

            BeanDefinition def = new BeanDefinition();
            def.factoryName = factoryDefName;
            def.dependsOn = Lazy_Assign_Depends_On;
            def.type = beanClass;
            putBeanDefinition(def, false);
            if (!isEmpty(annDef.name))
                putBeanDefinition(annDef.name, def);
        }
    }

    private void registerMethodBean(Class<?> componentClass, BeanMethodInfo methodInfo) {
        Method method = methodInfo.method;
        Object[] dependsOn = Arrays.copyOf(methodInfo.paramsDeps, methodInfo.paramsDeps.length + 1);
        dependsOn[dependsOn.length - 1] = componentClass;

        String factoryDefName = "MethodFactoryBean_" + method.getDeclaringClass().getName() + "_"
                + method.getName();

        BeanDefinition factoryDef = new BeanDefinition();
        factoryDef.bean = new MethodFactoryBean(this, methodInfo);
        factoryDef.type = MethodFactoryBean.class;
        putBeanDefinition(factoryDef, false);
        putBeanDefinition(factoryDefName, factoryDef);

        BeanDefinition def = new BeanDefinition();
        def.factoryName = factoryDefName;
        def.dependsOn = dependsOn;
        def.type = method.getReturnType();
        putBeanDefinition(def, method.getAnnotation(Primary.class) != null);
        for (String name : methodInfo.names)
            if (!isEmpty(name) && !name.equals(method.getName()))
                putBeanDefinition(name, def);
        putBeanDefinition(method.getName(), def);
    }

    private void registerMethodBeans(Class<?> componentClass) {
        ReflectionCacheData data = getReflectionCacheData(componentClass);
        for (int i = 0; i < data.beanMethods.size(); i++)
            registerMethodBean(componentClass, data.beanMethods.get(i));
    }

    @SuppressWarnings("unchecked")
    public BeanDefinition getBeanDefinition(Object nameOrClass) {
        Object defOrList = beanMap.get(nameOrClass);
        if (defOrList == null)
            throw new SpringException("BeanDefinition [" + nameOrClass + "] not found");
        if (defOrList instanceof List)
            return ((List<BeanDefinition>) defOrList).get(0);
        else
            return (BeanDefinition) defOrList;
    }

    public Object getBean(Object nameOrClass) {
        Object bean = getBeanDefinition(nameOrClass).bean;
        if (bean == null)
            throw new SpringException("bean [" + nameOrClass + "] not found");
        return bean;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        return (T) getBean((Object) clazz);
    }

    private Object[] getBeans(Object[] nameOrClasses) {
        Object[] beans = new Object[nameOrClasses.length];
        for (int i = 0; i < nameOrClasses.length; i++)
            beans[i] = getBean(nameOrClasses[i]);
        return beans;
    }

    public void putBeanDefinition(BeanDefinition def, boolean primary) {
        Class<?> clazz = def.type;
        Set<Class<?>> superClasses = new HashSet<>();
        addClasses(superClasses, clazz);
        beanSet.add(def);
        for (Class<?> superClass : superClasses) {
            Object defOrList = beanMap.get(superClass);
            if (defOrList == null)
                beanMap.put(superClass, def);
            else if (defOrList instanceof List) {
                @SuppressWarnings("unchecked")
                List<BeanDefinition> list = (List<BeanDefinition>) defOrList;
                if (primary)
                    list.add(0, def);
                else
                    list.add(def);
            } else {
                List<BeanDefinition> list = new ArrayList<>();
                if (primary) {
                    list.add(def);
                    list.add((BeanDefinition) defOrList);
                } else {
                    list.add((BeanDefinition) defOrList);
                    list.add(def);
                }
                beanMap.put(superClass, list);
            }
        }
    }

    private void resetBeanMap() {
        for (Iterator<Map.Entry<Object, Object>> it = beanMap.entrySet().iterator(); it
                .hasNext();) {
            Object defOrList = it.next().getValue();
            if (defOrList instanceof List) {
                @SuppressWarnings("unchecked")
                List<BeanDefinition> list = (List<BeanDefinition>) defOrList;
                for (int i = list.size() - 1; i >= 0; i--)
                    if (!beanSet.contains(list.get(i)))
                        list.remove(i);
                if (list.isEmpty())
                    it.remove();
            } else {
                if (!beanSet.contains(defOrList))
                    it.remove();
            }
        }
    }

    private void addClasses(Set<Class<?>> superClasses, Class<?> clazz) {
        do {
            if (superClasses.add(clazz)) {
                for (Class<?> inter : clazz.getInterfaces())
                    addClasses(superClasses, inter);
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);
    }

    private void putBeanDefinition(String name, BeanDefinition def) {
        Object old = beanMap.put(name, def);
        if (old != null)
            throw new SpringException("duplicate bean name '" + name + "'");
    }

    private ReflectionCacheData getReflectionCacheData(Class<?> clazz) {
        ReflectionCacheData data = reflectionCacheDataMap.get(clazz);
        if (data == null) {
            reflectionCacheDataMap.put(clazz, data = new ReflectionCacheData());
            Class<?> classTmp = clazz;
            do {
                addReflectionCacheData(classTmp, data);
                classTmp = classTmp.getSuperclass();
            } while (classTmp != null && classTmp != Object.class);
        }
        return data;
    }

    private void addReflectionCacheData(Class<?> clazz, ReflectionCacheData data) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] anns = field.getDeclaredAnnotations();
            Value valueAnn = null;
            Autowired autowiredAnn = null;
            Lazy lazyAnn = null;
            Resource resourceAnn = null;
            for (int i = 0; i < anns.length; i++) {
                Annotation ann = anns[i];
                Class<? extends Annotation> annType = ann.annotationType();
                if (annType == Value.class)
                    valueAnn = (Value) ann;
                else if (annType == Autowired.class)
                    autowiredAnn = (Autowired) ann;
                else if (annType == Resource.class)
                    resourceAnn = (Resource) ann;
                else if (annType == Lazy.class)
                    lazyAnn = (Lazy) ann;
            }
            if (valueAnn != null || autowiredAnn != null || resourceAnn != null)
                field.setAccessible(true);
            if (valueAnn != null)
                data.valueFields.add(field);
            else if (autowiredAnn != null || resourceAnn != null) {
                (lazyAnn != null ? data.lazyAutowiredFields : data.autowiredFields)
                        .add(autowiredAnn != null ? new AutowiredFieldInfo(field, autowiredAnn)
                                : new AutowiredFieldInfo(field, resourceAnn));
            }
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Annotation[] anns = method.getDeclaredAnnotations();
            for (Annotation ann : anns) {
                Class<? extends Annotation> annType = ann.annotationType();
                Bean beanAnn = null;
                PostConstruct postConstructAnn = null;
                Autowired autowiredAnn = null;
                if (annType == Bean.class)
                    beanAnn = (Bean) ann;
                else if (annType == PostConstruct.class)
                    postConstructAnn = (PostConstruct) ann;
                else if (annType == Autowired.class)
                    autowiredAnn = (Autowired) ann;
                if (beanAnn != null || postConstructAnn != null || autowiredAnn != null)
                    method.setAccessible(true);
                if (beanAnn != null)
                    data.beanMethods.add(new BeanMethodInfo(method, beanAnn.value()));
                else if (postConstructAnn != null || autowiredAnn != null)
                    data.autowiredMethods.add(new AutowiredMethodInfo(method));
            }
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static class BeanDefinition {
        public Object bean;
        public Class<?> type;
        public Object[] dependsOn;
        public String factoryName;

        @Override
        public String toString() {
            return "type=" + type + ",dependsOn=" + Arrays.toString(dependsOn) + ",factoryName="
                    + factoryName + ",bean=" + bean;
        }
    }

    public static class SpringException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SpringException(String message, Throwable cause) {
            super(message, cause);
        }

        public SpringException(Throwable cause) {
            super(cause);
        }

        public SpringException(String message) {
            super(message);
        }

        public SpringException() {
            super();
        }
    }

    public static class AnnotationBeanDefinition {
        public String name;
        public Class<? extends FactoryBean<?>> factoryClass;
        public Object[] constructorArgs;

        public AnnotationBeanDefinition(String name, Class<? extends FactoryBean<?>> factoryClass,
                Object... constructorArgs) {
            this.name = name;
            this.factoryClass = factoryClass;
            this.constructorArgs = constructorArgs;
        }

        public AnnotationBeanDefinition() {}
    }

    public interface AnnotationBeanRegister<A extends Annotation> {
        public AnnotationBeanDefinition register(A ann, Class<?> clazz);
    }

    static class AutowiredFieldInfo {
        public Field field;
        public Object dep;

        public AutowiredFieldInfo(Field field, Resource resource) {
            this.field = field;
            this.dep = field.getType();
            if (!resource.name().isEmpty())
                this.dep = resource.name();
        }

        public AutowiredFieldInfo(Field field, Autowired autowired) {
            this.field = field;
            this.dep = field.getType();
        }
    }

    static class AutowiredMethodInfo {
        public Method method;
        public Object[] paramsDeps;

        public AutowiredMethodInfo(Method method) {
            this.method = method;
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            paramsDeps = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Object dep = paramTypes[i];
                for (Annotation paramAnn : paramAnns[i]) {
                    if (paramAnn.annotationType() == Qualifier.class) {
                        String name = ((Qualifier) paramAnn).value();
                        if (!isEmpty(name))
                            dep = name;
                        break;
                    }
                }
                paramsDeps[i] = dep;
            }
        }
    }

    static class BeanMethodInfo extends AutowiredMethodInfo {
        public String[] names;

        public BeanMethodInfo(Method method, String[] names) {
            super(method);
            this.names = names;
        }
    }

    static class ReflectionCacheData {
        public List<Field> valueFields = new ArrayList<>();
        public List<AutowiredFieldInfo> autowiredFields = new ArrayList<>();
        public List<AutowiredFieldInfo> lazyAutowiredFields = new ArrayList<>();
        public List<AutowiredMethodInfo> autowiredMethods = new ArrayList<>();
        public List<BeanMethodInfo> beanMethods = new ArrayList<>();
    }

    public static abstract class AbstractFactoryBean<T> implements FactoryBean<T> {
        protected Class<?> clazz;

        public AbstractFactoryBean(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> getObjectType() {
            return clazz;
        }

        @Override
        public boolean isSingleton() {
            return false;
        }

    }
    static class SimpleFactoryBean extends AbstractFactoryBean<Object> {
        public SimpleFactoryBean(Class<?> clazz) {
            super(clazz);
        }

        @Override
        public Object getObject() throws Exception {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
    }

    static class MethodFactoryBean extends AbstractFactoryBean<Object> {
        private SpringApplication app;
        private BeanMethodInfo methodInfo;

        public MethodFactoryBean(SpringApplication app, BeanMethodInfo methodInfo) {
            super(methodInfo.method.getReturnType());
            this.app = app;
            this.methodInfo = methodInfo;
        }

        @Override
        public Object getObject() throws Exception {
            Method method = methodInfo.method;
            return method.invoke(app.getBean(method.getDeclaringClass()),
                    app.getBeans(methodInfo.paramsDeps));
        }
    }

    static class AnnotationFactoryBean extends AbstractFactoryBean<FactoryBean<?>> {
        private AnnotationBeanDefinition annDef;

        public AnnotationFactoryBean(Class<?> clazz, AnnotationBeanDefinition annDef) {
            super(clazz);
            this.annDef = annDef;
        }

        @Override
        public FactoryBean<?> getObject() throws Exception {
            return (FactoryBean<?>) annDef.factoryClass.getConstructors()[0]
                    .newInstance(annDef.constructorArgs);
        }
    }
}
