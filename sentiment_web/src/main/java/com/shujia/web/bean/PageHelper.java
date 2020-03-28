package com.shujia.web.bean;

import java.util.List;
import java.util.Map;

public class PageHelper {
    // 注意：这两个属性名称不能改变，是定死的
    // 实体类集合
    private List<Article> rows;
    // 数据总条数
    private Long total;


    public PageHelper() {
    }

    @Override
    public String toString() {
        return "PageHelper{" +
                "rows=" + rows +
                ", total=" + total +
                '}';
    }

    public List<Article> getRows() {
        return rows;
    }

    public void setRows(List<Article> rows) {
        this.rows = rows;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public PageHelper(List<Article> rows, Long total) {
        this.rows = rows;
        this.total = total;
    }
}
