package com.ljheee.ibatis;

import com.ljheee.ibatis.binding.MapperMethod;
import com.ljheee.ibatis.session.DefaultSqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * MapperProxy MyBatis真正执行CRUD的就是该代理
 */
public class MapperProxy<T> implements InvocationHandler {


    private DefaultSqlSession sqlSession;
    private Class<?> clazz;

    public MapperProxy(DefaultSqlSession sqlSession, Class<?> clazz) {
        this.sqlSession = sqlSession;
        this.clazz = clazz;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

//        // 参数转化
//        MapperMethod mapperMethod = new MapperMethod(clazz, method, sqlSession.getConfiguration());
//        Object param = mapperMethod.getMethod().convertArgsToSqlCommandParam(args);
//
//        Class<?> returnType = method.getReturnType();
//        String namespaceId = clazz.getName() + "." + method.getName();
//        if (Collection.class.isAssignableFrom(returnType)) {
//            return sqlSession.selectList(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
//        } else {
//            return sqlSession.selectOne(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
//        }
        MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }

    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = new MapperMethod(clazz, method, sqlSession.getConfiguration());// 用hashmap缓存 mapperMethod
        return mapperMethod;
    }
}
