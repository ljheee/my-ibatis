package com.ljheee.ibatis.session;

import com.ljheee.ibatis.Configuration;
import com.ljheee.ibatis.MappedStatement;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 */
public class SqlSessionFactory {

    private static final String MAPPER_CONFIG_LOCATION = "mapper";
    public static final String JDBC_LOCATION = "jdbc.properties";
    private Configuration configuration;

    public SqlSessionFactory() {
        loadDatabase();
        mappedStatement();
    }


    /**
     * 读取 数据库配置
     * 转配 Configuration
     */
    private void loadDatabase() {

        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(JDBC_LOCATION);
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String driver = properties.getProperty("driver");
        String userName = properties.getProperty("userName");
        String password = properties.getProperty("password");
        String url = properties.getProperty("url");
        configuration = new Configuration(driver, userName, password, url);
    }

    //加载指定文件夹下的所有mapper.xml
    private void mappedStatement() {
        URL resources = null;
        resources = SqlSessionFactory.class.getClassLoader().getResource(MAPPER_CONFIG_LOCATION);
        File mappers = new File(resources.getFile());//获取指定文件夹信息
        if (mappers.isDirectory()) {
            File[] listFiles = mappers.listFiles();
            //遍历文件夹下所有的mapper.xml，解析信息后，注册至conf对象中
            for (File file : listFiles) {
                loadMapperInfo(file);
            }
        } else {
            loadMapperInfo(mappers);
        }
    }

    // 解析mapper.xml，封装成一个个MappedStatement
    private void loadMapperInfo(File file) {
        SAXReader reader = new SAXReader();//mybatis也是用dom4j解析mapper.xml文件
        Document document = null;

        try {
            document = reader.read(file);
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        Element root = document.getRootElement();
        String namespace = root.attribute("namespace").getData().toString();// <mapper namespace="com.ljheee.dao.mapper.IUserDAO">

        List<Element> selects = root.elements("select");//还有 insert、update、delete节点
        List<Element> allElemnrts = new ArrayList<>();
        allElemnrts.addAll(selects);
        allElemnrts.addAll(root.elements("insert"));
        allElemnrts.addAll(root.elements("update"));
        allElemnrts.addAll(root.elements("delete"));
        for (Element element : allElemnrts) {
            String id = element.attribute("id").getData().toString();//就是方法名
            String resultType = element.attribute("resultType").getData().toString();
            String sql = element.getData().toString();
            String mapperId = namespace + "." + id;// com.ljheee.dao.mapper.IUserDAO.selectUserById
            MappedStatement ms = new MappedStatement(namespace, mapperId, resultType, sql);
            configuration.getMappedStatements().put(mapperId, ms);
        }

    }

    public SqlSession openSession() {
        return new DefaultSqlSession(configuration);
    }

}
