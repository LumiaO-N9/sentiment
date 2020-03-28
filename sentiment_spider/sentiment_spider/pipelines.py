# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://doc.scrapy.org/en/latest/topics/item-pipeline.html
from sentiment_spider.items import WeiBoUserItem, WeiboCommentItem, ArticleItem, CommentUrlItem
import json
from kafka import KafkaProducer
from sentiment_spider.settings import *
import redis
import time
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail


class SentimentspiderPipeline(object):

    # 初始化redis连接，kafka连接
    def __init__(self):

        # 初始化kafka连接
        # settings.get("BOOTSTRAP_SERVERS")  获取settings配置信息
        self.producer = KafkaProducer(bootstrap_servers=BOOTSTRAP_SERVERS)

        # redis 连接池
        self.pool = redis.ConnectionPool(host=REDIS_HOST, port=REDIS_PORT,
                                         decode_responses=True)  # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379

    # 数据处理方法  将数据打入kafka或者写入redis
    def process_item(self, item, spider):

        # 微博用户数据打入kafka  spark  streaming 消费去重存到hbase
        if isinstance(item, WeiBoUserItem):
            client = redis.Redis(connection_pool=self.pool, db=0)
            user_id = item["id"]
            sentiment_id = item["sentiment_id"]
            distinct_id = str(sentiment_id) + "_" + str(user_id)
            flag = client.sismember("weiBo_user_distinct", distinct_id)
            if not flag:
                # 自定义对象转换成json
                data = json.dumps(dict(item), ensure_ascii=False)
                key = str(int(time.time() * 1000))
                print(data)
                # # 数据打入kafka   key  偏移量
                self.producer.send(topic="WeiBoUserItemTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
                self.producer.flush()
                client.sadd("weiBo_user_distinct", distinct_id)

        # 微博文章数据打入kafka   spark  streaming 消费  数据写入es  提供搜索服务
        if isinstance(item, ArticleItem):
            client = redis.Redis(connection_pool=self.pool, db=0)
            article_id = item["id"]
            sentiment_id = item["sentiment_id"]
            distinct_id = str(sentiment_id) + "_" + str(article_id)
            flag = client.sismember("weiBo_article_distinct", distinct_id)

            if not flag:
                # 自定义对象转换成json
                data = json.dumps(dict(item), ensure_ascii=False)
                print(data)

                # 判断博主粉丝数 如果大于100W就发送邮件提醒
                alter_switch = int(client.get("AlertSwitch"))
                if alter_switch == 1:
                    followers_count = item["followers_count"]
                    if int(followers_count) > 1000000:
                        article_base_url = "https://m.weibo.cn/status/%s"
                        detailUrl = article_base_url % article_id
                        html_content_base = '<strong>有粉丝数大于100W的博主发布了一篇微博，快来看看吧！<a href="%s"target="_blank">%s</a></strong>'
                        html_content = html_content_base % (detailUrl, detailUrl)
                        message = Mail(
                            from_email=FROM_EMAIL,
                            to_emails=TO_EMAILS,
                            subject=SUBJECT,
                            html_content=html_content
                        )
                        try:
                            sg = SendGridAPIClient(SENDGRID_APIKEY)
                            response = sg.send(message)
                            print(response.status_code)
                            print(response.body)
                            print(response.headers)
                        except Exception as e:
                            print(e.message)
                # 数据打入kafka
                key = str(int(time.time() * 1000))
                self.producer.send(topic="ArticleItemTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
                self.producer.flush()
                client.sadd("weiBo_article_distinct", distinct_id)
        # 评价数据处理方法
        if isinstance(item, WeiboCommentItem):
            # 对数据进行去重
            # 从连接池获取一个连接
            client = redis.Redis(connection_pool=self.pool, db=0)

            # 评价id
            comment_id = item["comment_id"]
            sentiment_id = item["sentiment_id"]
            distinct_id = str(sentiment_id) + "_" + str(comment_id)
            # 如果存在返回true,不存在返回false
            flag = client.sismember("weiBo_comment_distinct", distinct_id)

            if not flag:
                # 自定义对象转换成json
                data = json.dumps(dict(item), ensure_ascii=False)
                print(data)
                # 时间戳作为偏移量
                key = str(int(time.time() * 1000))
                # 数据打入kafka
                self.producer.send(topic="WeiBoCommentTopic", value=data.encode("utf-8"), key=key.encode("utf-8"))
                self.producer.flush()

                # 将已经爬取过的数据的id写入redis
                client.sadd("weiBo_comment_distinct", distinct_id)

        # 评价url  打入redis  其他爬虫从redis获取url爬取数据
        if isinstance(item, CommentUrlItem):
            # 将评价url  压入redis
            client = redis.Redis(connection_pool=self.pool, db=0)
            client.lpush("weibo_comment_spider:start_urls", item["url"])

            # 将评价url和舆情id写入redis
            client.hset("url_flag", item["url"], item["sentiment_id"])
