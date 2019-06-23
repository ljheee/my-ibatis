

#### 多个参数的参数化
v2版本实现简单参数化，就是硬编码ps.setXxx(1,arg[0])，只能设置单个参数。
对于多个参数的如何实现呢？

先来看看，mybatis对于Mapper接口中多参数的方法，如何使用的
mybatis parameterType 多个参数
https://blog.csdn.net/lixld/article/details/77980443
- 单个参数时，可用parameterType 指定参数类型。
- 多个参数时，不需要写parameterType参数，
    select t.* from tableName where id = #{0} and name = #{1} 用#{index}是第几个就用第几个的索引，索引从0开始。

那我们手写mybatis，要实现多个参数的 参数化，似乎可用这种索引的方式（#{0}、#{1}）；
通过解析完mapper.xml后、封装的MappedStatement，可以得到SQL及里面的#{0}、#{1}……，遍历这些需要预编译设置参数值的占位符，
把执行XxxMapper.selectUserById()方法的参数一一set到SQL中，就能完成SQL参数化了。但prepareStatement.setXxx(idx,value)必须知道参数类型，才能确定setXxx是setInt还是setString。

如何获取每一个参数的类型呢？
spring中有LocalVariableTableParameterNameDiscoverer，是利用ASM解析字节码，jdk8新增了API反射获取方法参数名。
想到MappedStatement 中保存全路径接口名和方法名，知道了具体要执行的方法，参数类型就可以从XxxMapper.selectUserById()方法中的形参类型中获取(method.getParameterTypes())。

下面立马动手试一试


```java
public class DefaultExecutor implements Executor {

    private Configuration configuration;

    public DefaultExecutor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter) {


        Method method  = null;
        try {
            Class<?> aClass = Class.forName(ms.getNamespace());
            int idx = ms.getMapperId().lastIndexOf(".");
            String methodName = ms.getMapperId().substring(idx + 1);

            Method[] declaredMethods = aClass.getDeclaredMethods();
            for (int j = 0; j < declaredMethods.length; j++) {
                if (methodName.equals(declaredMethods[j].getName())) {
                    // 重载的方法，
                    method = declaredMethods[j];
                    Class<?>[] parameterTypes = declaredMethods[j].getParameterTypes();
                    System.out.println(parameterTypes);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < args.length; i++) {
            Object value = parameterTypes[i].cast(args);
            if (parameterTypes[i] == Integer.class) {
                ps.setInt(i+1,value);
            } else if (parameterTypes[i] == String.class) {
                ps.setString(i+1,value);
            } else {
                // ……
            }
        }

        
        List<E> ret = new ArrayList<>();

        // 执行SQL查询
        // 处理结果

        return ret;
    }


}

```
发现对于重载的方法，是无法实现的。
因为mybatis的Mapper接口，允许我们定义重载方法，意味着方法名一样，需要通过参数列表才能确定重载的方法。
如果不考虑重载的方法，这种方式是简单有效的，直接通过getParameterTypes参数类型，确定ps.setXxx的类型。但不考虑重载方法，是不现实的。

这条路行不通，下面调试下源码，看mybatis是如何实现的。
mybatis的做法是：根据每个参数类型 args[0].getClass()，进行相应的参数设置 （如LongTypeHandler ps.setLong(i, parameter)）
可以根据参数类型，获取具体的TypeHandler。而这种对应关系，都保存在`org.apache.ibatis.type.TypeHandlerRegistry`
里面保存的对应关系如下：
```
    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());
```
其他类型也是类型，都是保存在HashMap中。

再回头看看我们v2.0版本实现的单个参数的参数化：
```
    // 参数化，硬编码ps.setXxx(1,arg[0])，只能设置单个参数；对于多个参数还未实现
    private void parameterize(PreparedStatement ps, Object parameter) throws SQLException {
        if (parameter instanceof Integer) {
            ps.setInt(1, (Integer) parameter);
        } else if (parameter instanceof Long) {
            ps.setLong(1, (Long) parameter);
        } else if (parameter instanceof String) {
            ps.setString(1, (String) parameter);
        } else {
            // ...
        }
    }
```
也是根据参数类型，进行具体的ps.setXxxx()的设置。虽说Java基本类型是确定的8种，但这种长长的if-else，还是有点……

