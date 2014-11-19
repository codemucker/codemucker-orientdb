package org.codemucker.orientdb;

import java.lang.reflect.Method;
import java.util.Map;

import javassist.util.proxy.MethodHandler;

import com.tinkerpop.blueprints.Element;

/**
 * Intercept the proxy bean setters/getter to set/get the properties on the underlying document. 
 */
class PropertyMethodInterceptor implements MethodHandler {
    
    private final Element vertexOrEdge;
    private final Map<String, MethodMeta> methodMetaByName;
    
    private static final String METHOD_GET_ELEMENT = "getElement"; //ProxyBean.getElement
    //private static final String METHOD_GET_PROXY = "getProxy"; //ProxyBean.getProxy
        
    public PropertyMethodInterceptor(Element vertexOrEdge,Map<String,MethodMeta> methodMetaByName) {
        super();
        this.vertexOrEdge = vertexOrEdge;
        this.methodMetaByName = methodMetaByName;
    }

    @Override
    public Object invoke(Object self, Method calledMethod, Method realMethod, Object[] args) throws Throwable {
        //special method to get just the vertex for this proxy bean
        if( calledMethod.getParameterTypes().length == 0 ){
            if(calledMethod.getName().equals(METHOD_GET_ELEMENT)){
                return vertexOrEdge;
            }
        }
        MethodMeta meta = methodMetaByName.get(calledMethod.getName());
        if( meta != null){ //invoke the getter/setter to retrieve/set the property on the wrapped document
                if(meta.isSetter()){
                    vertexOrEdge.setProperty(meta.getProperty(), args[0]);
                    realMethod.invoke(self, args);//also call real setter so fields are correct and can be used
                    return null;
                } else {
                    return vertexOrEdge.getProperty(meta.getProperty());
                }
        } else {
            return realMethod.invoke(self, args);  // execute the original method.
        }
    }
}