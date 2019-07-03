CREATE TABLE keywords
(
    word    varchar(256),
    article_id INT REFERENCES articles (id),
    UNIQUE (word, article_id)
);