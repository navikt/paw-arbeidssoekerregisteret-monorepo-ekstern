create user oppslag_api with password 'Paw1234';
create user eksternt_api with password 'Paw1234';

create database arbeidssoekerregisteretapioppslag with owner oppslag_api;
create database arbeidssoekerregisteretapiekstern with owner eksternt_api;
