import json
import re
import time
import random

import scrapy

from sentiment_spider.items import ArticleItem, CommentUrlItem, WeiBoUserItem
from scrapy_redis.spiders import RedisSpider
import redis
from sentiment_spider.settings import *
from fake_useragent import UserAgent

ua = UserAgent


# url 取redis中获取   key  = weibo_search_spider:start_urls
class weibo_search_spider(RedisSpider):
    name = "weibo_search_spider"
    userAgent = ua.random

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # redis 连接池
        self.pool = redis.ConnectionPool(host=REDIS_HOST, port=REDIS_PORT,
                                         decode_responses=True)  # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379

    def start_requests(self):
        Cookie_dict = {'_T_WM': '94772099193', 'WEIBOCN_FROM': '1110006030',
                       'TMPTOKEN': 'WOjIxtRECuXJM2kcq6CpLpX5wNSs0oPppMHKzanpMU7rac2zngFfuhHeEGu0TrUO',
                       'SUB': '_2A25zchGHDeRhGeVM6FQQ-CzEwjyIHXVQnL_PrDV6PUJbktAKLRj4kW1NTLxnqENk1I3Y8aJfeP6XGHDMLTpl5ZIA',
                       'SUBP': '0033WrSXqPxfM725Ws9jqgMF55529P9D9WhM.aS0DkGCs1PP_vOZPLyN5JpX5KzhUgL.FoeEe0qp1hzR1K52dJLoIf2LxKML12-LBK.LxKBLBonLB-2LxK-LB.-LB--LxK-L12BL1-2LxKBLBo.L1-qLxKnLBKqL1h2LxKnLBo-LBoMLxK-L1K5L12BLxK-LB-BL1KMt',
                       'SUHB': '0sqBOWTJ_XFJdM', 'SSOLoginState': '1584816599', 'MLOGIN': '1',
                       'M_WEIBOCN_PARAMS': 'oid%3D4475265549647387%26luicode%3D10000011%26lfid%3D100103type%253D1%2526q%253D%25E5%258D%258E%25E4%25B8%25AD%25E7%25A7%2591%25E6%258A%2580%25E5%25A4%25A7%25E5%25AD%25A6%25EF%25BC%258C%25E6%2596%25B0%25E5%259E%258B%25E5%2586%25A0%25E7%258A%25B6%25E7%2597%2585%25E6%25AF%2592%26uicode%3D20000174',
                       'XSRF-TOKEN': '6cd675'}

        head = {
            "Content-Type": "application/x-www-form-urlencoded",
            "Origin": "https://passport.weibo.cn",
            "Referer": "https://passport.weibo.cn/signin/login?entry=mweibo&res=wel&wm=3349&r=https%3A%2F%2Fm.weibo.cn%2Fdetail%2F4357286229257109",
            "User-Agent": self.userAgent
        }
        # 获取登录页面
        yield scrapy.Request(url="https://passport.weibo.cn/signin/login", headers=head, cookies=Cookie_dict,
                             callback=self.login)

    def login(self, response):
        head = {
            "Content-Type": "application/x-www-form-urlencoded",
            "Origin": "https://passport.weibo.cn",
            "Referer": "https://passport.weibo.cn/signin/login?entry=mweibo&res=wel&wm=3349&r=https%3A%2F%2Fm.weibo.cn%2Fdetail%2F4357286229257109",
            "User-Agent": self.userAgent
        }
        Cookie_dict = {'_T_WM': '94772099193', 'WEIBOCN_FROM': '1110006030',
                       'TMPTOKEN': 'WOjIxtRECuXJM2kcq6CpLpX5wNSs0oPppMHKzanpMU7rac2zngFfuhHeEGu0TrUO',
                       'SUB': '_2A25zchGHDeRhGeVM6FQQ-CzEwjyIHXVQnL_PrDV6PUJbktAKLRj4kW1NTLxnqENk1I3Y8aJfeP6XGHDMLTpl5ZIA',
                       'SUBP': '0033WrSXqPxfM725Ws9jqgMF55529P9D9WhM.aS0DkGCs1PP_vOZPLyN5JpX5KzhUgL.FoeEe0qp1hzR1K52dJLoIf2LxKML12-LBK.LxKBLBonLB-2LxK-LB.-LB--LxK-L12BL1-2LxKBLBo.L1-qLxKnLBKqL1h2LxKnLBo-LBoMLxK-L1K5L12BLxK-LB-BL1KMt',
                       'SUHB': '0sqBOWTJ_XFJdM', 'SSOLoginState': '1584816599', 'MLOGIN': '1',
                       'M_WEIBOCN_PARAMS': 'oid%3D4475265549647387%26luicode%3D10000011%26lfid%3D100103type%253D1%2526q%253D%25E5%258D%258E%25E4%25B8%25AD%25E7%25A7%2591%25E6%258A%2580%25E5%25A4%25A7%25E5%25AD%25A6%25EF%25BC%258C%25E6%2596%25B0%25E5%259E%258B%25E5%2586%25A0%25E7%258A%25B6%25E7%2597%2585%25E6%25AF%2592%26uicode%3D20000174',
                       'XSRF-TOKEN': '6cd675'}

        formdata = {
            'username': WEIBO_LOGIN_USERNAME1,
            "password": WEIBO_LOGIN_PASSWORD1,
        }
        # 登录
        yield scrapy.FormRequest(url="https://passport.weibo.cn/sso/login", formdata=formdata,
                                 callback=self.parse_login, headers=head, cookies=Cookie_dict)

    # 登录之后响应
    def parse_login(self, response):
        print(response.text)

    # 回调函数
    def parse(self, response):
        # 获取请求url
        url = response.request.url
        # 创建redis连接池
        client = redis.Redis(connection_pool=self.pool, db=0)
        # 初始化邮件发送器

        # 从redis获取舆情id
        sentiment_id = int(client.hget("url_flag", url))

        jsonstr = json.loads(response.text)
        time.sleep(random.randint(1, 3))
        for card in jsonstr["data"]["cards"]:
            mblog = card["mblog"]

            ######################################微博文章##################################################
            articleItem = ArticleItem()

            attitudes_count = mblog["attitudes_count"]  # 点赞
            comments_count = mblog["comments_count"]  # 评论
            reposts_count = mblog["reposts_count"]  # 转发
            text = mblog["text"]  # 微博内容
            article_created_at = mblog["created_at"]  # 时间

            id = mblog["id"]

            page_url = "https://m.weibo.cn/detail/%s" % id

            re_h = re.compile('</?\w+[^>]*>')  # 去掉HTML标签
            text = re_h.sub("", text)

            user_id = mblog["user"]["id"]
            followers_count = mblog["user"]["followers_count"]

            articleItem["id"] = id
            articleItem["user_id"] = user_id
            articleItem["article_created_at"] = article_created_at
            articleItem["sentiment_id"] = sentiment_id
            articleItem["attitudes_count"] = attitudes_count
            articleItem["comments_count"] = comments_count
            articleItem["reposts_count"] = reposts_count
            articleItem["text"] = text
            articleItem["page_url"] = page_url
            articleItem["followers_count"] = followers_count

            # 发生给pipelines
            yield articleItem

            # ######################################评价url#################################################

            mid = mblog["mid"]

            # 构建评价url
            commenturl = "https://m.weibo.cn/comments/hotflow?id=%s&mid=%s&max_id_type=0"
            commenturl = commenturl % (id, mid)
            commenturlItem = CommentUrlItem()
            commenturlItem["url"] = commenturl
            commenturlItem["sentiment_id"] = sentiment_id
            # 发生给pipelines
            yield commenturlItem

            # ######################################微博用户信息################################################
            userItem = WeiBoUserItem()

            user = mblog["user"]

            userItem["sentiment_id"] = sentiment_id
            userItem["description"] = user["description"]  # 描述
            userItem["id"] = user["id"]  # 用户id
            userItem["screen_name"] = user["screen_name"]  # 用户名
            userItem["follow_count"] = user["follow_count"]  # 关注度
            userItem["followers_count"] = user["followers_count"]  # 粉丝数
            userItem["gender"] = user["gender"]  # 性别

            # 发生给pipelines
            yield userItem
