package com.ljheee.ibatis.session;

import java.util.List;

/**
 * SqlSession 接口
 * SqlSession是Mybatis对外提供数据访问的主要API
 * 意味着需要给外部提供CRUD接口
 */
public interface SqlSession {

    <T> T selectOne(String statement, Object parameter);

    <T> List<T> selectList(String statement, Object parameter);

    int insert(String statement, Object parameter);

    int update(String statement, Object parameter);

    int delete(String statement, Object parameter);

    <T> T getMapper(Class<?> clazz);
}
