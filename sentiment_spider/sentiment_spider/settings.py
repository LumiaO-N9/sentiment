# -*- coding: utf-8 -*-

# Scrapy settings for sentimentspider project
#
# For simplicity, this file contains only settings considered important or
# commonly used. You can find more settings consulting the documentation:
#
#     https://doc.scrapy.org/en/latest/topics/settings.html
#     https://doc.scrapy.org/en/latest/topics/downloader-middleware.html
#     https://doc.scrapy.org/en/latest/topics/spider-middleware.html

BOT_NAME = 'sentiment_spider'

SPIDER_MODULES = ['sentiment_spider.spiders']
NEWSPIDER_MODULE = 'sentiment_spider.spiders'

# kafka地址
BOOTSTRAP_SERVERS = "master:9092,node1:9092,node2:9092"

# redis连接地址
REDIS_HOST = "master"
REDIS_PORT = "6379"
# REDIS_URL = "redis://:123456@node1:6379"
# Crawl responsibly by identifying yourself (and your website) on the user-agent
# USER_AGENT = 'sentimentspider (+http://www.yourdomain.com)'

# Obey robots.txt rules
ROBOTSTXT_OBEY = False

# 整合redis配置
######################################################################
# Enables scheduling storing requests queue in redis.
SCHEDULER = "scrapy_redis.scheduler.Scheduler"

# Ensure all spiders share same duplicates filter through redis.
DUPEFILTER_CLASS = "scrapy_redis.dupefilter.RFPDupeFilter"

######################################################################

WEIBO_LOGIN_USERNAME = '17600195923'
WEIBO_LOGIN_PASSWORD = 'shujia888'
WEIBO_LOGIN_USERNAME1 = '15170818675'
WEIBO_LOGIN_PASSWORD1 = 'zzk1044740758'
WEIBO_LOGIN_USERNAME2 = '15107965760'
WEIBO_LOGIN_PASSWORD2 = '911015jj'
USER_COOKIE = '_T_WM=25459262795; WEIBOCN_FROM=1110006030; MLOGIN=1; XSRF-TOKEN=cdffe3; SSOLoginState=1581767213; SUB=_2A25zQ6p9DeRhGeVM6FQQ-CzEwjyIHXVQzzY1rDV6PUJbktANLULwkW1NTLxnqE6H534kIEFQBX81HfhxqAvIQgF-; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhM.aS0DkGCs1PP_vOZPLyN5JpX5KzhUgL.FoeEe0qp1hzR1K52dJLoIf2LxKML12-LBK.LxKBLBonLB-2LxK-LB.-LB--LxK-L12BL1-2LxKBLBo.L1-qLxKnLBKqL1h2LxKnLBo-LBoMLxK-L1K5L12BLxK-LB-BL1KMt; SUHB=0ElaXc0a38Zzbj; M_WEIBOCN_PARAMS=oid%3D4471164695130833%26lfid%3D4471164695130833%26luicode%3D20000174%26uicode%3D20000174'
USER_COOKIE1 = '_T_WM=94772099193; WEIBOCN_FROM=1110006030; TMPTOKEN=WOjIxtRECuXJM2kcq6CpLpX5wNSs0oPppMHKzanpMU7rac2zngFfuhHeEGu0TrUO; SUB=_2A25zchGHDeRhGeVM6FQQ-CzEwjyIHXVQnL_PrDV6PUJbktAKLRj4kW1NTLxnqENk1I3Y8aJfeP6XGHDMLTpl5ZIA; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhM.aS0DkGCs1PP_vOZPLyN5JpX5KzhUgL.FoeEe0qp1hzR1K52dJLoIf2LxKML12-LBK.LxKBLBonLB-2LxK-LB.-LB--LxK-L12BL1-2LxKBLBo.L1-qLxKnLBKqL1h2LxKnLBo-LBoMLxK-L1K5L12BLxK-LB-BL1KMt; SUHB=0sqBOWTJ_XFJdM; SSOLoginState=1584816599; MLOGIN=1; M_WEIBOCN_PARAMS=oid%3D4475265549647387%26luicode%3D10000011%26lfid%3D100103type%253D1%2526q%253D%25E5%258D%258E%25E4%25B8%25AD%25E7%25A7%2591%25E6%258A%2580%25E5%25A4%25A7%25E5%25AD%25A6%25EF%25BC%258C%25E6%2596%25B0%25E5%259E%258B%25E5%2586%25A0%25E7%258A%25B6%25E7%2597%2585%25E6%25AF%2592%26uicode%3D20000174; XSRF-TOKEN=6cd675'
USER_COOKIE2 = '_T_WM=41937541010; XSRF-TOKEN=66c7d7; WEIBOCN_FROM=1110006030; MLOGIN=0; M_WEIBOCN_PARAMS=uicode%3D10000011%26fid%3D102803; TMPTOKEN=FRtNiHj3ROvlUXxvpzGNsyw6Yo2zCeu6VNCvGqmq7zBdJG5y2DCUadbWodpd9K27; SUB=_2A25zYkdcDeRhGeBO6FAV-SfPzj2IHXVQrWkUrDV6PUJbkdANLW6lkW1NSgOQfk_WjxT9070Ipx-jPkPNntlIHQW9; SUHB=0FQrdOLt1ir-t8; SCF=Ah2dUsS2FLouziw8E8QBTT1S1DTXL1SZvUgNwiznsG3xpv_S6cVh1UhM4ggRfmF2lCjhb_-6KcIkG-uitI4zbrg.; SSOLoginState=1583757068'

