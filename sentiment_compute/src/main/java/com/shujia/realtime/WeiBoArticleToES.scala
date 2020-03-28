package com.shujia.realtime

import com.google.gson.Gson
import com.shujia.Constant
import com.shujia.common.SparkTool
import kafka.serializer.StringDecoder
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import com.shujia.bean.ScalaClass.WeiBoArticle
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.elasticsearch.spark.sql._

object WeiBoArticleToES extends SparkTool {
  /**
   * 在run方法里面编写spark业务逻辑
   */
  override def run(args: Array[String]): Unit = {

    //创建spark streaming 上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(60))

    //读取kafka数据   评价表数据

    val kafkaParams = Map(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> Constant.KAFKA_BOOTSTRAP_SERVERS,
      ConsumerConfig.GROUP_ID_CONFIG -> "success2",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "smallest"
    )
    /**
     * direct模式  主动拉取kafka数据   文章数据
     *
     */
    val articlesDS = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc,
      kafkaParams,
      Set("ArticleItemTopic")
    )
    val SparkSQL = sql
    import SparkSQL.implicits._
    //将数据写入ES
    articlesDS.foreachRDD(rdd => {

      //将json数据转换成自定义对象
      val caseRDD = rdd.map(_._2).map(line => {
        val gson = new Gson()
        gson.fromJson(line, classOf[WeiBoArticle])
      }).toDF()
      val cfg = Map(
        ("es.resource", "article/fulltext"),
        ("es.mapping.id", "id")
      )
      caseRDD.saveToEs(cfg)
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
    conf.set("cluster.name", "bigdata-es")
    conf.set("es.index.auto.create", "true")
    conf.set("es.nodes", "master,node1,node2")
    conf.set("es.port", "9200")
    conf.set("es.index.read.missing.as.empty", "true")
    conf.set("es.nodes.wan.only", "true")
    conf.set("es.mapping.date.rich", "false")
  }
}
