import scrapy
import json
import re
import time
from sentiment_spider.items import WeiboCommentItem
import random


class weibo_comment_spider(scrapy.Spider):
    name = "weibo_comment_spider"

    # 爬虫入口
    def start_requests(self):
        url = "https://m.weibo.cn/comments/hotflow?id=4394343743609962&mid=4394343743609962&max_id_type=0"

        yield scrapy.Request(url=url, callback=self.parse)

    # 回调函数
    def parse(self, response):

        sentiment_id = 1

        head = {
            "Content-Type": "application/x-www-form-urlencoded",
            "Origin": "https://passport.weibo.cn",
            "Referer": "https://passport.weibo.cn/signin/login?entry=mweibo&res=wel&wm=3349&r=https%3A%2F%2Fm.weibo.cn%2Fdetail%2F4357286229257109",
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36"
        }
        try:
            id_re = re.compile("\?id=(\w*)&")
            mid_re = re.compile("mid=(\w*)&")
            url = response.request.url
            id = id_re.findall(url)[0]
            mid = mid_re.findall(url)[0]

            jsonStr = json.loads(response.text)
            print(jsonStr)

            if "data" in jsonStr:
                for data in jsonStr["data"]["data"]:
                    comment_id = data["id"]
                    created_at = data["created_at"]

                    time_string = created_at[4:10] + ' ' + created_at[26:30] + ' ' + created_at[11:19]
                    time_struct = time.strptime(time_string, "%b %d %Y %H:%M:%S")
                    created_at = time.strftime("%Y-%m-%d %H:%M:%S", time_struct)

                    user_name = data["user"]["screen_name"]
                    user_id = data["user"]["id"]

                    total_number = data["total_number"]
                    like_count = data["like_count"]
                    text = data["text"]

                    re_h = re.compile('</?\w+[^>]*>')  # 去掉HTML标签
                    text = re_h.sub("", text)

                    # 评价内容自定义对象
                    item = WeiboCommentItem()
                    item["article_id"] = id  # 微博id
                    item["sentiment_id"] = sentiment_id  # 舆情编号
                    item["comment_id"] = comment_id  # 评价id
                    item["created_at"] = created_at  # 创建时间
                    item["user_name"] = user_name  # 评价人名
                    item["user_id"] = user_id  # 评价人编号
                    item["total_number"] = total_number  # 回复人数
                    item["like_count"] = like_count  # 点赞数量
                    item["text"] = text


                    yield item

                # 构建下一页请求地址
                url = "https://m.weibo.cn/comments/hotflow?id=%s&mid=%s&max_id=%s&max_id_type=%s"
                max_id = jsonStr['data']["max_id"]
                max_id_type = jsonStr['data']["max_id_type"]
                url = url % (id, mid, max_id, max_id_type)

                # 暂停一会，防止被烦爬虫
                time.sleep(random.randint(1, 3))

                # 获取下一页
                yield scrapy.Request(url=url, headers=head, callback=self.parse)
        except:
            pass
