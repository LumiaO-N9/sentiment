package com.shujia.train

import com.shujia.common.SparkTool
import com.shujia.common.IK
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.classification.NaiveBayes

object ModelTrain extends SparkTool {


  /**
    * 贝叶斯分类   一般用于文本文类   垃圾邮件分类
    *
    *
    * 1、对数据进行处理，去除脏数据
    * 2、对评论进行分词
    * 3、将数据转换成向量，加上if-idf
    * 4、将训练集带入贝叶斯算法  得到模型
    * 5、将模型保存到hdfs
    *
    *
    */

  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {

    val data = sc.textFile("data/train.txt")

    //1、脏数据过滤
    val filterRDD = data
      .map(_.split("\t"))
      .map(arr => (arr(0), arr(1)))
      .map(t => (t._1, t._2.replace("'", "")))
      .map(t => (t._1, "http.*".r.replaceAllIn(t._2, "")))
      .map(t => (t._1, "#".r.replaceAllIn(t._2, "")))
      .map(t => (t._1, "�".r.replaceAllIn(t._2, "")))
      .map(t => (t._1, "\\s".r.replaceAllIn(t._2, "")))
      .map(t => (t._1, "[a-zA-Z0-9]".r.replaceAllIn(t._2, "")))
      .map(t => (t._1, "\\p{P}".r.replaceAllIn(t._2, " ")))
      .map(t => (t._1, "【".r.replaceAllIn(t._2, " ")))
      .filter(t => !t._2.contains("转发微博"))
      .filter(t => t._2.trim.length > 1)

    //对数据进行分词
    val wordsRDD = filterRDD.map(t => {
      (t._1.toDouble, IK.fit(t._2))
    })
      .filter(_._2.length > 1)

    //3、将数据转换成向量，加上if-idf

    val s = sql
    import s.implicits._

    //将RDD转换成DF
    val srcDF = wordsRDD
      .map(t => (t._1, t._2.mkString(" ")))
      .toDF("label", "text")


    //Tokenizer  英文分词器
    val tok = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("feature")

    val tokDF = tok.transform(srcDF)

    //计算tf
    val tfModel = new HashingTF()
      .setInputCol("feature")
      .setOutputCol("tf")

    val tfDF = tfModel.transform(tokDF)


    tfDF.cache()

    //计算idf

    //计算if-idf
    val idf = new IDF()
      .setInputCol("tf")
      .setOutputCol("features")

    //训练idf模型
    val idfModel = idf.fit(tfDF)

    val tfIdfDF = idfModel.transform(tfDF)

    //切分训练集和测试集
    val splitDF = tfIdfDF.randomSplit(Array(0.8, 0.2))
    val trainDF = splitDF(0)
    val testDF = splitDF(1)

    //构建贝叶斯算法
    val naiveBayes = new NaiveBayes()
      .setLabelCol("label")
      .setFeaturesCol("features")
      .setModelType("multinomial")

    //训练模型
    val nbModel = naiveBayes.fit(trainDF)

    //通过测试集进行测试
    val redultDF = nbModel.transform(testDF)
    redultDF.cache()

    redultDF.show(false)

    //计算模型准确率
    val flagRDD = redultDF
      .rdd
      .map(row => {
        val prediction = row.getAs[Double]("prediction")
        val label = row.getAs[Double]("label")
        math.abs(prediction - label)
      })

    //模型准确率
    val testaccuracy = 1 - flagRDD.reduce(_ + _) / flagRDD.count().toDouble

    println("模型准确率：" + testaccuracy)
    //保存模型
    if (testaccuracy > 0.8) {
      //保存模型
      idfModel.write.overwrite().save("model/idfModel")
      nbModel.write.overwrite().save("model/nbModel")

      //保存特征数量
      println(tfModel.getNumFeatures)
    }


  }


  /**
    * 初始化spark配置
    *  conf.setMaster("local")
    */
  override def init(): Unit = {
    conf.setMaster("local[4]")
    conf.set("spark.sql.shuffle.partitions", "4")
  }

}