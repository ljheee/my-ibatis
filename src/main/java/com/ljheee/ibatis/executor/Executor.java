package com.ljheee.ibatis.executor;

/**
 * 执行器
 * 复制执行SQL
 * 需要提供CRUD API接口
 */
public interface Executor {

    <E> E query(String statement, Object parameter);
}
