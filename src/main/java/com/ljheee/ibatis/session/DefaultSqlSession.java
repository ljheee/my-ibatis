package com.ljheee.ibatis.session;

import com.ljheee.ibatis.MapperProxy;
import com.ljheee.ibatis.executor.Executor;
import com.ljheee.ibatis.executor.SimpleExecutor;

import java.lang.reflect.Proxy;

/**
 * Created by lijianhua04 on 2018/10/6.
 */
public class DefaultSqlSession implements SqlSession {


    private Executor executor = new SimpleExecutor();


    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return executor.query(statement, parameter);
    }

    @Override
    public <T> T getMapper(Class<?> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new MapperProxy<T>(this, clazz));

    }

}
