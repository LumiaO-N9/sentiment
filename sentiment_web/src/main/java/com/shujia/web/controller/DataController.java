package com.shujia.web.controller;

import com.shujia.common.JDBCUtil;
import com.shujia.web.bean.GenderCount;
import com.shujia.web.bean.Sentiment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class DataController {

    /**
     * 获取性别占比
     */
    @RequestMapping("/getGenderCount")
    public ArrayList<GenderCount> getGenderCount(String id) {
        ArrayList<GenderCount> genderCounts = new ArrayList<>();
        genderCounts.add(new GenderCount("男", 1000L));
        genderCounts.add(new GenderCount("女", 1040L));
        genderCounts.add(new GenderCount("其他", 2000L));

        return genderCounts;
    }

}
