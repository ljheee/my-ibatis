package com.ljheee.ibatis.binding;

import com.ljheee.ibatis.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 *
 */
public class MapperMethod {

    private MethodSignature method;

//    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
//        this.command = new SqlCommand(config, mapperInterface, method);
//        this.method = new MethodSignature(config, method);
//    }

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.method = new MethodSignature(method);
    }

    public MethodSignature getMethod() {
        return method;
    }

    // 通过MethodSignature将参数转化为Map
    public static class MethodSignature {

        private final SortedMap<Integer, String> params;

        public MethodSignature(Method method) {
            this.params = Collections.unmodifiableSortedMap(getParams(method));
        }

        //参数转化
        public Object convertArgsToSqlCommandParam(Object[] args) {
            final int paramCount = params.size();
            if (args == null || paramCount == 0) {
                return null;
            } else if (paramCount == 1) {
                return args[params.keySet().iterator().next()];// 单个参数时，这单个参数值 作为object返回
            } else {
                final Map<String, Object> param = new ParamMap<Object>();
                int i = 0;
                for (Map.Entry<Integer, String> entry : params.entrySet()) {
                    param.put(entry.getValue(), args[entry.getKey()]);// 形参名-参数值

                    final String genericParamName = "param" + String.valueOf(i + 1);
                    if (!param.containsKey(genericParamName)) {
                        param.put(genericParamName, args[entry.getKey()]);
                    }
                    i++;
                }
                return param;
            }
        }


        /**
         * 获取方法 形参名
         * 按参数顺序，构成TreeMap：key为参数索引，value为形参名
         *
         * @param method
         * @return
         */
        private SortedMap<Integer, String> getParams(Method method) {
            final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();
                params.put(i, paramName);// jdk8 获取方法形参名 https://www.liaoxuefeng.com/article/992594806963488 需要开启javac -parameters编译参数
            }
            return params;
        }

        // mybatis获取参数名，是通过@Param注解 实现的参数绑定
//        private String getParamNameFromAnnotation(Method method, int i, String paramName) {
//            final Object[] paramAnnos = method.getParameterAnnotations()[i];
//            for (Object paramAnno : paramAnnos) {
//                if (paramAnno instanceof Param) {
//                    paramName = ((Param) paramAnno).value();
//                }
//            }
//            return paramName;
//        }


    }


    public static class ParamMap<V> extends HashMap<String, V> {
        private static final long serialVersionUID = -2212268410512043556L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new RuntimeException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }

    }

}
