1、微博文章写入es,实现站内搜索模块
    1、启动ES服务器
    2、创建索引库
        curl -XPUT http://node2:9200/article/
    3、配置ik分词器映射
        设置对应的列使用ik分词器   text：列名
        curl -XPOST http://node2:9200/article/fulltext/_mapping -d'
        {
            "fulltext": {
                "properties": {
                    "text": {
                        "type": "string",
                        "store": "no",
                        "term_vector": "with_positions_offsets",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_max_word",
                        "include_in_all": "true",
                        "boost": 8
                    }
                }
            }
        }'

       测试查询

        curl -XPOST http://node2:9200/article/fulltext/_search  -d'
        {
            "query" : { "term" : { "text" : "司机" }}
        }
        '
