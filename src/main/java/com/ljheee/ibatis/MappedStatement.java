package com.ljheee.ibatis;

/**
 * 封装SQL语句
 */
public class MappedStatement {

    private String namespace;
    private String mapperId;//namespace+方法ID名，如 com.ljheee.dao.mapper.IUserDAO.selectUserById
    private String resultType;
    private String sql;

    public MappedStatement(String namespace, String mapperId, String resultType, String sql) {
        this.namespace = namespace;
        this.mapperId = mapperId;
        this.resultType = resultType;
        this.sql = sql;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMapperId() {
        return mapperId;
    }

    public void setMapperId(String mapperId) {
        this.mapperId = mapperId;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
