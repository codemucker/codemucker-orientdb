package org.codemucker.orientdb;

import org.junit.Test;

import com.darelist.service.db.ObjectGraphDatabase;
import com.darelist.service.finder.AccountFinder;
import com.darelist.service.finder.DareFinder;
import com.darelist.service.finder.UserFinder;
import com.darelist.service.model.Account;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ObjectGraphTest extends BaseDBTest {

    @Test
    public void test1() throws Exception {
        MyWork work = inject(MyWork.class);
        work.createAccount(); 
    }

    public static class MyWork {
        private final ObjectGraphDatabase db;
        private final AccountFinder accountFinder;
        private final UserFinder userFinder;
        private final DareFinder dareFinder;

        @Inject
        public MyWork(ObjectGraphDatabase db, AccountFinder accountFinder, UserFinder profileFinder,DareFinder dareFinder) {
            this.db = db;
            this.accountFinder = accountFinder;
            this.userFinder = profileFinder;
            this.dareFinder = dareFinder;
        }

        @Transactional
        public void createAccount() {
            Account acc = db.newInstance(Account.class);
            acc.setEmail("test@nowhere.com");
            acc.setUsername("testuser");
                        
            db.save(acc);
        }

    }
}
