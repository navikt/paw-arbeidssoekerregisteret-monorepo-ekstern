ALTER TABLE stillinger_v2
    ADD COLUMN tags VARCHAR[] NOT NULL DEFAULT '{}';

CREATE INDEX stillinger_v2_tags_idx ON stillinger_v2 USING GIN (tags);

