package com.shujia.web.controller;

import com.shujia.common.Config;
import com.shujia.common.JDBCUtil;
import com.shujia.web.bean.GenderCount;
import com.shujia.web.bean.Sentiment;
import com.shujia.web.bean.Word;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import redis.clients.jedis.Jedis;

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

}
