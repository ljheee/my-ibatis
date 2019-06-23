package com.ljheee.ibatis;

/**
 * Created by lijianhua04 on 2019/6/22.
 */
public class Test {


    public static void main(String[] args) {

        String sql = "select * from user where id=#{0} and name=#{1}";

        Object[] argss = new Object[2];
        argss[0] = 1;
        argss[1] = "ljheee";
//        for (int i = 0; i < argss.length; i++) {
//            sql = sql.replace("#{" + i + "}", argss[i].toString());
//        }
//        System.out.println(sql);// select * from user where id=1 and name=ljheee


        for (int i = 0; i < argss.length; i++) {
            sql = sql.replace("#{" + i + "}", "?");
        }
        System.out.println(sql);//select * from user where id=? and name=?

    }
}
