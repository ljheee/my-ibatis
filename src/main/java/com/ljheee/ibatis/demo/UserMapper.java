package com.ljheee.ibatis.demo;

import java.util.List;

/**
 */
public interface UserMapper {

    User selectUserById(Integer id);
    List<User> selectAll();

}
