package com.ljheee.ibatis;

import com.ljheee.ibatis.session.DefaultSqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

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
        Class<?> returnType = method.getReturnType();
//        String namespaceId = method.getDeclaringClass().getName() + "." + method.getName();
        String namespaceId = clazz.getName() + "." + method.getName();
        if (Collection.class.isAssignableFrom(returnType)) {
            return sqlSession.selectList(namespaceId, args == null ? null : args[0]);
        } else {
            return sqlSession.selectOne(namespaceId, args == null ? null : args[0]);
        }
    }
}
