package org.codemucker.orientdb;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.vyarus.guice.persist.orient.db.DatabaseManager;
import ru.vyarus.guice.persist.orient.db.scheme.PackageSchemeInitializer;
import ru.vyarus.guice.persist.orient.db.scheme.SchemeInitializer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class DbModelInitialiser extends PackageSchemeInitializer { //implements SchemeInitializer {

    private static final Logger logger = LogManager.getLogger(SchemeInitializer.class);

    private final Provider<OrientGraphNoTx> provider;

    @Inject
    public DbModelInitialiser(Provider<OrientGraphNoTx> provider,@Named("orient.model.package") final String modelPkg,
            final Provider<OObjectDatabaseTx> dbProvider,
            final Provider<DatabaseManager> databaseManager) {
        super(modelPkg,dbProvider,databaseManager);
        this.provider = provider;
    }

    @Override
    public void init(OObjectDatabaseTx db){
        scripts(provider.get());
        super.init(db);
    }

    private void scripts(OrientGraphNoTx db) {
        script(db, "/schema.defaults.osql.txt");
        //script(db, "/schema.darelist.osql.txt");
    }

    private void script(OrientGraphNoTx db, String script) {
        String sql;
        try {
            sql = readResource(this.getClass(), script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("executing schema script:\n" + sql);
        db.command(new OCommandScript("sql", sql)).execute();
        db.commit();
    }
    
    private static String readResource(Class<?> fromClass, String path) throws IOException {
        try (InputStream is = fromClass.getResourceAsStream(path)) {
            if(is == null){
                throw new IOException("could not load " + path + " relative to class " + fromClass.getName());
            }
            return IOUtils.toString(is);
        }
    }

}
