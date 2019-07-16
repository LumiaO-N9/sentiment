package com.shujia.util;


import java.sql.Connection;
import java.sql.DriverManager;

/**
 * jdbc连接锅具
 */
public class JDBCUtil {


    private static String USERNAME;
    private static String PASSWORD;
    private static String URL;
    private static String DRIVER;

    /**
     *
     * 类加载的时候执行
     */
    static {
        DRIVER = Config.getString("MYSQL.DRIVER");
        //类加载，不需要每次都加载
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        URL = Config.getString("MYSQL.URL");
        USERNAME = Config.getString("MYSQL.USERNAME");
        PASSWORD = Config.getString("MYSQL.PASSWORD");

    }


    /**
     * 获取jdbc连接
     *
     * @return
     */

    public static Connection getConnection() {

        Connection connection = null;
        try {
            //2、建立连接
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return connection;

    }

}
