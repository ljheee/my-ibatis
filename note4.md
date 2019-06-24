##### my-ibatis 4.0
本4.0版本，重点解决3.0之后的问题“只能查询，增删改还未实现”。
目前实现了查询功能（单个参数、无参和多个查询参数），并能对SQL中的#{0}、#{1}进行参数替换，在已有的功能前提下，实现‘增删改’还是不难的。
MapperProxy拦截到Mapper接口执行的方法，构造MethodSignature将Object[] args参数数组转化为Map，让executor执行SQL就行了，executor执行SQL之前的参数化功能是相同的，直接复用DefaultParameterHandler即可。

重点是如何区分Mapper接口中执行的方法，是select查询，还是insert、update、delete增删改？
大致分析一下，之前的版本在实现查询时，是直接在MapperProxy.invoke()方法 调用了sqlSession.selectList或sqlSession.selectOne实现查询，如果要实现insert、update、delete增删改，大概也应该在这个地方实现，可以根据SQL语句字符串前缀，解析出是insert、update、delete的哪一种。然后也是调用sqlSession对应insert、update、delete的方法，因为SqlSession是暴露给用户可直接操作的接口，增删改查的API都在此接口定义了。

新增SqlCommandType枚举，对应SQL类型
```java
public enum SqlCommandType {
  UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH;
}
```
就是用于区分SQL是增删改查的哪一种。
MapperProxy.invoke方法，之前只实现了查询：
```
public class MapperProxy<T> implements InvocationHandler {

    //...
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 参数转化
        MapperMethod mapperMethod = new MapperMethod(clazz, method, sqlSession.getConfiguration());
        Object param = mapperMethod.getMethod().convertArgsToSqlCommandParam(args);

        Class<?> returnType = method.getReturnType();
        String namespaceId = clazz.getName() + "." + method.getName();
        if (Collection.class.isAssignableFrom(returnType)) {
            return sqlSession.selectList(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
        } else {
            return sqlSession.selectOne(namespaceId, args == null ? null : param);// 用转化后的参数，替换之前硬编码args[0]
        }
    }

    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = new MapperMethod(clazz, method, sqlSession.getConfiguration());// 用hashmap缓存 mapperMethod
        return mapperMethod;
    }
}
```
ORM框架除了查询功能，就是重要的增删改；需要在这里对SQL类型进行判断，再调用sqlSession具体执行。
具体SQL判断的逻辑，实现在MapperMethod中，因此MapperProxy.invoke方法直接调用MapperMethod.execute()
```
public class MapperProxy<T> implements InvocationHandler {
    //...
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }

    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = new MapperMethod(clazz, method, sqlSession.getConfiguration());// 用hashmap缓存 mapperMethod
        return mapperMethod;
    }
}
```
创建MapperMethod的时候，new MapperMethod(clazz, method, sqlSession.getConfiguration())构造方法里面创建了一个内部类对象SqlCommand：
```
public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(method);
}
```
SqlCommand就是实现SQL的类型区分；另一个对象MethodSignature再之前版本已经实现、分析过了，就是完成参数转成Map。
SqlCommand就两个属性，一个是SqlCommandType枚举，标识SQL的DML类型；另一个属性name保存mapperId（namespaceId+方法名），就是MappedStatement保存的mapperId，获取到MappedStatement就能获得。
下面来看SqlCommand的实现：
```java
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
```
区分SQL类型，用了简单的取巧方式，因为insert、update、delete、select都是6个字符，取出MappedStatement保存的SQL字符串，去除空格后，取出0~5这前6个字符，就是insert、update、delete、select；再用switch判断，设置SQL类型。

MapperMethod内部创建的 SqlCommand 准备好了，下面来看MapperMethod.execute()方法:
```java
public class MapperMethod {

    private SqlCommand command;
    private MethodSignature method;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method);
        this.method = new MethodSignature(method);
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
    //...
}
```
MapperMethod.execute()方法是SqlSession执行的入口，SqlSession 是Mybatis对外提供数据访问的主要API，mybatis的SqlSession接口定义了增删改查的所有接口，用户可以直接用于数据操作。

之前自己定义的SqlSession，只包含查询的接口，补充定义：
```
int insert(String statement, Object parameter);
int update(String statement, Object parameter);
int delete(String statement, Object parameter);
```
insert、update、delete接口参数和selectList的参数一样，都是一个statement、一个Object的parameter对象；至于为什么是这俩参数，之前在3.0版本已经提及&解析过了。
SqlSession 并不复杂SQL真正的执行，实际都是委托给 executor真正执行SQL。
想到这里，我先行一步，在Executor中也定义了增删改的方法；并在 sqlSession 的实现类DefaultSqlSession中，对新增接口进行实现，是调用executor的方法。
等这么实现完之后，发现mybatis的Executor，只定义了查询和update的方法，没有insert和delete，在 DefaultSqlSession中的insert和delete也是调用executor.update。

