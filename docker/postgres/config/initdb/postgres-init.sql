create user oppslag_api with password 'Paw1234';
create user eksternt_api with password 'Paw1234';
create user egenvurdering_dialog_tjeneste with password 'Paw1234';

create database arbeidssoekerregisteretapioppslag with owner oppslag_api;
create database arbeidssoekerregisteretapiekstern with owner eksternt_api;
create database egenvurderingdialogtjenestedb with owner egenvurdering_dialog_tjeneste;
