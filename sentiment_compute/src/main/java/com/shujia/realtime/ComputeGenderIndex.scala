package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.bean.ScalaClass.WeiboComment
import com.shujia.common.SparkTool
import kafka.serializer.StringDecoder
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}

object ComputeGenderIndex extends SparkTool {

  /**
    * 计算每隔舆情评价人性别占比
    *
    */
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {


    //创建spark streaming上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(5))

    val params = Map(
      "zookeeper.connect" -> "node1:2181,node2:2181,node3:2181",
      "group.id" -> "asdasasasdddasd",
      "auto.offset.reset" -> "smallest",
      "zookeeper.connection.timeout.ms" -> "10000"
    )

    val topics = Map("WeiBoCommentTopic" -> 4)

    //读取kafka数据   评价表数据
    val commentDS: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
      ssc, params, topics, StorageLevel.MEMORY_AND_DISK_SER
    )


    commentDS.map(line => {
      val gson = new Gson()
      //将json字符串转换成自定义对象
      val comment = gson.fromJson(line._2, classOf[WeiboComment])
      comment
    }).print()


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
