package com.ljheee.ibatis.demo;

import java.util.List;

/**
 */
public interface UserMapper {

    User selectUserById(Integer id);

    User selectUserByIdAndName(Integer id, String name);

    User selectUserByParams(Integer id, String name, String passwd, String appid);

    List<User> selectAll();

}
