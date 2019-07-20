package com.shujia.web.bean;


public class GenderCount {
    private String name;
    private Long value;

    @Override
    public String toString() {
        return "GenderCount{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public GenderCount() {
    }

    public GenderCount(String name, Long value) {
        this.name = name;
        this.value = value;
    }
}
