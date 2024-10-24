insert into periode (periode_id, identitetsnummer, startet, avsluttet)
select    gen_random_uuid() periode_id,
          c1 identitetsnummer,
          TO_TIMESTAMP(c2, 'YYYY-MM-DD HH24:MI:SS') startet,
          TO_TIMESTAMP(c3, 'YYYY-MM-DD HH24:MI:SS') avsluttet
from test_data_ekstern_api;