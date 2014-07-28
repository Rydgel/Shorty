# Urls schema

# --- !Ups

ALTER TABLE urls ADD COLUMN created bigint NOT NULL;

# --- !Downs

ALTER TABLE urls DROP COLUMN created;