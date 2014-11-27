package org.codemucker.orientdb;

import org.junit.After;
import org.junit.Before;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

public abstract class BaseDBTest {

    private PersistService dbService;
    private Injector injector;
    private UnitOfWork uow;
    
    @Before
    public void setup() throws Exception {
        Config cfg = TestConfig.withDefaults();//.parseArgs(new String[] { "--db.url=plocal:target/junit_test_db" });
        injector = Guice.createInjector(new GuiceConfigModule(cfg), new GuiceDatabaseModule(cfg));
        dbService = injector.getInstance(PersistService.class);
        dbService.start();
        
        uow = injector.getInstance(UnitOfWork.class);
        uow.begin();
    }

    @After
    public void teardown() {
        uow.end();
        dbService.stop();
    }
    
    protected <T> T inject(Class<T> type){
        return injector.getInstance(type);
    }
}
