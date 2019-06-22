
#### 手写my-batis
###### 2.0版本
`新增实现：dom4j解析mapper.xml并构造实现Configuration配置类，使用反射+ResultSetMetaData实现结果集处理。`
- 实现ID查询记录,和selectAll全表查询；
- 实现Configuration配置类，用于保存mapperId(sourceId) 和MappedStatement的映射；使用dom4j解析mapper.xml并构造实现Configuration；
- 获取SQL后，实现简单参数化；参数化，硬编码ps.setXxx(1,arg[0])，只能设置单个参数，对于多个参数还未实现；
- 实现ResultSet结果集的处理，handleResultSet()使用反射+ResultSetMetaData 取代硬编码；
- 数据库连接connection，是直接创建获取的。

###### TODO
- 完善参数化的设置，考虑多个参数时的参数化。

###### 还存在的问题
- mapper.xml XML文件的语法格式检查，如<setect id="selectUserById">必须指定resultType，必须有id；
- mapper.xml文件和对应的Mapper接口的正确性检查。