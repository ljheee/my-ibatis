package com.ljheee.ibatis.executor;

import com.ljheee.ibatis.Configuration;
import com.ljheee.ibatis.MappedStatement;
import com.ljheee.ibatis.binding.MapperMethod;
import com.ljheee.ibatis.util.StringUtil;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/*
        try {
            Class<?> aClass = Class.forName(ms.getNamespace());
            int idx = ms.getMapperId().lastIndexOf(".");
            String methodName = ms.getMapperId().substring(idx + 1);

            Method[] declaredMethods = aClass.getDeclaredMethods();
            for (int j = 0; j < declaredMethods.length; j++) {
                if (methodName.equals(declaredMethods[j].getName())) {
                    // 重载的方法，
                    Class<?>[] parameterTypes = declaredMethods[j].getParameterTypes();
                    System.out.println(parameterTypes);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // mybatis的做法是：根据每个参数类型 args[0].getClass()，进行相应的参数设置 （如LongTypeHandler ps.setLong(i, parameter);）

        Method method  = null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[4];
        for (int i = 0; i < 4; i++) {
            Object cast = parameterTypes[i].cast(args);
            if (parameterTypes[i] == Integer.class) {
                ps.setInt(i+1,cast);
            }
        }
*/

        Map<String, Object> param = null;
        if (parameter instanceof Map) {
            param = (Map<String, Object>) parameter;
        } else if(parameter == null){
            param = new HashMap<>();// 无参 的情况
        } else {
            //单个参数的情况
            param = new MapperMethod.ParamMap<Object>();
            param.put("param1", parameter);
            param.put("1", parameter);

        }
        int count = param.size() / 2;


        String sql = ms.getSql();
        String preparedSql = sql;

        //解析SQL，ms.getSql()把#{0}、#{1}...替换成 ？
        for (int i = 0; i < count; i++) {
            preparedSql = preparedSql.replace("#{" + i + "}", "?");
        }

        List<E> ret = new ArrayList<>();
        Connection connection = null;
        PreparedStatement prepareStatement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            prepareStatement = connection.prepareStatement(preparedSql);

            // 参数化
//            parameterize(prepareStatement, parameter);

            //遍历 把#{0}、#{1}，取得索引index，去map中get("param"+index)拿到对应的参数值
            // setParameter(ps, index, parameter)
            for (int i = 0; i < count; i++) {       // 把这个for循环注释掉，用下面两行替换也可
                Object value = param.get("param" + (i + 1));
                parameterize(prepareStatement, (i + 1), value);
            }
//            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(ms, parameter);
//            parameterHandler.setParameters(prepareStatement);

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

    // 参数化，此处只列举了常用类型
    private void parameterize(PreparedStatement ps, int index, Object parameter) throws SQLException {

        if (parameter instanceof Integer) {
            ps.setInt(index, (Integer) parameter);
        } else if (parameter instanceof Long) {
            ps.setLong(index, (Long) parameter);
        } else if (parameter instanceof String) {
            ps.setString(index, (String) parameter);
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
