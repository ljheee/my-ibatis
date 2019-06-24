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


        // insert、update、delete的测试
//        System.out.println("==================================");
//        int count = userMapper.updateUserById(11);
//        System.out.println(count);// 1
//        System.out.println(userMapper.selectUserById(11));// User{id=11, name='ljh', passwd='hello', appid='95955542780'}
//
//        userMapper.saveUser(6,"ljheee","password","appid");
//        System.out.println(userMapper.selectUserById(6));
//
//        userMapper.deleteUserById(6);
//        System.out.println(userMapper.selectUserById(6));// null


    }
}
