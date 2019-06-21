package com.ljheee.ibatis.session;

/**
 * SqlSession 接口
 */
public interface SqlSession {

    public <T> T selectOne(String statement, Object parameter);


    public <T> T getMapper(Class<?> clazz);
}
