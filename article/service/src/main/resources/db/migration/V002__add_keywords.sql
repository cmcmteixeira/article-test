CREATE TABLE keywords
(
    word    varchar(256),
    article Int references articles (id),
    UNIQUE (word, article)
);