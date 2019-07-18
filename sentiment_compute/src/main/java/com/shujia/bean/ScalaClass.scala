package com.shujia.bean

object ScalaClass {

  //微博评价自定义类
  case class WeiboComment(article_id: String,
                          sentiment_id: Integer,
                          comment_id: String,
                          created_at: String,
                          user_name: String,
                          user_id: Long,
                          total_number: Integer,
                          like_count: Integer,
                          text: String
                         )



}
