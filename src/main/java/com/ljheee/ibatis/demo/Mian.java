package com.ljheee.ibatis.demo;

import com.ljheee.ibatis.session.DefaultSqlSession;
import com.ljheee.ibatis.session.SqlSession;

/**
 *
 */
public class Mian {
    public static void main(String[] args) {

        SqlSession sqlSession = new DefaultSqlSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = mapper.selectUserById(10);
        System.out.println(user);

    }
}
