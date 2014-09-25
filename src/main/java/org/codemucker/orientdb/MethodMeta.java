package org.codemucker.orientdb;

import java.lang.reflect.Method;

class MethodMeta {
    public final boolean setter;
    public final String property;
    public final String methodSig;
    
    public MethodMeta(Method m,String property, boolean setter) {
        super();
        this.methodSig = m.getName();
        this.property = property;
        this.setter = setter;
    }
    
    public boolean isSetter(){
        return setter;
    }
    
    public String getProperty(){
        return property;
    }
    
    public String getMethodSig(){
        return methodSig;
    }
    
}