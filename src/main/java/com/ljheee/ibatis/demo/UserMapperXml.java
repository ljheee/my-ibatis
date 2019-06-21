package com.ljheee.ibatis.demo;

import java.util.HashMap;
import java.util.Map;

/**
 * mapper.xml
 * <p>
 * 需要记录的信息：
 * 1、数据库表 所有列名
 * 2、列 的数据类型
 * 3、列名 和POJO 属性的映射
 */
public class UserMapperXml {

    public static final String NAME_SPACE = "com.ljheee.ibatis.demo.UserMapper";

    public static Map<String, String> methodSqlMapping = new HashMap<String, String>();

    static {
        methodSqlMapping.put("selectUserById", "SELECT * FROM user where id =%d");
    }

    public static String getMethodSQL(String method) {
        return methodSqlMapping.get(method);
    }

}
