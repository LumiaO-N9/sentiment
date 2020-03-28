import com.shujia.common.SparkTool

object constructStopWordsDir extends SparkTool {
  /**
   * 在run方法里面编写spark业务逻辑
   */
  override def run(args: Array[String]): Unit = {
    val stopwords = sc.textFile("data/stopwords")
    stopwords.distinct().coalesce(1).saveAsTextFile("data/stopwords.dir")
  }

  /**
   * 初始化spark配置
   *  conf.setMaster("local")
   */
  override def init(): Unit = {
    conf.setMaster("local[4]")
  }
}
