package com.shujia.common

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.{Logger, LoggerFactory}

/**
  * spark写代码工具，用于简写spark代码
  *
  */
abstract class SparkTool {
  var conf: SparkConf = _
  var sc: SparkContext = _
  var sql: SQLContext = _
  var LOGGER: Logger = _

  def main(args: Array[String]): Unit = {
    //创建打日志对象
    LOGGER = LoggerFactory.getLogger(this.getClass)


    conf = new SparkConf()

    //调用初始化方法
    init()

    conf.setAppName(this.getClass.getSimpleName)

    LOGGER.info("============开始执行spark程序============")

    //spark删改问对象
    sc = new SparkContext(conf)
    //spark sql上下文对象
    sql = new SQLContext(sc)

    //调用子类run函数
    run(args)
    LOGGER.info("============spark程序执行完成============")
  }

  /**
    * 在run方法里面编写spark业务逻辑
    */
  def run(args: Array[String])

  /**
    * 初始化spark配置
    *  conf.setMaster("local")
    */
  def init()

}
