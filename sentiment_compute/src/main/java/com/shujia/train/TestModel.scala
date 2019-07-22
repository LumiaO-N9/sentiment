package com.shujia.train

import com.shujia.common.{IK, SparkTool}
import org.apache.spark.ml.feature.{HashingTF, IDFModel, Tokenizer}
import org.apache.spark.ml.classification.NaiveBayesModel

object TestModel extends SparkTool {
  /**
    * 在run方法里面编写spark业务逻辑
    */
  override def run(args: Array[String]): Unit = {
    val srcRDD = sc.parallelize(
      List(
        "生日快乐，今天生日的人一定会永保快乐和青春。, ，还有, 家的欣欣宝贝",
        "就是没时间！至于为什么，呵呵，那不是孩子的问题也不是家长的问题！",
        "某些网民极其有意思，九年义务教育没培养出你对历史古迹的珍视和尊重感、形成对人类文明的认识，反而在这疯狂敲键盘秀下限",
        "一切都会消失 没有什么永垂不朽 可还是很伤感 很伤感 伴随着很多奇奇怪怪的情绪"
      )
    )

    //、分词
    val wordRDD = srcRDD.map(line => IK.fit(line).mkString(" "))

    val s = sql
    import s.implicits._

    val wordDF = wordRDD.toDF("text")


    //Tokenizer  英文分词器
    val tok = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("feature")

    val tokDF = tok.transform(wordDF)


    //计算tf
    val tfModel = new HashingTF()
      .setNumFeatures(262144) //设置特征数量
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

    rsultDF.show(false)
  }

  /**
    * 初始化spark配置
    *  conf.setMaster("local")
    */
  override def init(): Unit = {
    conf.setMaster("local")
  }

}
