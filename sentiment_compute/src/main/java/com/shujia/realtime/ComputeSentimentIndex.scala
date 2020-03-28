package com.shujia.realtime

import java.io.IOException
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util

import com.google.gson.Gson
import com.sendgrid.{Content, Email, Mail, Method, Request, SendGrid}
import com.shujia.Constant
import com.shujia.bean.ScalaClass.WeiboComment
import com.shujia.common.{IK, JDBCUtil, SparkTool}
import kafka.serializer.StringDecoder
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.HConnectionManager
import org.apache.spark.ml.classification.NaiveBayesModel
import org.apache.spark.ml.feature.{HashingTF, IDFModel, Tokenizer}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import org.apache.spark.mllib.linalg.Vector
import redis.clients.jedis.Jedis
import org.apache.hadoop.hbase.client.Put
import org.apache.kafka.clients.consumer.ConsumerConfig

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

    val kafkaParams = Map(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> Constant.KAFKA_BOOTSTRAP_SERVERS,
      ConsumerConfig.GROUP_ID_CONFIG -> "successS13",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "smallest"
    )
    /**
     * direct模式  主动拉取kafka数据   用户数据
     *
     */
    val commentDS = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc,
      kafkaParams,
      Set("WeiBoCommentTopic")
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
    val flagDS = commentDSKV.transform(rdd => {
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
      //      rsultDF.printSchema()

      rsultDF.rdd
    })

    flagDS.cache()

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


    //将结果写入HBase
    resultDS.foreachRDD(rdd => {
      rdd.foreachPartition(iter => {

        //创建hbase连接
        val conf: Configuration = new Configuration
        conf.set("hbase.zookeeper.quorum", Constant.HBASE_ZOOKEEPER_CONNECT)
        val connection = HConnectionManager.createConnection(conf)
        //create 'comment_sentiment' ,{NAME => 'info' ,VERSIONS => 100}
        val sentiment = connection.getTable("comment_sentiment")

        // 创建MySQL连接
        val mySQLConn: Connection = JDBCUtil.getConnection()
        val sql = "select * from tb_sentiment where id = ?";
        val stat = mySQLConn.prepareStatement(sql)
        //创建redis连接
        val jedis = new Jedis(Constant.REDIS_HOST, 6379)

        //舆情预警开关
        val switchFlag = if (jedis.get("AlertSwitch").toInt == 1) {
          true
        } else {
          false
        }
        iter.foreach(t => {
          val split = t._1.split("_")
          val sentimentId = split(0)
          //以时间作为版本号
          val time = split(1)
          val format = new SimpleDateFormat("yyyy-MM-dd HH")
          val ts = format.parse(time).getTime
          val value = t._2

          val put = new Put(sentimentId.getBytes())

          put.add("info".getBytes(), "real".getBytes(), ts, value.getBytes())

          sentiment.put(put)

          // 舆情预警 如果负面评论超过30%则触发邮件
          if (switchFlag) {
            val map: util.HashMap[String, Integer] = new util.HashMap[String, Integer]
            map.put("0.0", 0)
            map.put("1.0", 0)
            map.put("2.0", 0)
            for (kv <- value.split("\\|")) {
              val k = kv.split(":")(0)
              val v = kv.split(":")(1).toInt
              map.put(k, v)
            }
            val total = map.get("0.0") + map.get("1.0") + map.get("2.0")
            val percent = map.get("0.0") / total
            if (percent >= 0.3) {
              stat.setInt(1,sentimentId.toInt)
              val resultSet = stat.executeQuery()
              var id = sentimentId
              var name = ""
              while (resultSet.next()) {
                id = resultSet.getInt("id").toString
                name = resultSet.getString("name")
              }
              val from = new Email("xiaoBei@sentiment.com")
              val subject = "微博舆情分析平台预警"
              val to = new Email("1585659554@qq.com")
              val url = s"http://localhost:8080/sentimentinfo.do?id=$sentimentId"
              val text =
                s"""<strong>舆情："${name}" 在${time}点的负面评论占比已超过阈值，请及时处理！<a href="$url" target="
              _blank">$url</a></strong>"""
              val content = new Content("text/html", text)
              val mail = new Mail(from, subject, to, content)

              val sg = new SendGrid(Constant.SENDGRID_APIKEY)
              val request = new Request()
              try {
                request.setMethod(Method.POST)
                request.setEndpoint("mail/send")
                request.setBody(mail.build)
                val response = sg.api(request)
                System.out.println(response.getStatusCode)
                System.out.println(response.getBody)
                System.out.println(response.getHeaders)
              } catch {
                case ex: IOException =>
                  throw ex
              }
            }
          }

        })
        stat.close()
        mySQLConn.close()
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
          val article_url = s"https://m.weibo.cn/status/$article_id"
          val created_at = row.getAs[String]("created_at")
          val user_name = row.getAs[String]("user_name")
          val user_id = row.getAs[Long]("user_id")
          val total_number = row.getAs[Long]("total_number")
          val like_count = row.getAs[Long]("like_count")
          val text = row.getAs[String]("text")
          //预测结果
          val prediction = row.getAs[Double]("prediction")

          //正负标记的概率
          val probability = row.getAs[Vector]("probability")

          //计算两个概率的差值
          val p = math.abs(probability(0) - probability(1))

          val put = new Put(comment_id.getBytes())

          put.add("info".getBytes(), "created_at".getBytes(), created_at.getBytes())
          put.add("info".getBytes(), "article_id".getBytes(), article_id.getBytes())
          put.add("info".getBytes(), "article_url".getBytes(), article_url.getBytes())
          put.add("info".getBytes(), "user_name".getBytes(), user_name.getBytes())
          put.add("info".getBytes(), "user_id".getBytes(), user_id.toString.getBytes())
          put.add("info".getBytes(), "total_number".getBytes(), total_number.toString.getBytes())
          put.add("info".getBytes(), "like_count".getBytes(), like_count.toString.getBytes())
          put.add("info".getBytes(), "text".getBytes(), text.getBytes())
          put.add("info".getBytes(), "prediction".getBytes(), prediction.toString.getBytes())
          put.add("info".getBytes(), "p".getBytes(), p.toString.getBytes())

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
  override def init(): Unit = {
    conf.setMaster("local[4]")

  }
}
