package com.ljheee.ibatis.session;

import com.ljheee.ibatis.Configuration;
import com.ljheee.ibatis.MappedStatement;
import com.ljheee.ibatis.MapperProxy;
import com.ljheee.ibatis.executor.DefaultExecutor;
import com.ljheee.ibatis.executor.Executor;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * SqlSession 是指挥官
 * 左手configuration，保存 全局配置
 * 右手executor，完成实际SQL执行
 */
public class DefaultSqlSession implements SqlSession {


    private Configuration configuration;
    private Executor executor;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
        this.executor = new DefaultExecutor(configuration);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        List<T> selectList = this.selectList(statement, parameter);
        if (selectList == null || selectList.size() == 0) {
            return null;
        } else if (selectList.size() == 1) {
            return selectList.get(0);
        }
        throw new RuntimeException("too many results");
    }

    @Override
    public <T> List<T> selectList(String statement, Object parameter) {
        MappedStatement mappedStatement = configuration.getMappedStatements().get(statement);
        return executor.query(mappedStatement, parameter);
    }

    @Override
    public <T> T getMapper(Class<?> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new MapperProxy<T>(this, clazz));
    }

}
