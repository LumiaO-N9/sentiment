# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://doc.scrapy.org/en/latest/topics/item-pipeline.html
from sentiment_spider.items import WeiBoUserItem, WeiboCommentItem, ArticleItem, CommentUrlItem
import json
from kafka import KafkaProducer
from scrapy.conf import settings
import redis
import time


class SentimentspiderPipeline(object):

    # 初始化redis连接，kafka连接
    def __init__(self):

        # 初始化kafka连接
        # settings.get("BOOTSTRAP_SERVERS")  获取settings配置信息
        self.producer = KafkaProducer(bootstrap_servers=settings.get("BOOTSTRAP_SERVERS"))


        # redis 连接池
        self.pool = redis.ConnectionPool(host=settings.get("REDIS_HOST"), port=settings.get("REDIS_PORT"),
                                         decode_responses=True)  # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379



    # 数据处理方法  将数据打入kafka或者写入redis
    def process_item(self, item, spider):

        # 微博用户数据打入kafka  spark  streaming 消费去重存到hbase
        if isinstance(item, WeiBoUserItem):
            # 自定义对象转换成json
            data = json.dumps(dict(item), ensure_ascii=False)
            key = str(int(time.time() * 1000))
            print(data)
            # # 数据打入kafka   key  偏移量
            self.producer.send(topic="WeiBoUserItemTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
            self.producer.flush()

        # 微博文章数据打入kafka   apark  streaming 消费  数据写入es  提供搜索服务
        if isinstance(item, ArticleItem):
            # 自定义对象转换成json
            data = json.dumps(dict(item), ensure_ascii=False)
            print(data)

            # 数据打入kafka
            key = str(int(time.time() * 1000))
            self.producer.send(topic="ArticleItemTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
            self.producer.flush()

        # 评价数据处理方法
        if isinstance(item, WeiboCommentItem):
            # 自定义对象转换成json
            data = json.dumps(dict(item), ensure_ascii=False)
            # 时间戳作为偏移量
            key = str(int(time.time() * 1000))
            # 数据打入kafka
            self.producer.send(topic="WeiBoCommentTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
            self.producer.flush()

        # 评价url  打入redis  其他爬虫从redis获取url爬取数据
        if isinstance(item, CommentUrlItem):
            # 将评价url  压入redis
            client = redis.Redis(connection_pool=self.pool, db=0)
            client.lpush("weibo_comment_spider:start_urls", item["url"])
