package com.ljheee.ibatis.binding;

import com.ljheee.ibatis.Configuration;
import com.ljheee.ibatis.MappedStatement;
import com.ljheee.ibatis.SqlCommandType;
import com.ljheee.ibatis.session.SqlSession;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 *
 */
public class MapperMethod {

    private SqlCommand command;
    private MethodSignature method;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method);
        this.method = new MethodSignature(method);
    }

    public MethodSignature getMethod() {
        return method;
    }


    /**
     * SqlSession执行的入口
     * 实际都是委托给 executor真正执行SQL
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {

        Object result = null;
        switch (command.getType()) {
            case INSERT: {
                Object param = method.convertArgsToSqlCommandParam(args);// 参数转成Map
                result = rowCountResult(sqlSession.insert(command.getName(), param));
                break;
            }
            case UPDATE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.update(command.getName(), param));
                break;
            }
            case DELETE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.delete(command.getName(), param));
                break;
            }
            case SELECT:
                Object param = method.convertArgsToSqlCommandParam(args);
                String namespaceId = command.getName();

                Class<?> returnType = method.getReturnType();
                if (Collection.class.isAssignableFrom(returnType)) {
                    result = sqlSession.selectList(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
                } else {
                    result = sqlSession.selectOne(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
                }
                break;

            default:
                throw new RuntimeException("Unknown execution method for: " + command.getName());
        }
        return result;
    }

    private Object rowCountResult(int rowCount) {

        final Object result;
        if (Void.class.equals(method.getReturnType()) || Void.TYPE.equals((method.getReturnType()))) {
            result = null;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            result = rowCount;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            result = (long) rowCount;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            result = rowCount > 0;
        } else {
            throw new RuntimeException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
        }
        return result;
    }

    // 通过MethodSignature将参数转化为Map
    public static class MethodSignature {

        private final SortedMap<Integer, String> params;
        private Class<?> returnType;

        public MethodSignature(Method method) {
            this.returnType = method.getReturnType();
            this.params = Collections.unmodifiableSortedMap(getParams(method));
        }

        public Class<?> getReturnType() {
            return returnType;
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

    public static class SqlCommand {
        private final String name;// 就是mapperId,保存namespaceId，Mapper接口名+方法名
        private final SqlCommandType type;

        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            final String methodName = method.getName();
            final Class<?> declaringClass = method.getDeclaringClass();

            MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
            if (ms == null) {
                throw new RuntimeException("Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
            }
            this.name = ms.getMapperId();

            // 取SQL的前6个字符；这里走的取巧的方式，因为insert、update、delete、select都是6个字符
            String sql = ms.getSql().trim().substring(0, 6);
            switch (sql) {
                case "insert":
                case "INSERT":
                    this.type = SqlCommandType.INSERT;
                    break;

                case "update":
                case "UPDATE":
                    this.type = SqlCommandType.UPDATE;
                    break;

                case "delete":
                case "DELETE":
                    this.type = SqlCommandType.DELETE;
                    break;

                case "select":
                case "SELECT":
                    this.type = SqlCommandType.SELECT;
                    break;

                default:
                    this.type = SqlCommandType.UNKNOWN;
                    break;
            }
        }

        public String getName() {
            return name;
        }

        public SqlCommandType getType() {
            return type;
        }

        private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                                                       Class<?> declaringClass, Configuration configuration) {
            String statementId = mapperInterface.getName() + "." + methodName;
            return configuration.getMappedStatements().get(statementId);
        }
    }
}
