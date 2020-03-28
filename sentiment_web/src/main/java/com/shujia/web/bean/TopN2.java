package com.shujia.web.bean;

import java.util.List;

public class TopN2 {
    private List<String> x;
    private List<User> users;


    @Override
    public String toString() {
        return "TopN{" +
                "x=" + x +
                ", users=" + users +
                '}';
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }


    public TopN2() {
    }

}
