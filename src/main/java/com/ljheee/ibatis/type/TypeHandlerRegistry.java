package com.ljheee.ibatis.type;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 自己简化后的 TypeHandlerRegistry
 * 就是保存 不同类型、及其对应的 TypeHandler
 * <p>
 * 每一个具体的TypeHandler 实现也非常简单，就是完成对应类型ps.setXxxx
 * <p>
 * 如果把所有Java类型，都实现相应的TypeHandler，感觉有些繁多，为精简起见，实现了两个具代表性的int和string类型
 */
public final class TypeHandlerRegistry {


    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();

    public TypeHandlerRegistry() {

        register(Integer.class, new IntegerTypeHandler());
        register(int.class, new IntegerTypeHandler());

        register(String.class, new StringTypeHandler());

//    register(Long.class, new LongTypeHandler());
//    register(long.class, new LongTypeHandler());
//
//    register(Float.class, new FloatTypeHandler());
//    register(float.class, new FloatTypeHandler());
//    register(JdbcType.FLOAT, new FloatTypeHandler());
//
//    register(Double.class, new DoubleTypeHandler());
//    register(double.class, new DoubleTypeHandler());
//    register(JdbcType.DOUBLE, new DoubleTypeHandler());
//
//    register(String.class, new StringTypeHandler());
//    register(String.class, JdbcType.CHAR, new StringTypeHandler());
//    register(String.class, JdbcType.CLOB, new ClobTypeHandler());
//    register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
//    register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
//    register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
//    register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
//    register(String.class, JdbcType.NCLOB, new NClobTypeHandler());

    }


    public TypeHandler<?> getMappingTypeHandler(Class<?> handlerType) {
        return ALL_TYPE_HANDLERS_MAP.get(handlerType);
    }

    public void register(Class<?> type, TypeHandler<?> handler) {
        ALL_TYPE_HANDLERS_MAP.put(type, handler);
    }

    public Collection<TypeHandler<?>> getTypeHandlers() {
        return Collections.unmodifiableCollection(ALL_TYPE_HANDLERS_MAP.values());
    }

}
