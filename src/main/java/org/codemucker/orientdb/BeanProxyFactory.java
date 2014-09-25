package org.codemucker.orientdb;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

import org.apache.logging.log4j.util.Strings;

import com.tinkerpop.blueprints.Vertex;

class BeanProxyFactory {
    private static List<String> ignoreMethods = new ArrayList<>();

    private final Class<?> beanClass;
    private final ProxyFactory factory;
    private final Class<?> proxyClass;
    private final Map<String, MethodMeta> methodMetaByMethodName;
    private final String logicalName;

    static {
        Method[] methods = Object.class.getMethods();
        for (Method m : methods) {
            ignoreMethods.add(m.getName());
        }
//        methods = ProxyBean.class.getMethods();
//        for (Method m : methods) {
//            ignoreMethods.add(m.getName());
//        }

    }

    public BeanProxyFactory(Class<?> beanClass) {
        this.beanClass = beanClass;
        String logicalName = beanClass.getSimpleName();
        VertexBean classAnon = (VertexBean) beanClass.getAnnotation(VertexBean.class);
        if (classAnon != null && !Strings.isEmpty(classAnon.name())) {
            logicalName = classAnon.name();
        }
        this.logicalName = logicalName;
        methodMetaByMethodName = extractMethodMeta(beanClass);
        factory = new ProxyFactory();
        factory.setUseCache(true);
        factory.setSuperclass(beanClass);
        factory.setInterfaces(new Class[] { ProxyBean.class });

        factory.setFilter(new MethodFilter() {
            @Override
            public boolean isHandled(Method m) {
                return !ignoreMethods.contains(m.getName());
            }
        });
        proxyClass = factory.createClass();
    }

    public Object newBeanInstance(final Vertex v) {
        try {
            PropertyMethodInterceptor handler = new PropertyMethodInterceptor(v, methodMetaByMethodName);
            Object instance = proxyClass.newInstance();
            ((Proxy) instance).setHandler(handler);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating proxy class", e);
        }
    }

    private static Map<String, MethodMeta> extractMethodMeta(Class<?> beanClass) {
        Map<String, MethodMeta> meta = new HashMap<>();
        Method[] methods = beanClass.getMethods();
        for (Method m : methods) {
            MethodMeta methodMeta = extractMeta(m);
            if (methodMeta != null) {
                meta.put(methodMeta.getMethodSig(), methodMeta);
            }
        }
        return meta;
    }

    private static MethodMeta extractMeta(Method m) {
        if (ignoreMethod(m)) {// .isSynthetic() || m.isBridge() ||
                              // !m.isAccessible() ||
                              // ignoreMethods.contains(m.getName())){
            return null;
        }
        String property = null;
        boolean setter = true;
        String methodName = m.getName();
        if (methodName.startsWith("get") && m.getParameterTypes().length == 0) {
            property = extractPropNameOrNull("get", methodName);
            setter = false;
        } else if (methodName.startsWith("is") && m.getParameterTypes().length == 0) {
            property = extractPropNameOrNull("is", methodName);
            setter = false;
            ;
        } else if (methodName.startsWith("set") && m.getParameterTypes().length == 1) {
            property = extractPropNameOrNull("set", methodName);
            setter = true;
        }

        Property vertexProp = m.getAnnotation(Property.class);
        if (vertexProp != null) {
            if (vertexProp.property() != "") {
                property = vertexProp.property();
            }
            if (vertexProp.ignore()) {
                property = null;
            }
        }
        if (property != null) {
            return new MethodMeta(m, property, setter);
        }
        return null;
    }

    private static boolean ignoreMethod(Method m) {
        return m.isSynthetic() || m.isBridge() || Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers()) || ignoreMethods.contains(m.getName());
    }

    private static String extractPropNameOrNull(String prefix, String methodName) {
        if (methodName.length() == prefix.length() + 1) {// eg. getX
            return Character.toLowerCase(methodName.charAt(prefix.length())) + "";
        }
        if (methodName.length() > prefix.length()) { // eg getFoo
            return Character.toLowerCase(methodName.charAt(prefix.length())) + methodName.substring(prefix.length() + 1);
        }
        return null;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getLogicalName() {
        return logicalName;
    }

}