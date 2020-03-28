package com.shujia.web.bean;

import java.util.List;

public class TopN {
    private List<String> x;
    private List<Integer> y;


    @Override
    public String toString() {
        return "TopN{" +
                "x=" + x +
                ", y=" + y+
                '}';
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<Integer> getY() {
        return y;
    }

    public void setY(List<Integer> y) {
        this.y = y;
    }

    public TopN() {
    }

    public TopN(List<String> x, List<Integer> y) {
        this.x = x;
        this.y = y;
    }
}
