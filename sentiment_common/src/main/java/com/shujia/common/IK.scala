package com.shujia.common

import java.io.StringReader

import org.wltea.analyzer.core.IKSegmenter

import scala.collection.mutable.ListBuffer

object IK {
  def main(args: Array[String]): Unit = {
    val text = "张三峰说的确实在理"
    println("待切分字符串： " + text)
    println("Smart Mode")
    println(IK.fit(text).mkString("|"))
    println("no Smart Mode")
    println(IK.fit(text, false).mkString("|"))
  }

  /**
   * ik分词器
   *
   * @return
   */
  def fit(text: String, smartMode: Boolean = true): List[String] = {
    val sr = new StringReader(text)
    val ik = new IKSegmenter(sr, smartMode)

    val lf = new ListBuffer[String]

    var lex = ik.next
    while (lex != null) {
      lf.+=(lex.getLexemeText)
      lex = ik.next
    }

    lf.toList
  }
}
