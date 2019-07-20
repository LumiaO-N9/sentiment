package com.shujia.web.controller;

import com.shujia.common.JDBCUtil;
import com.shujia.web.bean.Sentiment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.*;
import java.util.ArrayList;

@RestController
public class SentimentController {

    /**
     * 增加舆情
     */
    @RequestMapping("/addSentiment")
    public RedirectView addSentiment(Sentiment sentiment) {
        System.out.println(sentiment);

        Connection connection = JDBCUtil.getConnection();

        String sql = "insert into tb_sentiment(name,words,date) values(?,?,?)";
        try {
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, sentiment.getName());
            stat.setString(2, sentiment.getWords());
            stat.setDate(3, new Date(System.currentTimeMillis()));
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //返回主页面
        return new RedirectView("/");

    }


    /**
     * 获取舆情列表
     */
    @RequestMapping("/getSentimentList")
    public ArrayList<Sentiment> getSentimentList() {

        Connection connection = JDBCUtil.getConnection();

        ArrayList<Sentiment> sentiments = new ArrayList<>();

        String sql = "select * from tb_sentiment";

        try {
            PreparedStatement stat = connection.prepareStatement(sql);
            ResultSet resultSet = stat.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String words = resultSet.getString("words");
                Date date = resultSet.getDate("date");

                String a_id = "<a href=\"sentimentinfo.do?id=" + id + "\">" + id + "</a>";

                Sentiment sentiment = new Sentiment(a_id, name, words, date);

                sentiments.add(sentiment);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //hi姐返回一个数组，spring  会自动将集合转换成json字符串
        return sentiments;

    }
}
