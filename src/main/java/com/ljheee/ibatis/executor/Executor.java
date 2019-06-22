package com.ljheee.ibatis.executor;

import com.ljheee.ibatis.MappedStatement;

import java.util.List;

/**
 * 执行器
 * 负责执行SQL
 * 需要提供CRUD API接口
 */
public interface Executor {

    <E> List<E> query(MappedStatement statement, Object parameter);
}
