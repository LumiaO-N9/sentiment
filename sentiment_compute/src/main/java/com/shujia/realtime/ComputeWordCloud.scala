package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.Constant
import com.shujia.bean.ScalaClass.WeiboComment
import com.shujia.common.{IK, SparkTool}
import kafka.serializer.StringDecoder
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import redis.clients.jedis.Jedis

object ComputeWordCloud extends SparkTool {

  /**
    * 计算每个舆情词云图
    *
    */
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {


    //创建spark streaming上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(5))

    ssc.checkpoint(Constant.WORD_CLOUD_INDEX_CHECKPOINT)

    val params = Map(
      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
      "group.id" -> "asdadasdd",
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

    //1、对数据进行分词
    val wordsDS = commentDSKV.flatMap(com => {
      val sId = com.sentiment_id
      val words = IK.fit(com.text)

      //增加舆情编号
      words.map(word => sId + "_" + word)
    })

    //统计每个词出现的次数
    val countDS = wordsDS
      .map((_, 1))
      .updateStateByKey((seq: Seq[Int], opt: Option[Int]) => Some(seq.sum + opt.getOrElse(0)))



    //将结果写入redis
    countDS.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {
        val jedis = new Jedis(Constant.REDIS_HOST, 6379)
        iter.foreach(line => {
          val split = line._1.split("_")
          val sentimentId = split(0)
          val word = split(1)

          //数量
          val count = line._2

          val key = sentimentId + "_word_cloud"

          jedis.hset(key, word, count.toString)
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
