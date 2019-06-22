package com.ljheee.ibatis.demo;

import com.ljheee.ibatis.session.SqlSession;
import com.ljheee.ibatis.session.SqlSessionFactory;

import java.util.List;

/**
 * 测试类
 */
public class Mian {
    public static void main(String[] args) {

        SqlSessionFactory factory = new SqlSessionFactory();

        SqlSession sqlSession = factory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = userMapper.selectUserById(10);
        System.out.println(user);

        List<User> users = userMapper.selectAll();
        System.out.println(users);


    }
}
