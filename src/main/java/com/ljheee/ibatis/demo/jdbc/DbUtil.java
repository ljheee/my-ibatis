package com.ljheee.ibatis.demo.jdbc;

import com.ljheee.ibatis.demo.User;

import java.sql.*;

/**
 * 最 原始jdbc 操作数据库方式
 */
public class DbUtil {

    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String userName = "root";
    private static String password = "12345678";
    private static String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false";

    static Connection conn = null;

    static {
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, userName, password);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static User selectById(Integer id) {
        User user = null;

        try {
            String sql = "SELECT * FROM user where id =?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1,id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                int userId = rs.getInt("id");// 表中的列名，列的数据类型，
                String appid = rs.getString("appid");
                String name = rs.getString("name");
                String password = rs.getString("passwd");
                user = new User(userId, name, password, appid);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

        }
        return user;

    }

    public static void main(String[] args) {
        System.out.println(DbUtil.selectById(11));
    }


}
