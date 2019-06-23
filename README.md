
#### 手写my-batis
`目前通过不断完善，实现的各个分支`
- https://github.com/ljheee/my-ibatis/tree/my-ibatis1.0
- https://github.com/ljheee/my-ibatis/tree/my-ibatis2.0
- https://github.com/ljheee/my-ibatis/tree/my-ibatis3.0

###### 3.0版本
`新增实现：实现MapperMethod.MethodSignature，通过MethodSignature将参数转化为Map；
使用TypeHandlerRegistry保存不同Java类型、及其对应的TypeHandler，并用DefaultParameterHandler完成参数设置。`
- 实现多参数的SQL执行；
- 在MapperProxy 中拦截Mapper方法执行后，创建MapperMethod(和内部类MethodSignature)；
- MapperProxy 拦截Mapper方法执行后，在MethodSignature中完成参数数组Object[] args到Map的转换；
- TypeHandlerRegistry保存不同Java类型、及其对应的TypeHandler，取代对不同类型的参数进行if-else类型判断；
- DefaultParameterHandler完成参数设置，遍历需要设置的参数，根据参数类型找到对应的TypeHandler 完成ps.setXxx

###### 3.0版本后存在的问题
- 只能查询，增删改还未实现
- 开发者定义的Mapper接口，由MapperProxy 创建动态代理后，如何托管给spring容器；


###### 2.0版本
`新增实现：dom4j解析mapper.xml并构造实现Configuration配置类，使用反射+ResultSetMetaData实现结果集处理。`
- 实现ID查询记录,和selectAll全表查询；
- 实现Configuration配置类，用于保存mapperId(sourceId) 和MappedStatement的映射；使用dom4j解析mapper.xml并构造实现Configuration；
- 获取SQL后，实现简单参数化；参数化，硬编码ps.setXxx(1,arg[0])，只能设置单个参数，对于多个参数还未实现；
- 实现ResultSet结果集的处理，handleResultSet()使用反射+ResultSetMetaData 取代硬编码；
- 数据库连接connection，是直接创建获取的。



###### 还存在的问题
- mapper.xml XML文件的语法格式检查，如<setect id="selectUserById">必须指定resultType，必须有id；
- mapper.xml文件和对应的Mapper接口的正确性检查。