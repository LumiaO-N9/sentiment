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

    val userparams = Map(
      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
      "group.id" -> "asdasdas",
      "auto.offset.reset" -> "smallest",
      "zookeeper.connection.timeout.ms" -> "10000"
    )

    val usertopics = Map("WeiBoUserItemTopic" -> 4)

    //读取kafka数据   评价表数据
    val userDS: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
      ssc, userparams, usertopics, StorageLevel.MEMORY_AND_DISK_SER
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

        iter.foreach(user => {
          //构建put对象  ，以用户id作为rowkey
          val put = new Put(Bytes.toBytes(user.id))
          put.add("info".getBytes(), "description".getBytes(), user.description.getBytes())
          put.add("info".getBytes(), "screen_name".getBytes(), user.screen_name.getBytes())
          put.add("info".getBytes(), "follow_count".getBytes(), Bytes.toBytes(user.follow_count))
          put.add("info".getBytes(), "followers_count".getBytes(), Bytes.toBytes(user.followers_count))
          put.add("info".getBytes(), "gender".getBytes(), user.gender.getBytes())
          //插入数据
          WeiBoUser.put(put)
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
