1、创建数据库
create database sentiment;

2、创建舆情信息表

#DROP TABLE tb_sentiment;
CREATE TABLE IF NOT EXISTS tb_sentiment
(
  id     INT UNSIGNED AUTO_INCREMENT,
  name   VARCHAR(20)  NOT NULL,
  words  VARCHAR(100) NOT NULL,
  `date` DATE,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8;


3、修改sentiment_common 项目的default.properties  改成自己的数据库


4、启动spring boot
    通过http://localhost:8080/ 访问页面