mybatis的优化方法就是，为每种类型实现一个对应的XxxTypeHandler，而XxxTypeHandler中最重要的方法setParameter()就是执行相应的ps.setXxx.
参数类型、及其对应的TypeHandler 都保存在 TypeHandlerRegistry类中，下面是自己简化后的 TypeHandlerRegistry：
```java
public final class TypeHandlerRegistry {

    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();

    public TypeHandlerRegistry() {

        register(Integer.class, new IntegerTypeHandler());
        register(int.class, new IntegerTypeHandler());
        register(String.class, new StringTypeHandler());
        //...
    }

    public TypeHandler<?> getMappingTypeHandler(Class<?> handlerType) {
        return ALL_TYPE_HANDLERS_MAP.get(handlerType);
    }

    public void register(Class<?> type, TypeHandler<?> handler) {
        ALL_TYPE_HANDLERS_MAP.put(type, handler);
    }

}
```
是不是非常简洁<^_^>
就是保存 不同类型、及其对应的 TypeHandler。具体的TypeHandler 实现也非常简单，就是完成对应类型ps.setXxxx，如IntegerTypeHandler
```java
public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setInt(i, parameter);
  }

  @Override
  public Integer getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getInt(columnName);
  }

  @Override
  public Integer getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getInt(columnIndex);
  }

  @Override
  public Integer getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return cs.getInt(columnIndex);
  }
}
```
其他类型不用举例，想必大伙也都能知道个大概。

有了不同类型、及其对应的 TypeHandler，实现参数处理，就容易多了，下面看DefaultParameterHandler的实现：

```java
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

        //...

        //遍历 把#{0}、#{1}，取得索引index，去map中get("param"+index)拿到对应的参数值
        for (int i = 0; i < count; i++) {// 遍历设置每一个参数

            // 获取参数值
            Object value = param.get("param" + (i + 1));

            // 根据参数类型，获取对应的TypeHandler
            TypeHandler typeHandler = typeHandlerRegistry.getMappingTypeHandler(value.getClass());
            typeHandler.setParameter(ps, (i + 1), value, null);// ps.setXxx 真正的参数设置，延迟到具体子类
        }
    }
}
```
在DefaultParameterHandler 的构造方法中将预编译好的PreparedStatement 和参数对象传入，在setParameters()方法完成所有参数的设置，就是遍历获取每一个参数，根据参数类型获取到对应的TypeHandler，让具体的TypeHandler完成ps.setXxxx.

###### 自己的思考
- 如果把所有Java类型，都实现相应的TypeHandler，感觉有些繁多；为精简起见，我就实现了两个具代表性的int和string类型。
- 原先自己实现的parameterize()方法，实现不同的参数设置，是没问题的，就是if-else太长；mybatis针对这个问题、为了以后的扩展升级进行了相应的优化。
个人感觉，自己手写模仿实现，能实现功能，知道原设计者在面对问题时是如何改进的即可，不需要钻到里面写个一模一样的。


###### 参数对象的传递
mybatis的SqlSession接口，对应了所有的用户操作接口，如下
```
<T> T selectOne(String statement, Object parameter);
<E> List<E> selectList(String statement, Object parameter);
```
发现第二个参数对象，就是一个Object parameter，那对于多个参数的情况，如何得到适用呢？
如UserMapper.selectUserByIdAndName(Integer id, String name);查询条件有两个，接口对应了两个参数，但SqlSession接口定义的查询方法，参数只有一个Object！！
原理就在MapperMethod.MethodSignature，通过MethodSignature将参数转化为Map。
在执行UserMapper.selectUserByIdAndName()时，被动态代理MapperProxy.invoke()拦截执行，invoke()方法的第三个参数Object[] args,可以获得selectUserByIdAndName()的所有参数值【实参】。
然后把Object[] args参数数组，转化成HashMap保存，作为Object parameter一个Object参数、一直传递到SqlSession.selectList()。
SqlSession.selectList()并不真正执行SQL，而是委托给Executor完成，在Executor执行SQL之前，构造好预编译的mappedStatement、并且把用Map保存的参数对象parameterObject 作为参数，创建一个 DefaultParameterHandler 完成SQL参数设置，参数设置完就能执行了，执行完SQL、再进行结果集的处理，这就是mybatis动态SQL执行过程。

下面来看通过MethodSignature将参数转化为Map的源码：

```
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

```
最终实现的效果是，用HashMap保存了每个参数对应的实参值，param0-value0、param1-value1....现实键值对，
在进行 param.put(param{i}, args[i]);时，mybatis的实现并不是直接遍历Object[] args 实参数组，而是在创建MapperMethod时，在MapperMethod的构造方法中同时创建了MethodSignature。
MethodSignature作为静态内部类，完成convertArgsToSqlCommandParam()参数转化。MethodSignature的另外一个作用就是完成参数绑定。
我们在Mapper接口中定义的方法有多个参数时，会用@Param注解标识别名，如getUser(@Param("id") Integer id,@Param("name") String userName);
如果不用@Param注解，在mapper.xml中，SQL的参数就需要像我们上面那样用索引方式，select t.* from tableName where id = #{0} and name = #{1}

下面我们来看mybatis如何解析的：
```
        // mybatis获取参数名，是通过@Param注解 实现的参数绑定
        private String getParamNameFromAnnotation(Method method, int i, String paramName) {
            final Object[] paramAnnos = method.getParameterAnnotations()[i];
            for (Object paramAnno : paramAnnos) {
                if (paramAnno instanceof Param) {
                    paramName = ((Param) paramAnno).value();
                }
            }
            return paramName;
        }
```
在创建MethodSignature时，构造方法参数传入了invoke()的method参数，用method可以反射获取方法参数上的注解，得到@Param注解里绑定的参数名。
由于我们只是手写实现mybatis核心部分，就是打算使用SQL参数 索引方式，所以了解了mybatis的参数绑定，我们的实现还是按简单的#{0}、#{1}方式。

