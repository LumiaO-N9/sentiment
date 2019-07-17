package com.shujia.web.bean;

public class Word {
   private String name;
   private Integer value;

    public Word() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Word(String name, Integer value) {
        this.name = name;
        this.value = value;
    }
}
