package com.shujia.schduler;

import com.shujia.common.Config;
import com.shujia.common.JDBCUtil;
import redis.clients.jedis.Jedis;

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchedulerUrl {


    /**
     * 查询数据库，每隔5分钟调度一次，将url写入redis
     */
    public static void main(String[] args) throws Exception {


        String baseUrl = "https://m.weibo.cn/api/container/getIndex?containerid=100103type%3D60%26q%3D$1%26t%3D0&page_type=searchall&page=$2";


        while (true) {

            //创建redis连接

            Jedis jedis = new Jedis(Config.getString("redis.host"), 6379, 100000);
            /**
             *
             * 连接数据获取舆情信息
             */

            //1、创建数据库连接
            Connection connection = JDBCUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from tb_sentiment");
            //获取数据
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String words = resultSet.getString("words");
                Integer id = resultSet.getInt("id");

                System.out.println(id + "\t" + name + "\t" + words);


//                //构建url
//                for (String word : words.split(",")) {

                //获取多个页面数据
                for (int page = 0; page < 10; page++) {
                    //构建url
                    String url = baseUrl
                            .replace("$1", URLEncoder.encode(words))
                            .replace("$2", String.valueOf(page));

                    System.out.println(url);

                    //将url写入redis
                    String key = "weibo_search_spider:start_urls";
                    jedis.lpush(key, url);

                    //将url和舆情id的对应关系写入redis
                    jedis.hset("url_flag", url, String.valueOf(id));

                }
            }
//            }


            jedis.close();
            //没5分钟执行一次
            Thread.sleep(5 * 60 * 1000);
        }


    }
}
