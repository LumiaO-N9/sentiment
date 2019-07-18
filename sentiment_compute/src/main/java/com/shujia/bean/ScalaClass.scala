package com.shujia.bean

object ScalaClass {

  //微博评价自定义类
  case class WeiboComment(article_id: String,
                          sentiment_id: Long,
                          comment_id: String,
                          created_at: String,
                          user_name: String,
                          user_id: Long,
                          total_number: Long,
                          like_count: Long,
                          text: String
                         )


  //微博用户
  case class WeiBoUser(
                            description: String,
                            id: Long,
                            screen_name: String,
                            follow_count: Long,
                            followers_count: Long,
                            gender: String
                          )


}