# 代理

# DOWNLOADER_MIDDLEWARES = {
#     'sentimentspider.middlewares.ProxyMiddleware': 730,
# }
PROXIES = ['http://113.121.22.240:9999']
# Configure maximum concurrent requests performed by Scrapy (default: 16)
# CONCURRENT_REQUESTS = 32

# Configure a delay for requests for the same website (default: 0)
# See https://doc.scrapy.org/en/latest/topics/settings.html#download-delay
# See also autothrottle settings and docs
# DOWNLOAD_DELAY = 3
# The download delay setting will honor only one of:
# CONCURRENT_REQUESTS_PER_DOMAIN = 16
# CONCURRENT_REQUESTS_PER_IP = 16

# Disable cookies (enabled by default)
# COOKIES_ENABLED = False

# Disable Telnet Console (enabled by default)
# TELNETCONSOLE_ENABLED = False

# Override the default request headers:
# DEFAULT_REQUEST_HEADERS = {
#   'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
#   'Accept-Language': 'en',
# }

# Enable or disable spider middlewares
# See https://doc.scrapy.org/en/latest/topics/spider-middleware.html
# SPIDER_MIDDLEWARES = {
#    'sentimentspider.middlewares.SentimentspiderSpiderMiddleware': 543,
# }

# Enable or disable downloader middlewares
# See https://doc.scrapy.org/en/latest/topics/downloader-middleware.html
# DOWNLOADER_MIDDLEWARES = {
#    'sentimentspider.middlewares.SentimentspiderDownloaderMiddleware': 543,
# }

# Enable or disable extensions
# See https://doc.scrapy.org/en/latest/topics/extensions.html
# EXTENSIONS = {
#    'scrapy.extensions.telnet.TelnetConsole': None,
# }

# Configure item pipelines
# See https://doc.scrapy.org/en/latest/topics/item-pipeline.html
ITEM_PIPELINES = {
    'sentiment_spider.pipelines.SentimentspiderPipeline': 300,
}

# Enable and configure the AutoThrottle extension (disabled by default)
# See https://doc.scrapy.org/en/latest/topics/autothrottle.html
# AUTOTHROTTLE_ENABLED = True
# The initial download delay
# AUTOTHROTTLE_START_DELAY = 5
# The maximum download delay to be set in case of high latencies
# AUTOTHROTTLE_MAX_DELAY = 60
# The average number of requests Scrapy should be sending in parallel to
# each remote server
# AUTOTHROTTLE_TARGET_CONCURRENCY = 1.0
# Enable showing throttling stats for every response received:
# AUTOTHROTTLE_DEBUG = False

# Enable and configure HTTP caching (disabled by default)
# See https://doc.scrapy.org/en/latest/topics/downloader-middleware.html#httpcache-middleware-settings
# HTTPCACHE_ENABLED = True
# HTTPCACHE_EXPIRATION_SECS = 0
# HTTPCACHE_DIR = 'httpcache'
# HTTPCACHE_IGNORE_HTTP_CODES = []
# HTTPCACHE_STORAGE = 'scrapy.extensions.httpcache.FilesystemCacheStorage'

SENDGRID_APIKEY = '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'
FROM_EMAIL = 'xiaoBei@sentiment.com'
TO_EMAILS = '1585659554@qq.com'
SUBJECT = '微博舆情分析平台预警'
