package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.bean.ScalaClass.{WeiBoUser, WeiboComment}
import com.shujia.common.SparkTool
import kafka.serializer.StringDecoder
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Get, HConnectionManager}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import redis.clients.jedis.Jedis
import com.shujia.Constant

object ComputeGenderIndex extends SparkTool {

  /**
    * 计算每个舆情评价人性别占比
    *
    */
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {


    //创建spark streaming上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(5))

    ssc.checkpoint(Constant.GENDER_INDEX_CHECKPOINT)

    val params = Map(
      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
      "group.id" -> "asdasaaasdsasd",
      "auto.offset.reset" -> "smallest",
      "zookeeper.connection.timeout.ms" -> "10000"
    )
    val topics = Map("WeiBoCommentTopic" -> 4)

    //读取kafka数据   评价表数据
    val commentDS: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
      ssc, params, topics, StorageLevel.MEMORY_AND_DISK_SER
    )

    val commentDSKV = commentDS.map(line => {
      val gson = new Gson()
      //将json字符串转换成自定义对象
      val comment = gson.fromJson(line._2, classOf[WeiboComment])
      comment
    })

    //重hbase获取用户性别
    val KVDS = commentDSKV.transform(rdd => {
      val newRDD = rdd.mapPartitions(iter => {

        //创建hbase连接
        val conf: Configuration = new Configuration
        conf.set("hbase.zookeeper.quorum", Constant.HBASE_ZOOKEEPER_CONNECT)
        val connection = HConnectionManager.createConnection(conf)
        val WeiBoUser = connection.getTable("WeiBoUser")

        val list = iter.map(comment => {
          //用户id
          val userId = comment.user_id
          //舆情编号
          val sentiment_id = comment.sentiment_id

          //通过用户id查询用户性别
          val get = new Get(Bytes.toBytes(userId))
          //指定需要查询的列
          get.addColumn("info".getBytes(), "gender".getBytes())
          val restltSet = WeiBoUser.get(get)
          val gender = Bytes.toString(restltSet.getValue("info".getBytes(), "gender".getBytes()))

          (sentiment_id + "_" + gender, 1)

        })

        list

      })

      newRDD
    })

    def fun = (seq: Seq[Int], opt: Option[Int]) => {
      //当前batch结果
      val curr = seq.sum
      //之前batch的结果
      val last = opt.getOrElse(0)

      Some(curr + last)
    }

    //计算评价性别人数
    val resultDS = KVDS.updateStateByKey(fun)


    //将结果写入redis

    resultDS.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {
        val jedis = new Jedis(Constant.REDIS_HOST, 6379)
        iter.foreach(line => {
          val split = line._1.split("_")
          val sentimentId = split(0)
          val gender = split(1)

          //人数
          val count = line._2

          val key = sentimentId + "_gender"

          jedis.hset(key, gender, count.toString)
        })
        jedis.close()
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
