package com.ljheee.ibatis;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration 贯穿全局（全局唯一）
 *
 * 保存mapperId(sourceId) 和MappedStatement的映射。
 */
public class Configuration {

    private String driver = null;
    private String userName = null;
    private String password = null;
    private String url = null;

    private Map<String,MappedStatement> mappedStatements = new HashMap<>();

    public Configuration() {
    }

    public Configuration(String driver, String userName, String password, String url) {
        this.driver = driver;
        this.userName = userName;
        this.password = password;
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, MappedStatement> getMappedStatements() {
        return mappedStatements;
    }

    public void setMappedStatements(Map<String, MappedStatement> mappedStatements) {
        this.mappedStatements = mappedStatements;
    }
}
