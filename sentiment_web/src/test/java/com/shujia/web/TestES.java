package com.shujia.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shujia.web.bean.Article;
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
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Map;

public class TestES {
    public static void main(String[] args) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("master", 9200, "http")));

        CountRequest countRequest = new CountRequest("article");
        //构建查询对象
        String word = "新型冠状病毒";
        int id = 1;
        int limit = 10;
        int offset = 0;
        QueryBuilder qb = QueryBuilders.termQuery("sentiment_id", 1);
        QueryBuilder qb1 = QueryBuilders.matchQuery("text", "浙江工商大学");
        //指定排序字段
        SortBuilder sortBuilder = SortBuilders.fieldSort("attitudes_count");
        sortBuilder.order(SortOrder.DESC);

        QueryBuilder s = QueryBuilders.boolQuery().must(qb).must(qb1);//.must(qb5);

        System.out.println(word + " " + limit + " " + offset);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(s);
        countRequest.source(searchSourceBuilder);
        CountResponse countResponse = client
                .count(countRequest, RequestOptions.DEFAULT);
        long count = countResponse.getCount();
        System.out.println(count);


        searchSourceBuilder.from(150);
        searchSourceBuilder.size(limit);
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightText =
                new HighlightBuilder.Field("text");
        highlightText.highlighterType("plain");
        highlightText.preTags("<mark>");
        highlightText.postTags("</mark>");
        highlightBuilder.field(highlightText);

        searchSourceBuilder.highlighter(highlightBuilder);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("article");
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping();
        Gson gson = gsonBuilder.create();
        for (SearchHit hit : hits.getHits()) {
            Article article = gson.fromJson(hit.getSourceAsString(), Article.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            System.out.println(hit.getSourceAsString());
            HighlightField highlight = highlightFields.get("text");
            Text[] fragments = highlight.fragments();
            String fragmentString = fragments[0].string();
            article.setText(fragmentString);
            String jsonObject = gson.toJson(article);
            System.out.println(jsonObject);
            System.out.println(fragmentString);
        }
        client.close();
    }
}
