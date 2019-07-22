package com.shujia.realtime

import java.text.SimpleDateFormat

import com.google.gson.Gson
import com.shujia.Constant
import com.shujia.bean.ScalaClass.WeiboComment
import com.shujia.common.{IK, SparkTool}
import com.shujia.realtime.ComputeWordCloud.sc
import kafka.serializer.StringDecoder
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.HConnectionManager
import org.apache.spark.ml.classification.NaiveBayesModel
import org.apache.spark.ml.feature.{HashingTF, IDFModel, Tokenizer}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import org.apache.spark.mllib.linalg.Vector
import redis.clients.jedis.Jedis
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes

object ComputeSentimentIndex extends SparkTool {
  /**
    * 实时情感打标
    *
    */
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {

    //创建spark streaming上下文对象
    val ssc = new StreamingContext(sc, Durations.seconds(5))

    ssc.checkpoint(Constant.SENTIMENT_INDEX_CHECKPOINT)

    val params = Map(
      "zookeeper.connect" -> Constant.KAFKA_ZOOKEEPER_CONNECT,
      "group.id" -> "asdasdaasddd",
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

    val s = sql
    import s.implicits._


    //情感打标
    var flagDS = commentDSKV.transform(rdd => {
      //分词
      val wordDF = rdd.map(comment => {
        var text = comment.text
        //分词
        comment.text = IK.fit(text).mkString(" ")

        comment
      }).toDF()


      //Tokenizer  英文分词器
      val tok = new Tokenizer()
        .setInputCol("text")
        .setOutputCol("feature")

      val tokDF = tok.transform(wordDF)


      //计算tf
      val tfModel = new HashingTF()
        //.setNumFeatures(262144) //设置特征数量,  值越大准确率越高   计算复杂度越高
        .setInputCol("feature")
        .setOutputCol("tf")

      val tfDF = tfModel.transform(tokDF)


      //计算idf
      //加载idf模型
      val idfModel = IDFModel.load("model/idfModel")

      val idfDF = idfModel.transform(tfDF)


      //情感打标
      //将数据带入贝叶斯模型

      //加载模型
      val nbModel = NaiveBayesModel.load("model/nbModel")

      //对数据进行打标
      val rsultDF = nbModel.transform(idfDF)
      rsultDF.printSchema()

      rsultDF.rdd
    })

    flagDS = flagDS.cache()

    //统计舆情
    val resultDS = flagDS.map(row => {
      val sentimentId = row.getAs[Long]("sentiment_id")
      //预测结果
      var prediction = row.getAs[Double]("prediction")
      //评价时间
      var created_at = row.getAs[String]("created_at")
      //正负标记的概率
      val probability = row.getAs[Vector]("probability")

      //计算两个概率的差值
      val p = math.abs(probability(0) - probability(1))

      //如果差值小于0.3评论为中性
      if (p < 0.3) {
        prediction = 2.0
      }
      created_at = created_at.substring(0, 13)

      val key = sentimentId + "_" + created_at + "_" + prediction

      (key, 1)
    })
      //统计舆情结果
      .updateStateByKey((seq: Seq[Int], opt: Option[Int]) => Some(seq.sum + opt.getOrElse(0)))
      .map(t => {
        val split = t._1.split("_")
        val sentimentId = split(0)
        val time = split(1)
        val flag = split(2)
        val count = t._2

        val key = sentimentId + "_" + time
        val value = flag + ":" + count

        (key, value)
      })
      //将结果转换格式
      /**
        * (1_2019-07-15 11,0.0:10|1.0:6)
        * (0_2019-07-20 18,0.0:3|1.0:2)
        * (1_2019-07-19 22,0.0:6|1.0:4)
        */
      .reduceByKey(_ + "|" + _)


    //将结果写入redis
    resultDS.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {

        //创建hbase连接
        val conf: Configuration = new Configuration
        conf.set("hbase.zookeeper.quorum", Constant.HBASE_ZOOKEEPER_CONNECT)
        val connection = HConnectionManager.createConnection(conf)
        //create 'comment_sentiment' ,{NAME => 'info' ,VERSIONS => 100}
        val sentiment = connection.getTable("comment_sentiment")

        iter.foreach(t => {
          val split = t._1.split("_")
          val sentimentId = split(0)
          //以时间作为版本号
          val time = split(1)
          val format = new SimpleDateFormat("yyyy-MM-dd HH")
          val ts = format.parse(time).getTime

          val put = new Put(sentimentId.getBytes())

          put.add("info".getBytes(), "real".getBytes(), ts, t._2.getBytes())

          sentiment.put(put)
        })

        connection.close()
      })
    })


    /**
      * 将打好标记的数据写入hbase 供下一次模型更新使用
      *
      */
    flagDS.foreachRDD(rdd => {

      rdd.foreachPartition(iter => {
        //创建hbase连接
        val conf: Configuration = new Configuration
        conf.set("hbase.zookeeper.quorum", Constant.HBASE_ZOOKEEPER_CONNECT)
        val connection = HConnectionManager.createConnection(conf)
        //create 'comment','info'
        val comment = connection.getTable("comment")

        iter.foreach(row => {
          val comment_id = row.getAs[String]("comment_id")
          val article_id = row.getAs[String]("article_id")
          val created_at = row.getAs[String]("created_at")
          val user_name = row.getAs[String]("user_name")
          val user_id = row.getAs[Long]("user_id")
          val total_number = row.getAs[Long]("total_number")
          val like_count = row.getAs[Long]("like_count")
          val text = row.getAs[String]("text")
          //预测结果
          var prediction = row.getAs[Double]("prediction")

          //正负标记的概率
          val probability = row.getAs[Vector]("probability")

          //计算两个概率的差值
          val p = math.abs(probability(0) - probability(1))

          val put = new Put(comment_id.getBytes())

          put.add("info".getBytes(), "created_at".getBytes(), created_at.getBytes())
          put.add("info".getBytes(), "article_id".getBytes(), article_id.getBytes())
          put.add("info".getBytes(), "user_name".getBytes(), user_name.getBytes())
          put.add("info".getBytes(), "user_id".getBytes(), Bytes.toBytes(user_id))
          put.add("info".getBytes(), "total_number".getBytes(), Bytes.toBytes(total_number))
          put.add("info".getBytes(), "like_count".getBytes(), Bytes.toBytes(like_count))
          put.add("info".getBytes(), "text".getBytes(), text.getBytes())
          put.add("info".getBytes(), "prediction".getBytes(), Bytes.toBytes(prediction))
          put.add("info".getBytes(), "p".getBytes(), Bytes.toBytes(p))

          comment.put(put)

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
  override def init(): Unit

  = {
    conf.setMaster("local[4]")

  }
}
