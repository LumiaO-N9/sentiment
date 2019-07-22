package com.shujia.web.controller;

import com.shujia.common.Config;
import com.shujia.common.JDBCUtil;
import com.shujia.web.bean.GenderCount;
import com.shujia.web.bean.PageHelper;
import com.shujia.web.bean.Sentiment;
import com.shujia.web.bean.Word;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DataController {

    /**
     * 获取性别占比
     */
    @RequestMapping("/getGenderCount")
    public ArrayList<GenderCount> getGenderCount(String id) {
        ArrayList<GenderCount> genderCounts = new ArrayList<>();

        String key = id + "_gender";

        //查询redis获取性别占比
        Jedis jedis = new Jedis(Config.getString("redis.host"), 6379);

        Map<String, String> map = jedis.hgetAll(key);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key1 = entry.getKey();
            long value = Long.parseLong(entry.getValue());

            if ("m".equals(key1)) {
                genderCounts.add(new GenderCount("男", value));
            } else if ("f".equals(key1)) {
                genderCounts.add(new GenderCount("女", value));
            } else {
                genderCounts.add(new GenderCount("其它", value));
            }
        }


        return genderCounts;
    }


    /**
     * 获取词云图
     */
    @RequestMapping("/getWordCloud")
    public ArrayList<Word> getWordCloud(String id) {
        ArrayList<Word> words = new ArrayList<>();

        String key = id + "_word_cloud";

        //查询redis获取性别占比
        Jedis jedis = new Jedis(Config.getString("redis.host"), 6379);

        Map<String, String> map = jedis.hgetAll(key);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String word = entry.getKey();
            Integer count = Integer.parseInt(entry.getValue());

            words.add(new Word(word, count));
        }

        return words;
    }

    /**
     * 关键字搜索
     */
    @RequestMapping(value = "/wordSearch", method = RequestMethod.GET)
    public PageHelper wordSearch(String id, String word, Integer offset, Integer limit) {

        PageHelper pageHelper = new PageHelper();
        ArrayList<Map> list = new ArrayList<Map>();
        pageHelper.setRows(list);

        // 通过setting对象指定集群配置信息, 配置的集群名
        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", "my-application") // 设置集群名
                .put("client.transport.ignore_cluster_name", true) // 忽略集群名字验证, 打开后集群名字不对也能连接上
                .build();

        /**
         * 创建es连接
         *
         */
        TransportClient client = TransportClient
                .builder()
                .settings(settings)
                .build()
                .addTransportAddresses(
                        new InetSocketTransportAddress(new InetSocketAddress("node1", 9300)),
                        new InetSocketTransportAddress(new InetSocketAddress("node2", 9300)),
                        new InetSocketTransportAddress(new InetSocketAddress("node3", 9300))
                );

        //构建查询对象
        QueryBuilder qb = QueryBuilders.termQuery("sentiment_id", id);
        QueryBuilder qb1 = QueryBuilders.matchQuery("text", word);


        //指定排序字段
        SortBuilder sortBuilder = SortBuilders.fieldSort("attitudes_count");
        sortBuilder.order(SortOrder.DESC);

        QueryBuilder s = QueryBuilders.boolQuery().must(qb).should(qb1);//.must(qb5);

        //查询微博总数
        CountRequestBuilder article = client
                .prepareCount("article")
                .setTypes("fulltext")
                .setQuery(s);
        Long total = article.get().getCount();
        pageHelper.setTotal(total);

        SearchRequestBuilder sv = client.prepareSearch("article")
                .setTypes("fulltext")
                .setQuery(s)
                .addSort(sortBuilder)
                .setSize(limit)
                .setFrom(offset)
                //高亮显示
                .addHighlightedField("*")
                .setHighlighterPreTags("<a>")
                .setHighlighterPostTags("<a>");


        SearchResponse response = sv.get();
        SearchHits searchHits = response.getHits();
        for (SearchHit hit : searchHits.getHits()) {
            list.add(hit.getSource());
        }

        return pageHelper;
    }




}
