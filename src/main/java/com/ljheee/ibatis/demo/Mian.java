package com.ljheee.ibatis.demo;

import com.ljheee.ibatis.session.SqlSession;
import com.ljheee.ibatis.session.SqlSessionFactory;

import java.util.List;

/**
 * 测试类
 * 演示 手写mybatis的执行效果
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

        User user1 = userMapper.selectUserByIdAndName(10, "abc");
        System.out.println(user1);

        user1 = userMapper.selectUserByParams(10, "abc", "123", "95955542783");
        System.out.println(user1);


    }
}
