package com.ljheee.ibatis.demo;

import java.util.List;

/**
 */
public interface UserMapper {

    User selectUserById(Integer id);

    void saveUser(Integer id, String name, String passwd, String appid);
    int updateUserById(Integer id);
    void deleteUserById(Integer id);

    User selectUserByIdAndName(Integer id, String name);

    User selectUserByParams(Integer id, String name, String passwd, String appid);

    List<User> selectAll();

}
