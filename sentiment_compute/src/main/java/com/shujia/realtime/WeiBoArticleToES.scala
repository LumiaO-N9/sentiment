package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.Constant
import com.shujia.common.SparkTool
import kafka.serializer.StringDecoder
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}

import com.shujia.bean.ScalaClass.WeiBoArticle

object WeiBoArticleToES extends SparkTool {
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {

    //1、创建spark streaming 上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(5))

    val params = Map(
      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
      "group.id" -> "asdaasdsdasf",
      "auto.offset.reset" -> "smallest",
      "zookeeper.connection.timeout.ms" -> "10000"
    )
    val topics = Map("ArticleItemTopic" -> 4)

    //读取kafka数据   评价表数据
    val articlesDS: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
      ssc, params, topics, StorageLevel.MEMORY_AND_DISK_SER
    )

    //将数据写入ES
    articlesDS.foreachRDD(rdd => {

      //将json数据转换成自定义对象
      val caseRDD = rdd.map(_._2).map(line => {
        val gson = new Gson()
        gson.fromJson(line, classOf[WeiBoArticle])
      })

      //导入隐式转换
      import org.elasticsearch.spark._
      //es.mapping.id  如果不指定，默认随机分配一个id   必须唯一
      caseRDD.saveToEs("article/fulltext", Map("es.mapping.id" -> "id"))
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

    //指定es配置
    conf.set("cluster.name", "my-application")
    conf.set("es.index.auto.create", "true")
    conf.set("es.nodes", "node1,node2,node3")
    conf.set("es.index.read.missing.as.empty", "true")
    conf.set("es.nodes.wan.only", "true")
  }
}
