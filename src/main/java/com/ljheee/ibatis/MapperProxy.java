package com.ljheee.ibatis;

import com.ljheee.ibatis.demo.UserMapperXml;
import com.ljheee.ibatis.session.DefaultSqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * MapperProxy MyBatis真正执行CRUD的就是该代理
 */
public class MapperProxy<T> implements InvocationHandler {


    private DefaultSqlSession sqlSession;
    Class<?> mapper;

    public MapperProxy(DefaultSqlSession sqlSession, Class<?> clazz) {
        this.sqlSession = sqlSession;
        this.mapper = clazz;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            try {
                return method.invoke(this, args);
            } catch (Throwable var5) {
                throw var5;
            }
        } else {

            String methodName = method.getName();// 就是sql id
            if (UserMapperXml.NAME_SPACE.equals(mapper)) {
                String sql = UserMapperXml.methodSqlMapping.get(methodName);
            }


//            Class<?> aClass = Class.forName(mapper.getName() + "Xml");
//            Field declaredField = aClass.getDeclaredField("methodSqlMapping");
//            System.out.println("declaredField="+declaredField);

            String sql = UserMapperXml.methodSqlMapping.get(methodName);
            return sqlSession.selectOne(sql, args[0]);//硬编码 只传了一个参数
        }
    }
}
