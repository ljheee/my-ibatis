package com.ljheee.ibatis.session;

import java.util.List;

/**
 * SqlSession 接口
 * SqlSession是Mybatis对外提供数据访问的主要API
 * 意味着需要给外部提供CRUD接口
 */
public interface SqlSession {

    public <T> T selectOne(String statement, Object parameter);
    public <T> List<T> selectList(String statement, Object parameter);
    public <T> T getMapper(Class<?> clazz);
}
