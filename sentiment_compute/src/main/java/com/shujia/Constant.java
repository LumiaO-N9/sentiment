package com.shujia;

import com.shujia.common.Config;

public class Constant {

    //计算性别占比checkpoint地址
    public static String GENDER_INDEX_CHECKPOINT = Config.getString("gender.index.checkpoint");
    //kafka  zookeeper连接地址
    public static String KAFKA_ZOOKEEPER_CONNECT = Config.getString("kafka.zookeeper.connect");
    //hbase  zookeeper连接地址
    public static String HBASE_ZOOKEEPER_CONNECT = Config.getString("hbase.zookeeper.connect");
    public static String REDIS_HOST = Config.getString("redis.host");

}
