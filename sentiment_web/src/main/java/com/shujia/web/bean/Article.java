package com.shujia.web.bean;

public class Article {
    private String id;
    private Long user_id;
    private Long sentiment_id;
    private Long attitudes_count;
    private String article_created_at;
    private Long comments_count;
    private Long reposts_count;
    private String text;
    private String page_url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public Long getSentiment_id() {
        return sentiment_id;
    }

    public void setSentiment_id(Long sentiment_id) {
        this.sentiment_id = sentiment_id;
    }

    public Long getAttitudes_count() {
        return attitudes_count;
    }

    public void setAttitudes_count(Long attitudes_count) {
        this.attitudes_count = attitudes_count;
    }

    public String getArticle_created_at() {
        return article_created_at;
    }

    public void setArticle_created_at(String article_created_at) {
        this.article_created_at = article_created_at;
    }

    public Long getComments_count() {
        return comments_count;
    }

    public void setComments_count(Long comments_count) {
        this.comments_count = comments_count;
    }

    public Long getReposts_count() {
        return reposts_count;
    }

    public void setReposts_count(Long reposts_count) {
        this.reposts_count = reposts_count;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPage_url() {
        return page_url;
    }

    public void setPage_url(String page_url) {
        this.page_url = page_url;
    }

    public Article() {
    }
}