**静坐片刻，想想为什么呢？**
insert 和 delete在底层executor执行SQL时，均用的update方式。
这也能理解，毕竟在很久之前，没有用ORM框架的时候，手写JDBC执行SQL时，insert、update、delete语句的执行都是prepareStatement.execute();
在执行prepareStatement.execute()之前，我们需要做的是用预编译的SQL创建prepareStatement，然后为prepareStatement设置参数，然后就能prepareStatement.execute()执行了。
实际执行prepareStatement.execute()，DBMS并不需要知道是增加了，还是修改、删除了数据，这些DBMS并不需要关心；对DBMS来说，这就是一条DML，对数据产生了有变化的影响，返回受影响的行数rowCount。
想到这里，不禁为mybatis团队点个案，会抽象、善于观察、有深度。于是我修改了自己实现的DefaultSqlSession：
```
@Override
public int update(String statement, Object parameter) {
    MappedStatement ms = configuration.getMappedStatements().get(statement);
    return executor.update(ms, parameter);
}

@Override
public int insert(String statement, Object parameter) {
    MappedStatement ms = configuration.getMappedStatements().get(statement);
    return executor.update(ms, parameter);  // insert 和delete在底层executor执行SQL时，均用的update方式
}

@Override
public int delete(String statement, Object parameter) {
    MappedStatement ms = configuration.getMappedStatements().get(statement);
    return executor.update(ms, parameter);
}
```
可以看到，SqlSession对外暴露的insert、update、delete增删改的API，均是调用executor.update真正执行。
在Executor新增调用update方法：
```java
public interface Executor {

    <E> List<E> query(MappedStatement statement, Object parameter);

    //insert、update、delete语句的执行，都是通过 Executor.update
    int update(MappedStatement ms, Object parameter);
}
```
在Executor的实现类DefaultExecutor中，进行实现：
```java
public class DefaultExecutor implements Executor {

    // insert、update、delete 实现

    @Override
    public int update(MappedStatement ms, Object parameter) {
        int result = 0;
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
        Connection connection = null;
        PreparedStatement prepareStatement = null;
        try {
            connection = getConnection();
            prepareStatement = connection.prepareStatement(preparedSql);

            // 参数化
            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(ms, parameter);
            parameterHandler.setParameters(prepareStatement);

            // 执行SQL
            prepareStatement.execute();
            result = prepareStatement.getUpdateCount();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
```
Object parameter参数对象，之前分析过，就是在 MapperProxy中创建的MapperMethod.MethodSignature完成的参数转成Map。
有了这个Map，直接复用 DefaultParameterHandler 完成参数设置，parameterHandler.setParameters(prepareStatement)内部就是根据每一个参数值的类型，进行对应的ps.setXxx参数设置。
完成参数化，就能SQL执行prepareStatement.execute()，并获取受影响的行数了prepareStatement.getUpdateCount()。
就这样增删改功能了，有很多东西能够慢慢连贯起来，顺利执行的过程，是不是很好<^_^>

###### 增删改 功能测试
之前的功能只实现了查询，在SqlSessionFactory初始化时，用dom4j只解析了<select>查询的节点`List<Element> selects = root.elements("select");`，
SqlSessionFactory新增对`<insert>、<update>、<delete>`节点元素的解析。

完成之后，在自己用于测试的 UserMapper 中新增增删改的接口：
```java
public interface UserMapper {

    User selectUserById(Integer id);

    void saveUser(Integer id, String name, String passwd, String appid);
    int updateUserById(Integer id);
    void deleteUserById(Integer id);

    User selectUserByIdAndName(Integer id, String name);
    User selectUserByParams(Integer id, String name, String passwd, String appid);
    List<User> selectAll();
}
```
saveUser、updateUserById、deleteUserById对应的UserMapper.xml中的SQL：
```
<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.ljheee.ibatis.demo.UserMapper">

    <!-- insert、update、delete的测试-->
    <update id="updateUserById" resultType="java.lang.Integer">
        update user
        set passwd='hello'
        where id =#{0}
    </update>

    <delete id="deleteUserById" resultType="java.lang.Void">
        delete from user
        where id =#{0}
    </delete>

    <insert id="saveUser" resultType="java.lang.Void">
        insert into user
        values(#{0} , #{1} , #{2} , #{3})
    </insert>
</mapper>
```
测试类，测试方法均测试通过。
```java
/**
 * 测试类
 * 演示 手写mybatis的执行效果
 */
public class Mian {
    public static void main(String[] args) {

        SqlSessionFactory factory = new SqlSessionFactory();

        SqlSession sqlSession = factory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        // insert、update、delete的测试
        System.out.println("==================================");
        int count = userMapper.updateUserById(11);
        System.out.println(count);// 1
        System.out.println(userMapper.selectUserById(11));// User{id=11, name='ljh', passwd='hello', appid='95955542780'}

        userMapper.saveUser(6,"ljheee","password","appid");
        System.out.println(userMapper.selectUserById(6));

        userMapper.deleteUserById(6);
        System.out.println(userMapper.selectUserById(6));// null
    }
}
```
保存insert插入记录的方法，定义成了`void saveUser(Integer id, String name, String passwd, String appid)`参数传入的方式，而非`void saveUser(User user)`传入对象在SQL中取对象属性；
目前实现的是SQL参数用索引的方式#{0}、#{1}，还未考虑对象属性的解析。

