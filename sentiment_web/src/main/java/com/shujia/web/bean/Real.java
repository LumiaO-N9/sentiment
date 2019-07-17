package com.shujia.web.bean;

import java.util.List;

public class Real {
    private List<String> x;
    private List<Integer> y1;
    private List<Integer> y2;
    private List<Integer> y3;

    @Override
    public String toString() {
        return "Real{" +
                "x=" + x +
                ", y1=" + y1 +
                ", y2=" + y2 +
                ", y3=" + y3 +
                '}';
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<Integer> getY1() {
        return y1;
    }

    public void setY1(List<Integer> y1) {
        this.y1 = y1;
    }

    public List<Integer> getY2() {
        return y2;
    }

    public void setY2(List<Integer> y2) {
        this.y2 = y2;
    }

    public List<Integer> getY3() {
        return y3;
    }

    public void setY3(List<Integer> y3) {
        this.y3 = y3;
    }

    public Real() {
    }

    public Real(List<String> x, List<Integer> y1, List<Integer> y2, List<Integer> y3) {
        this.x = x;
        this.y1 = y1;
        this.y2 = y2;
        this.y3 = y3;
    }
}
