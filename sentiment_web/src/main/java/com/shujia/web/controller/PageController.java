package com.shujia.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class PageController {

    /**
     * 获取页面
     * 以do结尾  ，自动返回jsp 下面的页面
     */
    @RequestMapping(value = "/{pagename}.do",   method = RequestMethod.GET)
    public String page(@PathVariable("pagename") String name) {
        return name;
    }


}
