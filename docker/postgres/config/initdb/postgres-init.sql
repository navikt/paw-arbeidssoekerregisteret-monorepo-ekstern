create user oppslag_api with password 'Paw1234';
create user eksternt_api with password 'Paw1234';
create user brukerprofiler_api with password 'Paw1234';
create user ledigestillinger_api with password 'Paw1234';

create database arbeidssoekerregisteretapioppslag with owner oppslag_api;
create database arbeidssoekerregisteretapiekstern with owner eksternt_api;
create database brukerprofiler with owner brukerprofiler_api;
create database ledigestillinger with owner ledigestillinger_api;
