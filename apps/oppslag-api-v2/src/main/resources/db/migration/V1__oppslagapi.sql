CREATE TABLE data
(
    id BIGSERIAL NOT NULL,
    arbeidssoeker_id bigint,
    identitetsnummer char(11),
    periode_id uuid NOT NULL,
    timestamp timestamp NOT NULL,
    data jsonb NOT NULL,
    primary key (id)
);

create table hwm
(
    version smallint NOT NULL,
    kafka_topic varchar(255) NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    primary key (version, kafka_topic, kafka_partition)
);
