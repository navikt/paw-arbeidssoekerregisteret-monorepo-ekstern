CREATE INDEX stillinger_status_idx ON stillinger(status);
CREATE INDEX stillinger_kilde_idx ON stillinger(kilde);
CREATE INDEX kategorier_normalisert_kode_idx ON kategorier(normalisert_kode);
CREATE INDEX klassifiseringer_type_idx ON klassifiseringer(type);
CREATE INDEX klassifiseringer_kode_idx ON klassifiseringer(kode);
CREATE INDEX lokasjoner_fylkeskode_idx ON lokasjoner(fylkeskode);
CREATE INDEX lokasjoner_kommunekode_idx ON lokasjoner(kommunekode);
