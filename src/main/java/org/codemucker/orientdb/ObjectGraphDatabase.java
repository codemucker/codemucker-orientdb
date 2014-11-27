package org.codemucker.orientdb;

import java.util.Arrays;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class ObjectGraphDatabase {
    private final EntityManager entityManager;
    private final Provider<OrientGraph> dbProvider;

    @Inject
    public ObjectGraphDatabase(Provider<OrientGraph> dbProvider) {
        super();
        this.dbProvider = dbProvider;
        this.entityManager = new EntityManager(dbProvider);
        
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public <T> T newInstance(Class<T> beanClass) {
        BeanProxyFactory factory = entityManager.getBeanFactoryForType(beanClass);
        String clsName = factory.getLogicalName();

        OrientVertex v = getGraph().addVertex("class:" + clsName);
        Object bean = factory.newBeanInstance(v);

        return (T) bean;
    }

    protected <T> T wrap(Class<T> beanClass, Vertex v) {
        BeanProxyFactory factory = entityManager.getBeanFactoryForType(beanClass);
        Object bean = factory.newBeanInstance(v);
        return (T) bean;
    }

    public void save(Object bean) {
        asElement(bean).save();
    }

    public void remove(Object bean) {
        asElement(bean).remove();
    }

    public void rollback() {
        getGraph().rollback();
    }

    private OrientElement asElement(Object bean) {
        if (bean instanceof ProxyBean) {
            OrientElement ele = ((ProxyBean) bean).getElement();
            return ele;
        } else {
            throw new IllegalArgumentException(bean.getClass().getName() + " is not registered. Are you sure you created this instance via the db.newInstance(<bean-class>) method?. Expected one of " + Arrays.toString(entityManager.getRegistedBeanClasses()));
        }
    }

    public OrientGraph getGraph() {
        return dbProvider.get();
    }

    

}
