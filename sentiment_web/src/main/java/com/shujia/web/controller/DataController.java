package com.shujia.web.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shujia.common.Config;
import com.shujia.web.bean.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class DataController {
    /**
     * 获取粉丝数排名
     */
    @RequestMapping("/getFollowersCount")
    public TopN getFollowersCount(String id) {
        ArrayList<String> xlist = new ArrayList<>();
        ArrayList<Integer> ylist = new ArrayList<Integer>();
        TopN topN = new TopN();
        topN.setX(xlist);
        topN.setY(ylist);
        String key = id + "_followers_TopN";
        //查询redis获取用户粉丝数
        Jedis jedis = new Jedis(Config.getString("redis.host"), 6379);

        Set<String> zrevrange = jedis.zrevrange(key, 0, 9);

        for (String idAndNameAndFollowers : zrevrange) {
            String[] splits = idAndNameAndFollowers.split("\\|");
            String userName = splits[1];
            xlist.add(userName);
            ylist.add(jedis.zscore(key, idAndNameAndFollowers).intValue());
        }

        return topN;

    }

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
     * 获取舆情走势
     */
    @RequestMapping("/getRealTimeSentiment")
    public Real getRealTimeSentiment(String id) {

        Configuration conf = new Configuration();
        conf.set("hbase.zookeeper.quorum", "master:2181,node1:2181,node2:2181");
        Real real = new Real();
        try {
            //连接regionserver,  负责表的增删改查
            HConnection connection = HConnectionManager.createConnection(conf);

            HTableInterface table = connection.getTable("comment_sentiment");

            Get get = new Get(id.getBytes());
            get.addFamily("info".getBytes());
            //如果不指定版本号，默认只查询一个版本的数据
            get.setMaxVersions(12);


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
                    Integer v = Integer.parseInt(kv.split(":")[1]) * 10;
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

    /**
     * 关键字搜索
     */
    @RequestMapping(value = "/wordSearch", method = RequestMethod.GET)
    public PageHelper wordSearch(String id, String word, Integer offset, Integer limit) throws IOException {
        if ("".equals(word)) {
            return new PageHelper();
        }
        PageHelper pageHelper = new PageHelper();
        ArrayList<Article> list = new ArrayList<Article>();
        pageHelper.setRows(list);

        /**
         * 创建es连接
         *
         */
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("master", 9200, "http")));


        //构建查询对象
        QueryBuilder qb = QueryBuilders.termQuery("sentiment_id", id);
        QueryBuilder qb1 = QueryBuilders.matchQuery("text", word);
        System.out.println(word + " " + limit + " " + offset);

        QueryBuilder s = QueryBuilders.boolQuery().must(qb).must(qb1);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(s);

        //查询微博总数
        CountRequest countRequest = new CountRequest("article");
        countRequest.source(searchSourceBuilder);
        CountResponse countResponse = client
                .count(countRequest, RequestOptions.DEFAULT);
        long total = countResponse.getCount();

        pageHelper.setTotal(total);

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightText =
                new HighlightBuilder.Field("text");
        highlightText.highlighterType("plain")
                .preTags("<mark>")
                .postTags("</mark>");
        highlightBuilder.field(highlightText);
        searchSourceBuilder.from(offset)
                .size(limit)
                .highlighter(highlightBuilder);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("article");
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping();
        Gson gson = gsonBuilder.create();

        for (SearchHit hit : searchHits.getHits()) {
            Article article = gson.fromJson(hit.getSourceAsString(), Article.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlight = highlightFields.get("text");
            Text[] fragments = highlight.fragments();
            String fragmentString = fragments[0].string();
            article.setText(fragmentString);
            list.add(article);
            //            list.add(hit.getSource());
        }

        return pageHelper;
    }
}
