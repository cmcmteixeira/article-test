CREATE TABLE keywords
(
    word    varchar(256),
    article INT REFERENCES articles (id),
    UNIQUE (word, article)
);