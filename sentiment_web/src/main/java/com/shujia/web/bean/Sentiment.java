package com.shujia.web.bean;

import java.sql.Date;

public class Sentiment {
    private String id;
    private String name;
    private String words;
    private Date date;

    @Override
    public String toString() {
        return "Sentiment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", words='" + words + '\'' +
                ", date=" + date +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Sentiment() {
    }

    public Sentiment(String id, String name, String words, Date date) {
        this.id = id;
        this.name = name;
        this.words = words;
        this.date = date;
    }
}