另外补充一点，jdk8实现了运行时反射获取方法参数名的新API，之前大部分框架都是用类似的注解实现参数名称绑定，如springMVC的@RequestParam，有了jdk8的支持，参数绑定的方式可以有所改善了。



jdk8 获取方法形参名 
https://www.liaoxuefeng.com/article/992594806963488 
需要开启javac -parameters编译参数，IDEA设置-parameters编译参数方法：https://juejin.im/post/5a01c4086fb9a044fd112d94
试了好像还是不行，获取参数名arg0。

IDEA设置编译参数
https://juejin.im/post/5a01c4086fb9a044fd112d94




###### SQL的处理
SQL的处理，其实分为两步：
- 将mapper.xml中的SQL(ms.getSql()) 获取处理，把#{0}、#{1}...替换成 ？，用于创建预编译的prepareStatement=connection.prepareStatement(preparedSql);
- 为预编译后的prepareStatement，进行设置参数 ps.setInt()等。

第一步解析#{0}替换？
```
        String sql = "select * from user where id=#{0} and name=#{1}";
        for (int i = 0; i < args.length; i++) {
            sql = sql.replace("#{" + i + "}", "?");
        }
        System.out.println(sql);//select * from user where id=? and name=?
```
采用普通的字符串替换，目前可以简单实现；就是把#{0}作为整体，replace成?。
后来仔细想想这样做存在的问题，发现：
当我们不小心在mapper.xml中写SQL，写成`select * from user where id=#{ 0} and name=#{1 }`
括号里的变量或参数名 多出了空格，这时候，这种简单的字符串替换，就不能胜任了。如何把#{ 0}或#{0 }，replace成?呢！

然后再想，有什么办法解决这个问题呢？在mapper.xml写SQL变量的时候，多个空格再正常不过了，mybatis如何解决的呢？
除了想到正则表达式和spring的SpEL外，目前不知道还能有什么好方法。因为手写的参数，根本不知道开发者在#{ 0}中加了几个空格，是加在前面还是后面了、这些都不知道。
突然想到，对于其他异常的情况，mybatis又是如何处理的，如在#和{之间也加上空格：
便在另外一个工程，试了下，如果在#和{之间也加上空格，执行过程会报错“无法设置参数”，并且在MyBatis3.4.4版不能直接使用#{0}，而要使用 #{arg0}，也就是要写成`select * from user_info where id=#{arg0} and name=#{arg1}`
大胆猜想，mybatis解析SQL，一定是#{作为 beginToken，}作为结束。
不出所料，很快找到了mybatis处理SQL占位符替换的代码：`org.apache.ibatis.parsing.GenericTokenParser`

mybatis解析#{0}替换
GenericTokenParser就是个普通的class，既没有用正则表达式，也没有用什么EL表达式语言解析，就是纯粹的字符串处理。
通过两层while循环，不断定位beginToken(#{)、及其对应的closeToken(})，对里面的参数进行替换，用offset保存扫描过的字符，下次定位(indexOf)都是基于当前offset偏移的。
其实现就是：找到开始字符和结束字符的下标位置，把这段截取出来作为一个expression，expression这个变量保存的就是每一次需要替换成?的#{0}占位符。
比如`select * from user where id=#{ 0}`，定位到#{的下标，及其对应的}下标，这个区间所有字符截取出来作为expression，把expression替换成?，就这样完成了，无论#{ 0}里有多少个空格，空格是在前面还是加在后面了都不care，因为截取了#{ 0}这一段，整体替换成?了。
下面演示下GenericTokenParser的效果：
```
       // org.apache.ibatis.parsing.GenericTokenParser
        GenericTokenParser genericTokenParser = new GenericTokenParser("#{", "}", new TokenHandler() {
            @Override
            public String handleToken(String s) {
                return "?";     // expression替换成?
            }
        });
        String text = genericTokenParser.parse("select * from user_info where id=#{ 0 } and name=#{  1}");
        System.out.println(text);// select * from user_info where id=? and name=?
```


手写一遍的意义。
意在用简练的代码，还原最底层的原理。
要手写实现一个功能类似的“产品”，实现功能的过程、手写一遍的过程，一定会遇到困难；
遇到这些困难，自己有什么解决思路和方法，如果按自己的实现，能完美解决吗？原框架设计者又是如何设计实现的，好在哪？如果以后自己实现一个新“产品”，从这些框架设计思想，能不能有一些启发。



mybatis动态设置参数 分析
https://blog.csdn.net/Mr_SeaTurtle_/article/details/73053103











