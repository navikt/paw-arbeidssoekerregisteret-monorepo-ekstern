ALTER TABLE stillinger_v2
    ADD COLUMN tekniske_tags VARCHAR[] NOT NULL DEFAULT '{}';

CREATE INDEX stillinger_v2_tekniske_tags_idx ON stillinger_v2 USING GIN (tekniske_tags);

