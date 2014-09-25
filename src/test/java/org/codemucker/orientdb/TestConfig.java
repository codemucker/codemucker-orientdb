package org.codemucker.orientdb;


public class TestConfig {

    public static Config withDefaults() {
        Config cfg = Config
                   .withDefaults()
                   .with(m->m.set(Config.Key.DB_URL, "memory:db_dl_junit_test").set(Config.Key.DB_USERNAME, "admin").set(Config.Key.DB_PASSWORD, "admin"))
                   ;
        return cfg;
    }
    
}
