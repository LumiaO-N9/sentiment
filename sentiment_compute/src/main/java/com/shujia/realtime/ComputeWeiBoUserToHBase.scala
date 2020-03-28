package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.bean.ScalaClass.{WeiBoUser, WeiboComment}
import com.shujia.common.SparkTool
import kafka.serializer.StringDecoder
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.HConnectionManager
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import com.shujia.Constant
import org.apache.kafka.clients.consumer.ConsumerConfig
import redis.clients.jedis.Jedis

object ComputeWeiBoUserToHBase extends SparkTool {

  /**
   * 微博用户表写入hbase
   *
   */
  /**
   * 在run方法里面编写spark业务逻辑
   */
  override def run(args: Array[String]): Unit = {

    //创建spark streaming上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(2))

    //    val userparams = Map(
    //      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
    //      "group.id" -> "adfasdfasdffa1df",
    //      "auto.offset.reset" -> "largest",
    //      "zookeeper.connection.timeout.ms" -> "10000"
    //    )
    //    val usertopics = Map("WeiBoUserItemTopic" -> 4)

    //读取kafka数据   评价表数据
    //    val userDS: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
    //      ssc, userparams, usertopics, StorageLevel.MEMORY_AND_DISK_SER
    //    )
    val kafkaParams = Map(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> Constant.KAFKA_BOOTSTRAP_SERVERS,
      ConsumerConfig.GROUP_ID_CONFIG -> "success1",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "smallest"
    )
    /**
     * direct模式  主动拉取kafka数据   用户数据
     *
     */
    val userDS = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc,
      kafkaParams,
      Set("WeiBoUserItemTopic")
    )

    //用户id为key的DS
    val userDSKV = userDS.map(line => {
      val gson = new Gson()
      //将json字符串转换成自定义对象
      val user = gson.fromJson(line._2, classOf[WeiBoUser])
      user
    })

    /**
     * 先创建hbaae  表
     *
     * create "WeiBoUser","info"
     *
     */

    //将数据写入hbase
    userDSKV.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {

        //创建hbase连接
        val conf: Configuration = new Configuration
        conf.set("hbase.zookeeper.quorum", Constant.HBASE_ZOOKEEPER_CONNECT)
        val connection = HConnectionManager.createConnection(conf)
        val WeiBoUser = connection.getTable("WeiBoUser")

        //创建redis连接
        val jedis = new Jedis(Constant.REDIS_HOST, 6379)

        iter.foreach(user => {
          val user_id = user.id
          val sentiment_id = user.sentiment_id
          //构建put对象  ，以 用户id+舆情id 作为rowkey
          val id = user_id + "_" + sentiment_id
          val put = new Put(Bytes.toBytes(id))
          put.add("info".getBytes(), "description".getBytes(), user.description.getBytes())
          put.add("info".getBytes(), "screen_name".getBytes(), user.screen_name.getBytes())
          put.add("info".getBytes(), "follow_count".getBytes(), Bytes.toBytes(user.follow_count))
          put.add("info".getBytes(), "followers_count".getBytes(), Bytes.toBytes(user.followers_count))
          put.add("info".getBytes(), "gender".getBytes(), user.gender.getBytes())
          //插入数据
          WeiBoUser.put(put)

          // 将用户名即用户ID还有粉丝数量写入redis 待根据粉丝数求TopN
          val key = sentiment_id + "_followers_TopN"
          val followers_count = user.followers_count
          val idAndNameAndFollowers = user.id + "|" + user.screen_name
          jedis.zadd(key, followers_count, idAndNameAndFollowers)
          //          jedis.hset(key, nameAndId, followers_count.toString)
        })

        connection.close()

      })
    })


    ssc.start()
    ssc.awaitTermination()
    ssc.stop()


  }

  /**
   * 初始化spark配置
   *  conf.setMaster("local")
   */
  override def init(): Unit = {
    conf.setMaster("local[4]")
  }

}
