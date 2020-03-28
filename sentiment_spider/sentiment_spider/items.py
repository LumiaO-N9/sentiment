# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# https://doc.scrapy.org/en/latest/topics/items.html

import scrapy


class SentimentSpiderItem(scrapy.Item):
    # define the fields for your item here like:
    # name = scrapy.Field()
    pass


# 微博用户自定义对象
class WeiBoUserItem(scrapy.Item):
    sentiment_id = scrapy.Field()  # 舆情ID
    description = scrapy.Field()  # 描述
    id = scrapy.Field()  # 用户id
    screen_name = scrapy.Field()  # 用户名
    follow_count = scrapy.Field()  # 关注度
    followers_count = scrapy.Field()  # 粉丝数
    url = scrapy.Field()  # 用户访问地址
    gender = scrapy.Field()  # 性别


# 评价url
class CommentUrlItem(scrapy.Item):
    url = scrapy.Field()
    sentiment_id = scrapy.Field()


# 微博文章自定义对象
class ArticleItem(scrapy.Item):
    id = scrapy.Field()  # 微博id
    user_id = scrapy.Field()  # 用户id
    sentiment_id = scrapy.Field()  # 舆情编号
    attitudes_count = scrapy.Field()  # 点赞
    article_created_at = scrapy.Field()  # 时间
    comments_count = scrapy.Field()  # 评论数
    reposts_count = scrapy.Field()  # 转发
    text = scrapy.Field()  # 微博内容
    page_url = scrapy.Field()  # 微博地址
    followers_count = scrapy.Field()  # 博主粉丝数


# 评价内容自定义对象
class WeiboCommentItem(scrapy.Item):
    article_id = scrapy.Field()  # 微博id
    sentiment_id = scrapy.Field()  # 舆情编号
    comment_id = scrapy.Field()  # 评价id
    created_at = scrapy.Field()  # 创建时间
    user_name = scrapy.Field()  # 评价人名
    user_id = scrapy.Field()  # 评价人编号
    total_number = scrapy.Field()  # 回复人数
    like_count = scrapy.Field()  # 点赞数量
    text = scrapy.Field()  # 评价内容
