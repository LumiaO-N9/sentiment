import com.shujia.common.IK

object IKTest extends App {
  val text = "中国你好"
  println(IK.fit(text).mkString(" "))

}
