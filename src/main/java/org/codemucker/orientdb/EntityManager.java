package org.codemucker.orientdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.vyarus.guice.persist.orient.db.DbType;
import ru.vyarus.guice.persist.orient.db.scheme.annotation.EdgeType;
import ru.vyarus.guice.persist.orient.db.scheme.annotation.VertexType;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class EntityManager {
    private Map<Class<?>, BeanProxyFactory> factoryByBeanClass = new ConcurrentHashMap<>();

    private final Provider<OrientGraph> dbProvider;

    private Map<String, SchemaClass> schemaClasses = new HashMap<String, SchemaClass>();
    private static final Logger logger = LogManager.getLogger(EntityManager.class);

    // private final Provider<OrientGraph> dbProvider;

    class SchemaClass {
        private Class<?> beanClass;
        private String parentSchemaClassName;

        public SchemaClass(Class<?> beanClass, Class<?> parentSchemaClass) {
            this(beanClass, parentSchemaClass.getSimpleName());
        }
        
        public SchemaClass(Class<?> beanClass, String parentSchemaClass) {
            super();
            this.beanClass = beanClass;
            this.parentSchemaClassName = parentSchemaClass;
        }

        public Class<?> getBeanClass() {
            return beanClass;
        }

        public String getParentSchemaClassName() {
            return parentSchemaClassName;
        }
        
        public String getBeanSchemaClassName(){
            return beanClass.getSimpleName();
        }

    }

    // @Inject
    // public EntityManager(Provider<OrientGraph> dbProvider) {
    // this.dbProvider = dbProvider;
    // }

    public EntityManager(Provider<OrientGraph> dbProvider) {
        this.dbProvider = dbProvider;
    }

    public Class<?>[] getRegistedBeanClasses(){
        return factoryByBeanClass.keySet().toArray(new Class[]{});
    }
    
    public <T> void register(Class<T> beanClass) {
        BeanProxyFactory beanProxyFactory = factoryByBeanClass.get(beanClass);
        if (beanProxyFactory == null) {
            beanProxyFactory = new BeanProxyFactory(beanClass);
            factoryByBeanClass.put(beanClass, beanProxyFactory);
            // dbProvider.get().getRawGraph().get
            //registerAsSchemaClass(beanClass);
        }
    }

    /**
     * If class annotated with @Edge or @Vertex will create.
     *
     * @param modelClass
     *            model class to map to scheme
     */
    @SuppressWarnings("unchecked")
    protected void registerAsSchemaClass(final Class modelClass) {
        logger.info("Registering model class: {}", modelClass);
        final VertexBean vertex = (VertexBean) modelClass.getAnnotation(VertexBean.class);
        final EdgeBean edge = (EdgeBean) modelClass.getAnnotation(EdgeBean.class);
        Preconditions.checkState(vertex == null || edge == null, "You can't use both Vertex and Edge annotations together, choose one.");
        final boolean isVertex = vertex != null;
        if (!isVertex && edge == null) {
            throw new IllegalArgumentException("Model class " + modelClass.getName() + " has neither " + VertexBean.class.getName() + " or "
                    + EdgeBean.class.getName() + " annotations");
        }

        List<SchemaClass> toCreate = new ArrayList<>();
        Class<?> currentType = modelClass;
        Class<?> supertype = modelClass;
        while (!Object.class.equals(supertype) && supertype != null) {
            currentType = supertype;
            supertype = supertype.getSuperclass();    
            if(!schemaClasses.containsKey(currentType.getSimpleName())){
                if(!Object.class.equals(supertype) ){
                    toCreate.add(new SchemaClass(currentType, supertype));
                } else {
                    toCreate.add(new SchemaClass(currentType, isVertex?"V":"E"));
                }
            }
        }

        for (int i = toCreate.size() - 1; i > 0; i--) {
            SchemaClass sc = toCreate.get(i);
            logger.info("Creating model class scheme {} as extension to {}", sc.getBeanSchemaClassName(), sc.getParentSchemaClassName());
            getDb().command(new OCommandSQL("create class " + sc.getBeanSchemaClassName() +  " extends " + sc.getParentSchemaClassName())).execute();
            schemaClasses.put(sc.getBeanSchemaClassName(), sc);
        }
    }

    private OrientGraph getDb() {
        return dbProvider.get();
    }

    private Class<?> findRootType(final Class<?> type) {
        // hierarchy support (topmost class must be vertex)
        Class<?> supertype = type;
        Class<?> baseType = type;
        while (!Object.class.equals(supertype) && supertype != null) {
            baseType = supertype;
            supertype = supertype.getSuperclass();
        }
        return baseType;
    }

    @SuppressWarnings("unchecked")
    protected <T> T addVertex(Class<T> beanClass, OrientGraph graph) {
        BeanProxyFactory beanProxyFactory = factoryByBeanClass.get(beanClass);
        String clsName = beanProxyFactory.getLogicalName();

        OrientVertex v = graph.addVertex("class:" + clsName);
        Object bean = beanProxyFactory.newBeanInstance(v);

        return (T) bean;
    }

    @SuppressWarnings("unchecked")
    protected <T> T wrapVertex(Class<T> beanClass, Vertex v) {
        BeanProxyFactory beanProxyFactory = getBeanFactoryForType(beanClass);
        Object bean = beanProxyFactory.newBeanInstance(v);
        return (T) bean;
    }

    protected BeanProxyFactory getBeanFactoryForType(Class<?> beanClass) {
        BeanProxyFactory beanProxyFactory = factoryByBeanClass.get(beanClass);
        if (beanProxyFactory == null) {
            throw new IllegalStateException("no bean proxy registered for class" + beanClass.getName());
        }
        return beanProxyFactory;
    }
}