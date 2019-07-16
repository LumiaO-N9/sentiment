import scrapy
import json
import re
from sentiment_spider.items import ArticleItem, CommentUrlItem, WeiBoUserItem


class weibo_search_spider(scrapy.Spider):
    name = "weibo_search_spider"

    # 爬虫入口
    def start_requests(self):
        url = "https://m.weibo.cn/api/container/getIndex?containerid=100103type%3D60%26q%3D%E5%A4%A9%E5%AE%AB%E4%BA%8C%E5%8F%B7%E8%A6%81%E9%80%80%E4%BC%91%E4%BA%86%26t%3D0&page_type=searchall"

        yield scrapy.Request(url=url, callback=self.parse)

    # 回调函数
    def parse(self, response):

        sentiment_id = 1

        jsonstr = json.loads(response.text)

        for card in jsonstr["data"]["cards"]:

            for line in card["card_group"]:
                mblog = line["mblog"]

                ######################################微博文章##################################################
                articleItem = ArticleItem()

                attitudes_count = mblog["attitudes_count"]  # 点赞
                comments_count = mblog["comments_count"]  # 评论
                reposts_count = mblog["reposts_count"]  # 转发
                text = mblog["text"]  # 微博内容
                created_at = mblog["created_at"]  # 时间

                id = mblog["id"]

                page_url = "https://m.weibo.cn/detail/%s" % id

                re_h = re.compile('</?\w+[^>]*>')  # 去掉HTML标签
                text = re_h.sub("", text)

                user_id = mblog["user"]["id"]

                articleItem["id"] = id
                articleItem["user_id"] = user_id
                articleItem["created_at"] = created_at
                articleItem["sentiment_id"] = sentiment_id
                articleItem["attitudes_count"] = attitudes_count
                articleItem["comments_count"] = comments_count
                articleItem["reposts_count"] = reposts_count
                articleItem["text"] = text
                articleItem["page_url"] = page_url

                print(articleItem)
                # 发生给pipelines
                yield articleItem

                #######################################评价url#################################################

                mid = mblog["mid"]

                # 构建评价url
                commenturl = "https://m.weibo.cn/comments/hotflow?id=%s&mid=%s&max_id_type=0"
                commenturl = commenturl % (id, mid)
                commenturlItem = CommentUrlItem()
                commenturlItem["url"] = commenturl
                commenturlItem["sentiment_id"] = sentiment_id
                # 发生给pipelines
                print(commenturlItem)
                yield commenturlItem

                #######################################微博用户信息################################################
                userItem = WeiBoUserItem()

                user = mblog["user"]

                userItem["description"] = user["description"]  # 描述
                userItem["id"] = user["id"]  # 用户id
                userItem["screen_name"] = user["screen_name"]  # 用户名
                userItem["follow_count"] = user["follow_count"]  # 关注度
                userItem["followers_count"] = user["followers_count"]  # 粉丝数
                userItem["gender"] = user["gender"]  # 性别
                print(userItem)
                # 发生给pipelines
                yield userItem
