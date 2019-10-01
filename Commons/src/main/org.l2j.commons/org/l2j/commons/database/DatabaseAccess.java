package org.l2j.commons.database;

import org.l2j.commons.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.lang.reflect.Proxy;
import java.sql.SQLException;

/**
 * @author JoeAlisson
 */
public class DatabaseAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccess.class);
    @SuppressWarnings("rawtypes")
    private static final Cache<Class, DAO> cache = CacheFactory.getInstance().getCache("dao", Class.class, DAO.class);
    private static final JDBCInvocation handler = new JDBCInvocation();

    private volatile static boolean initialized = false;

    public static boolean initialize() {
        if(initialized) {
            return true;
        }
        try {
            DatabaseFactory.getInstance();
            return initialized = true;
        } catch (SQLException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return initialized;
    }

    public static <T extends DAO<?>> T getDAO(Class<T> daoClass) {
        if(cache.containsKey(daoClass)) {
            return daoClass.cast(cache.get(daoClass));
        }

        var dao =  daoClass.cast(Proxy.newProxyInstance(daoClass.getClassLoader(), new Class[]{ daoClass }, handler));
        cache.put(daoClass, dao);
        return dao;
    }

    public static void shutdown() {
        try {
            DatabaseFactory.getInstance().shutdown();
        } catch (SQLException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }
}
