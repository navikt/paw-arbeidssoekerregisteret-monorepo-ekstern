# PAW Monorepo for applikasjoner med eksterne avhengigheter

Repo tiltenkt "downstream" tjenester i arbeidssøkerregisteret. Dvs tjenester som primært konsumerer: periode, opplysninger og/eller profileringstopic uten å forholde seg til hendelseloggen. Tjenester som benytter seg av hendelsesloggen bør i utgangspunktet plasseres i "
"paw-arbeidssoekerregisteret-monorepo-intern".

Eksempler på tjenester som skal flyttes/opprettes her:
- profilering
- ekstern-api
- oppslags-api
- Aktivering av microfrontends (tjenester for å trigge noe basert på records fra periode,opplysninger og/eller profilering).

Tjenester som benytter seg av hendelsesloggen plasseres i https://github.com/navikt/paw-arbeidssoekerregisteret-monorepo-intern.
