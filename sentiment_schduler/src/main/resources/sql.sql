#舆情信息表
#DROP TABLE tb_sentiment;
CREATE TABLE IF NOT EXISTS tb_sentiment
(
  id     INT UNSIGNED AUTO_INCREMENT,
  name   VARCHAR(20)  NOT NULL,
  words  VARCHAR(100) NOT NULL,
  `date` DATE,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8;





