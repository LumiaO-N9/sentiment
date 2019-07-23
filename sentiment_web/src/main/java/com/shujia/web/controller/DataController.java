package com.shujia.web.controller;

import com.shujia.common.Config;
import com.shujia.common.JDBCUtil;
import com.shujia.web.bean.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;

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


    /**
     * 获取舆情走势
     */
        @RequestMapping("/getRealTimeSentiment")
        public Real getRealTimeSentiment(String id) {

        Configuration conf = new Configuration();
        conf.set("hbase.zookeeper.quorum", "node1:2181,node2:2181,node3:2181");
        Real real = new Real();
        try {
            //丽娜姐regionserver,  负责表的增删改查
            HConnection connection = HConnectionManager.createConnection(conf);

            HTableInterface table = connection.getTable("comment_sentiment");

            Get get = new Get(id.getBytes());
            get.addFamily("info".getBytes());
            //如果不指定版本号，默认只查询一个版本的数据
            get.setMaxVersions(10);


            ArrayList<String> xlist = new ArrayList<>();
            ArrayList<Integer> y1list = new ArrayList<Integer>();
            ArrayList<Integer> y2list = new ArrayList<Integer>();
            ArrayList<Integer> y3list = new ArrayList<Integer>();

            real.setX(xlist);
            real.setY1(y1list);
            real.setY2(y2list);
            real.setY3(y3list);

            Result result = table.get(get);
            List<Cell> columnCells = result.getColumnCells("info".getBytes(), "real".getBytes());

            for (Cell columnCell : columnCells) {
                long timestamp = columnCell.getTimestamp();
                Date date = new Date(timestamp);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH");
                String x = format.format(date);
                xlist.add(x);

                String value = Bytes.toString(CellUtil.cloneValue(columnCell));

                HashMap<String, Integer> map = new HashMap<>();
                map.put("0.0", 0);
                map.put("1.0", 0);
                map.put("2.0", 0);

                for (String kv : value.split("\\|")) {
                    String k = kv.split(":")[0];
                    Integer v = Integer.parseInt(kv.split(":")[1]);
                    map.put(k, v);
                }
                y1list.add(map.get("0.0"));
                y2list.add(map.get("1.0"));
                y3list.add(map.get("2.0"));

            }

            Collections.reverse(real.getX());
            Collections.reverse(real.getY1());
            Collections.reverse(real.getY2());
            Collections.reverse(real.getY3());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return real;

    }


}
