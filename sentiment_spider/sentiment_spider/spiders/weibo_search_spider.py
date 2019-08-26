import scrapy
import json
import re
from sentiment_spider.items import ArticleItem, CommentUrlItem, WeiBoUserItem
from scrapy_redis.spiders import RedisSpider
import redis
from sentiment_spider.settings import *

# url 取redis中获取   key  = weibo_search_spider:start_urls
class weibo_search_spider(RedisSpider):
    name = "weibo_search_spider"

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # redis 连接池
        self.pool = redis.ConnectionPool(host=REDIS_HOST, port=REDIS_PORT,
                                         decode_responses=True)  # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379

    # 回调函数
    def parse(self, response):

        # 获取请求url
        url = response.request.url
        client = redis.Redis(connection_pool=self.pool, db=0)
        # 从redis获取舆情id
        sentiment_id = int(client.hget("url_flag", url))

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
