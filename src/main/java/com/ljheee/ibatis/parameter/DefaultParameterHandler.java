package com.ljheee.ibatis.parameter;


import com.ljheee.ibatis.MappedStatement;
import com.ljheee.ibatis.binding.MapperMethod;
import com.ljheee.ibatis.type.TypeHandler;
import com.ljheee.ibatis.type.TypeHandlerRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class DefaultParameterHandler implements ParameterHandler {

    private final TypeHandlerRegistry typeHandlerRegistry;

    private final MappedStatement mappedStatement;
    private final Object parameterObject;

    public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject) {
        this.mappedStatement = mappedStatement;
        this.typeHandlerRegistry = new TypeHandlerRegistry();
        this.parameterObject = parameterObject;
    }

    public Object getParameterObject() {
        return parameterObject;
    }

    // PreparedStatement预编译SQL已创建好，此处进行参数设置
    public void setParameters(PreparedStatement ps) throws SQLException {

        // 无参 的情况
        if(parameterObject == null){
            return;
        }
        Object parameter = parameterObject;
        MappedStatement ms = mappedStatement;
        Map<String, Object> param = null;
        if (parameter instanceof Map) {
            param = (Map<String, Object>) parameter;
        } else {
            //单个参数的情况
            param = new MapperMethod.ParamMap<Object>();
            param.put("param1", parameter);
            param.put("1", parameter);

        }
        int count = param.size() / 2;// 参数个数

        //遍历 把#{0}、#{1}，取得索引index，去map中get("param"+index)拿到对应的参数值
        // setParameter(ps, index, parameter)
        for (int i = 0; i < count; i++) {// 遍历设置每一个参数

            // 获取参数值
            Object value = param.get("param" + (i + 1));

            // 根据参数类型，获取对应的TypeHandler
            TypeHandler typeHandler = typeHandlerRegistry.getMappingTypeHandler(value.getClass());
            typeHandler.setParameter(ps, (i + 1), value, null);// ps.setXxx 真正的参数设置，延迟到具体子类
        }


    }

}
