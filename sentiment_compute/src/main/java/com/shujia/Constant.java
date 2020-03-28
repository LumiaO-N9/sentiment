package com.shujia;

import com.shujia.common.Config;

public class Constant {

    //计算性别占比checkpoint地址
    public static String GENDER_INDEX_CHECKPOINT = Config.getString("gender.index.checkpoint");
    public static String ARTICLE_COMMENTS_COUNT_CHECKPOINT = Config.getString("article.comments.count.checkpoint");
    //词云图checkpoing地址
    public static String WORD_CLOUD_INDEX_CHECKPOINT = Config.getString("word.cloud.index.checkpoint");

    public static String SENTIMENT_INDEX_CHECKPOINT = Config.getString("sentiment.index.checkpoint");
    //kafka  zookeeper连接地址
    public static String KAFKA_ZOOKEEPER_CONNECT = Config.getString("kafka.zookeeper.connect");
    public static String KAFKA_BOOTSTRAP_SERVERS = Config.getString("kafka.bootstrap.servers");
    //hbase  zookeeper连接地址
    public static String HBASE_ZOOKEEPER_CONNECT = Config.getString("hbase.zookeeper.connect");
    public static String REDIS_HOST = Config.getString("redis.host");
    public static String SENDGRID_APIKEY = Config.getString("sendgrid.api.key");
    // 邮件预警配置
    public static String FROM_EMAIL = Config.getString("from.email");
    public static String TO_EMAILS = Config.getString("to.emails");
    public static String SUBJECT = Config.getString("subject");


}
