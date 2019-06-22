
#### 手写my-batis
###### 1.0版本
`实现：mybatis简易框架，SqlSession、Executor和MapperProxy`
- 实现ID查询记录：参数化都是硬编码；
- 没有Configuration，用于保存mapperId(sourceId) 和MappedStatement的映射；而是采用硬编码 HashMap保存映射。
- DB的转化 也是硬编码的。
- 数据库连接connection，是直接创建获取的。