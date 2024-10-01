-- Fra veilarbregistrering må følgense spørring kjøres:

/*with registreringer as (select * from bruker_registrering
                        where foedselsnummer in  (<<liste_med_foedselsnummer>>)),

     profilering as (select p.* from bruker_registrering r
                                         inner join bruker_profilering p
                                                    on p.bruker_registrering_id= r.bruker_registrering_id
                     where r.foedselsnummer in  (<<liste_med_foedselsnummer>>)),

     profilering_long as (
         select alder.bruker_registrering_id, alder.verdi alder, arbeid.verdi arb_6_av_siste_12_mnd, resultat.verdi resultat_profilering
         from (select * from profilering where profilering_type = 'ALDER') alder
                  inner join (select * from profilering where profilering_type = 'ARB_6_AV_SISTE_12_MND') arbeid
                             on alder.bruker_registrering_id = arbeid.bruker_registrering_id
                  inner join (select * from profilering where profilering_type = 'RESULTAT_PROFILERING') resultat
                             on alder.bruker_registrering_id = resultat.bruker_registrering_id)

select reg.bruker_registrering_id, opprettet_dato, nus_kode, yrkespraksis, yrkesbeskrivelse, konsept_id, andre_utfordringer,
       begrunnelse_for_registrering, utdanning_godkjent_norge, utdanning_bestatt, jobbhistorikk, har_helseutfordringer, foedselsnummer,
       alder, arb_6_av_siste_12_mnd, resultat_profilering
from registreringer reg inner join profilering_long prof
                                   on reg.bruker_registrering_id = prof.bruker_registrering_id;*/

-- Deretter må denne filen lastes ned som en csv og lastes opp igjen her. Dette gjøres ved å høyreklikke på databaseconnectionene og velge import
-- navngi filen test_data_dev, husk å endre foedselsnummer fra numeric til text

-- Kod om fra vet ikke til ingen svar
update test_data_dev
set utdanning_godkjent_norge = null
where utdanning_godkjent_norge = 'INGEN_SVAR';

update test_data_dev
set utdanning_bestatt = null
where utdanning_bestatt = 'INGEN_SVAR';


-- endre begrunnelse som ikke lenger finnes
update test_data_dev
set begrunnelse_for_registrering = 'HAR_BLITT_SAGT_OPP'
where begrunnelse_for_registrering = 'MISTET_JOBBEN';


update test_data_dev
set resultat_profilering = 'ANTATT_GODE_MULIGHETER'
where resultat_profilering = 'IKVAL';

update test_data_dev
set resultat_profilering = 'ANTATT_BEHOV_FOR_VEILEDNING'
where resultat_profilering = 'BFORM';

update test_data_dev
set resultat_profilering = 'OPPGITT_HINDRINGER'
where resultat_profilering = 'BKART';


update test_data_dev
set jobbhistorikk = null
where jobbhistorikk = 'INGEN_SVAR';

update test_data_dev
set jobbhistorikk = 'JA'
where jobbhistorikk = 'HAR_HATT_JOBB';


INSERT INTO bruker (bruker_id, type)
SELECT
    foedselsnummer bruker_id,
    'SLUTTBRUKER' type
from test_data_dev;

-- Insert metadata for brukerne

insert into metadata (utfoert_av_id, tidspunkt, kilde, aarsak)
select
    ROW_NUMBER() OVER (ORDER BY (SELECT NULL)),
        TO_TIMESTAMP(t.opprettet_dato, 'YYYY-MM-DD HH24:MI:SS'),
    'veilarbregistrering' kilde,
    'manuell_insert' aarsak
from test_data_dev t;


insert into periode (periode_id, identitetsnummer, startet_id, avsluttet_id)
SELECT gen_random_uuid() AS uuid,
       foedselsnummer,
       ROW_NUMBER() OVER (ORDER BY (SELECT NULL)),
        null avsluttet_id
from test_data_dev;

insert into utdanning (nus, bestaatt, godkjent)
select
    nus_kode,
    (utdanning_bestatt)::JaNeiVetIkke bestaatt,
        (utdanning_godkjent_norge)::JaNeiVetIkke godkjent
