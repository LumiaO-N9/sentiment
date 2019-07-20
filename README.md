1、启动爬虫程序
    1、修改kafka和redis连接地址
        # kafka地址
        BOOTSTRAP_SERVERS = "47.102.118.141:9092,47.102.105.40:9092"

        # redis连接地址
        REDIS_HOST = "47.102.118.141"
        REDIS_PORT = "6379"

    2、启动爬虫
        进入爬虫项目根目录   进入cmd
        scrapy crawl weibo_search_spider
        scrapy crawl weibo_comment_spider

2、启动调度程序   每5分钟轮询一次
    修改mysql数据库地址和redis连接
    mysql--->  舆情信息
    redis--->  url连接

    启动调度

3、启动计算模块
    修改kafka连接地址
    修改hbase连接地址   只能用本地hbase
    修改redis连接地址


4、启动web模块
    修改mysql连接地址
    修改redis连接地址