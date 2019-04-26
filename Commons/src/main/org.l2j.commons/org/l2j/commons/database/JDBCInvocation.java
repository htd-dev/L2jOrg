package org.l2j.commons.database;

import io.github.joealisson.primitive.maps.IntObjectMap;
import io.github.joealisson.primitive.maps.impl.HashIntObjectMap;
import io.github.joealisson.primitive.pair.IntObjectPair;
import io.github.joealisson.primitive.pair.impl.ImmutableIntObjectPairImpl;
import org.l2j.commons.cache.CacheFactory;
import org.l2j.commons.database.annotation.Column;
import org.l2j.commons.database.annotation.Query;
import org.l2j.commons.database.annotation.Table;
import org.l2j.commons.database.annotation.Transient;
import org.l2j.commons.database.handler.TypeHandler;
import org.l2j.commons.database.helpers.EntityBasedStrategy;
import org.l2j.commons.database.helpers.QueryDescriptor;
import org.l2j.commons.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

class JDBCInvocation implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCInvocation.class);
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(":(.*?):");
    private static final String REPLACE_TEMPLATE = "REPLACE INTO %s %s VALUES %s";
    // TODO use cache API
    private static final Cache<Method, QueryDescriptor> descriptors = CacheFactory.getInstance().getCache("sql-descriptors");
    private static final Cache<Class<?>, QueryDescriptor> saveDescriptors = CacheFactory.getInstance().getCache("sql-save-descriptors");

    JDBCInvocation() {
        for (TypeHandler typeHandler : ServiceLoader.load(TypeHandler.class)) {
            TypeHandler.MAP.put(typeHandler.type(), typeHandler);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(method.getName().equalsIgnoreCase("save") && method.getParameterCount() == 1) {
            return save(method, args);
        }

        var handler = TypeHandler.MAP.getOrDefault(method.getReturnType().getName(), TypeHandler.MAP.get(Object.class.getName()));

        if(isNull(handler)) {
            throw new IllegalStateException("There is no TypeHandler Service");
        }

        if(!method.isAnnotationPresent(Query.class)) {
            return handler.defaultValue();
        }

        try(var con = DatabaseFactory.getInstance().getConnection();
            var query = buildQuery(method)) {
            query.execute(con, args);
            return handler.handleResult(query);
        }
    }

    private boolean save(Method method, Object[] args) throws SQLException {
        if(args.length < 1 || isNull(args[0])) {
            return false;
        }

        var clazz = args[0].getClass();
        var table = clazz.getAnnotation(Table.class);

        if(isNull(table)) {
            LOGGER.error("The class {} must be annotated with @Table to save it", args[0].getClass());
            return false;
        }

        try(var con = DatabaseFactory.getInstance().getConnection();
            var query = buildSaveQuery(clazz, method, table) ) {
            query.execute(con, args);
            return true;
        }
    }

    private QueryDescriptor buildSaveQuery(Class<?> clazz, Method method, Table table) {
        if(saveDescriptors.containsKey(clazz)) {
            return saveDescriptors.get(clazz);
        }

        var fields = Util.fieldsOf(clazz);
        Map<String, IntObjectPair<Class<?>>> parameterMap = new HashMap<>(fields.size());

        var columns = fields.stream().filter(f -> !f.isAnnotationPresent(Transient.class))
                .peek(f -> parameterMap.put(f.getName(), new ImmutableIntObjectPairImpl<>(parameterMap.size()+1, f.getType())))
                .map(this::fieldToColumnName).collect(Collectors.joining(",", "(", ")"));

        var values = "?".repeat(parameterMap.size()).chars().mapToObj(Character::toString).collect(Collectors.joining(",", "(", ")"));

        var query = new QueryDescriptor(method, String.format(REPLACE_TEMPLATE, table.value(), columns, values), new EntityBasedStrategy(parameterMap));

        saveDescriptors.put(clazz, query);
        return query;
    }

    private String fieldToColumnName(Field field) {
        return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).value() : field.getName();
    }

    private QueryDescriptor buildQuery(final Method method)  {
        if(descriptors.containsKey(method)) {
            return descriptors.get(method);
        }
        var descriptor = buildDescriptor(method);
        descriptors.put(method, descriptor);

        return descriptor;
    }

    private QueryDescriptor buildDescriptor(Method method) {
        var query = method.getAnnotation(Query.class).value();
        if(method.getParameters().length == 0) {
            return new QueryDescriptor(method, query);
        }

        var matcher = PARAMETER_PATTERN.matcher(query);
        var parameterMapper = mapParameters(method.getParameters());
        IntObjectMap<IntObjectPair<Class<?>>> parameters = new HashIntObjectMap<>();

        var parameterCount = 0;
        while (matcher.find()) {
            if(!parameterMapper.containsKey(matcher.group(1))) {
                LOGGER.error("There is no correspondent parameter to variable {} on method {}#{}", matcher.group(1), method.getDeclaringClass().getName(), method.getName());
                parameters.put(++parameterCount, null);
            } else {
                parameters.put(++parameterCount, parameterMapper.get(matcher.group(1)));
            }
        }
        return new QueryDescriptor(method, matcher.replaceAll("?"), parameters);
    }

    private Map<String, IntObjectPair<Class<?>>> mapParameters(Parameter[] parameters) {
        Map<String, IntObjectPair<Class<?>>> parameterMap = new HashMap<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];
            parameterMap.put(parameter.getName(), new ImmutableIntObjectPairImpl<>(i, parameter.getType()));
        }
        return parameterMap;
    }

}
