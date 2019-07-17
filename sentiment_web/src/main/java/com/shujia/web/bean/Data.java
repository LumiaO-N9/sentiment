package com.shujia.web.bean;

import java.util.Arrays;

public class Data {
    private String []x;
    private Integer [] y;

//


    @Override
    public String toString() {
        return "Data{" +
                "x=" + Arrays.toString(x) +
                ", y=" + Arrays.toString(y) +
                '}';
    }

    public Data() {
    }

    public Data(String[] x, Integer[] y) {
        this.x = x;
        this.y = y;
    }

    public String[] getX() {
        return x;
    }

    public void setX(String[] x) {
        this.x = x;
    }

    public Integer[] getY() {
        return y;
    }

    public void setY(Integer[] y) {
        this.y = y;
    }
}
