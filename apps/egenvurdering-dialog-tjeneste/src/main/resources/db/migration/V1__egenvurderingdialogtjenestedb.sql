/* Dialog */
CREATE TABLE dialog
(
    id BIGSERIAL PRIMARY KEY,
    dialog_id VARCHAR NOT NULL UNIQUE,
    egenvurdering_id UUID NOT NULL UNIQUE
);