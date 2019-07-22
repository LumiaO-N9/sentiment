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
                          var text: String
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

  //微博文章
  case class WeiBoArticle(
                           id: String,
                           user_id: Long,
                           sentiment_id: Long,
                           attitudes_count: Long,
                           created_at: String,
                           comments_count: Long,
                           reposts_count: Long,
                           text: String,
                           page_url: String
                         )


}
