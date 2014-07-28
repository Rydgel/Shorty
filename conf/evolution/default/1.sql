# Urls schema

# --- !Ups

CREATE ALIAS UUID FOR
"org.h2.value.ValueUuid.getNewRandom";

CREATE TABLE urls (
    id uuid NOT NULL,
    code text NOT NULL,
    long_url text NOT NULL,
    hits integer NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE urls;