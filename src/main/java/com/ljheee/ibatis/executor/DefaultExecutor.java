package com.ljheee.ibatis.executor;

import com.ljheee.ibatis.Configuration;
import com.ljheee.ibatis.MappedStatement;
import com.ljheee.ibatis.util.StringUtil;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Executor默认实现类
 */
public class DefaultExecutor implements Executor {

    private Configuration configuration;

    public DefaultExecutor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter) {

        List<E> ret = new ArrayList<>();
        Connection connection = null;
        PreparedStatement prepareStatement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            prepareStatement = connection.prepareStatement(ms.getSql());

            // 参数化
            parameterize(prepareStatement, parameter);

            // 执行SQL查询
            resultSet = prepareStatement.executeQuery();

            // 处理结果
            handleResultSet(resultSet, ret, ms.getResultType());

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return ret;
    }

    // 参数化，硬编码ps.setXxx(1,arg[0])，只能设置单个参数；对于多个参数还未实现
    private void parameterize(PreparedStatement ps, Object parameter) throws SQLException {

        if (parameter instanceof Integer) {
            ps.setInt(1, (Integer) parameter);
        } else if (parameter instanceof Long) {
            ps.setLong(1, (Long) parameter);
        } else if (parameter instanceof String) {
            ps.setString(1, (String) parameter);
        }
    }

    /**
     * 结果集 处理
     * 使用反射+ResultSetMetaData
     *
     * @param rs
     * @param list
     * @param resultType
     * @param <E>
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private <E> void handleResultSet(ResultSet rs, List<E> list, String resultType) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Class<E> clazz = (Class<E>) Class.forName(resultType);
        Field[] fields = clazz.getDeclaredFields();
        while (rs.next()) {
            //把每行记录 封装成一个POJO
            E entity = clazz.newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            //遍历结果集 这条数据的每个列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);//优先使用getColumnLabel，防止同名的列 在封装POJO时 字段被覆盖
                if (null == columnName || columnName.length() <= 0) {
                    columnName = metaData.getColumnName(i);
                }

                Object value = rs.getObject(i);
                //遍历找出POJO 和这个列名相符的field，并进行设值
                for (Field field : fields) {
                    if (StringUtil.camelCaseToUnderscore(field.getName()).equalsIgnoreCase(columnName)) {
                        field.setAccessible(true);
                        field.set(entity, value);
                        break;
                    }
                }
            }
            list.add(entity);
        }
    }


    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(configuration.getUrl(), configuration.getUserName(), configuration.getPassword());
        return conn;
    }
}
