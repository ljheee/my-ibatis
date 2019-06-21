package com.ljheee.ibatis.executor;

import com.ljheee.ibatis.demo.User;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * Created by lijianhua04 on 2018/10/6.
 */
public class SimpleExecutor implements Executor {

    private static String driver = null;
    private static String userName = null;
    private static String password = null;
    private static String url = null;

    public static final String JDBC_LOCATION = "jdbc.properties";


    static {
        try {
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(JDBC_LOCATION);
            Properties properties = new Properties();
            properties.load(stream);

            driver = properties.getProperty("driver");
            userName = properties.getProperty("userName");
            password = properties.getProperty("password");
            url = properties.getProperty("url");

            Class.forName(driver);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public <E> E query(String sql, Object parameter) {

        try {
            // 此处使用 format设置参数
            sql = String.format(sql, Integer.parseInt(String.valueOf(parameter)));
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            // TODO  parameterize() 参数设置 ,最终应该是ps.setInt(1, parameter)
            // 参数个数 1-n不确定，要获取每个参数类型，使用相应的setInt/setString
            ResultSet rs = ps.executeQuery();

            User user = null;
            while (rs.next()) {

                //DB的转化 是硬编码的
                int id = rs.getInt("id");// 表中的列名，列的数据类型，
                String appid = rs.getString("appid");
                String name = rs.getString("name");
                String password = rs.getString("passwd");
                user = new User(id, name, password, appid);
            }
            return (E) user;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, userName, password);
        return conn;
    }


}