from test_data_dev;

insert into helse (helsetilstand_hindrer_arbeid)
select
    (har_helseutfordringer)::JaNeiVetIkke helsetilstand_hindrer_arbeid
from test_data_dev;



insert into annet (andre_forhold_hindrer_arbeid)
select
    (andre_utfordringer)::JaNeiVetIkke andre_forhold_hindrer_arbeid
from test_data_dev;


-- Her er vi avhengig av samme rekkefølge hele veien
insert into opplysninger_om_arbeidssoeker(opplysninger_om_arbeidssoeker_id, sendt_inn_av_id,
                                          utdanning_id, helse_id, annet_id)
select
    gen_random_uuid() AS uuid,
    ROW_NUMBER() OVER (ORDER BY (SELECT NULL)),
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)),
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)),
        id -- bare for å få 5 rader

from annet;

insert into periode_opplysninger(periode_id, opplysninger_om_arbeidssoeker_id)
select periode_id,
       ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) -- + 10
from periode;

insert into beskrivelse_med_detaljer(opplysninger_om_arbeidssoeker_id)
select id from opplysninger_om_arbeidssoeker;


insert into beskrivelse( beskrivelse, beskrivelse_med_detaljer_id)
select
    (begrunnelse_for_registrering)::beskrivelseenum beskrivelse,
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) id
from test_data_dev;

-- må sette inn en systembruker i brukertabellen

INSERT INTO bruker (bruker_id, type)
SELECT     'SYSTEM' bruker_id,
           'SYSTEM' type;

-- må inserte metadata fro alle profileringene
insert into metadata (utfoert_av_id, tidspunkt, kilde, aarsak)
select
    6,
    TO_TIMESTAMP(t.opprettet_dato, 'YYYY-MM-DD HH24:MI:SS'),
    'veilarbregistrering' kilde,
    'manuell_insert' aarsak
from test_data_dev t;

insert into profilering (profilering_id, periode_id, opplysninger_om_arbeidssoeker_id, sendt_inn_av_id, profilert_til,
                         jobbet_sammenhengende_seks_av_tolv_siste_maneder, alder)
select
    gen_random_uuid() as profilering_id,
    periode.periode_id,
    ooa.opplysninger_om_arbeidssoeker_id,
    6 sendt_inn_id,
    (resultat_profilering)::profilerttil profilert_til,
        CAST(arb_6_av_siste_12_mnd as boolean),
    alder
from periode inner join periode_opplysninger po on periode.periode_id = po.periode_id
             inner join opplysninger_om_arbeidssoeker ooa on po.opplysninger_om_arbeidssoeker_id = ooa.id
             inner join test_data_dev on test_data_dev.foedselsnummer = periode.identitetsnummer

    insert into detaljer(beskrivelse_id, noekkel, verdi)
select
    id,
    'stilling' noekkel,
    'Administrasjonskonsulent' verdi
from beskrivelse;

insert into detaljer(beskrivelse_id, noekkel, verdi)
select
    id,
    'stilling_styrk08' noekkel,
    '3343' verdi
from beskrivelse;

delete from detaljer
where beskrivelse_id in (4,5);

--- Obsobs her må man sjekke at alle de manuelle tallene blir riktige
-- Avslutte og starte ny periode
insert into metadata (utfoert_av_id, tidspunkt, kilde, aarsak)
select 1,  NOW(), 'SLUTTBRUKER', 'manuell_avsluttning' ;

update periode
set avsluttet_id = 18
where id = 1;

-- avslutt og start
insert into metadata (utfoert_av_id, tidspunkt, kilde, aarsak)
select 2,  NOW(), 'SLUTTBRUKER', 'manuell_avsluttning' ;

update periode
set avsluttet_id = 19
where id = 2;

insert into metadata (utfoert_av_id, tidspunkt, kilde, aarsak)
select 2,  NOW(), 'SLUTTBRUKER', 'manuell_start' ;

insert into periode (periode_id, identitetsnummer, startet_id, avsluttet_id)
SELECT gen_random_uuid() AS uuid,
       23917399581,
       20,
       null avsluttet_id