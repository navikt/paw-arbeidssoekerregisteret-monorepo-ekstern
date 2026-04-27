# Stillinger API

API for å hente stillinger som er hentet fra `teampam.stilling-ekstern-1`-topicet til Arbeidsplassen.

## Arbeidsplassen

### Docs

* [pam.ansatt.nav.no](https://pam.ansatt.nav.no)

### Properties

```json
[
    {
        "key": "experience",
        "value": "[\"Ingen\"]"
    },
    {
        "key": "education",
        "value": "[\"Fagskole\", \"Fagbrev\"]"
    },
    {
        "key": "needDriversLicense",
        "value": "true"
    },
    {
        "key": "direktemeldtStillingskategori",
        "value": "STILLING"
    }
]
```
* experience
  * Ingen
  * Noe - 1-3 år
  * Mye - 4+ år
* education
  * Ingen krav
  * Videregående
  * Fagbrev
  * Fagskole
  * Bachelor
  * Master
  * Forskningsgrad
* needDriversLicense
  * true - Krever førerkort
  * false - Krever ikke førerkort

### GitHub

* [pam-stillingsok](https://github.com/navikt/pam-stillingsok)
* [pam-stilling-feed](https://github.com/navikt/pam-stilling-feed)
* [pam-stilling-feed-admin](https://github.com/navikt/pam-stilling-feed-admin)
* [pam-ad-broapplikasjoner](https://github.com/navikt/pam-ad-broapplikasjoner)
* [arbeidsplassen-search-api](https://github.com/navikt/arbeidsplassen-search-api)
* [arbeidsplassen-topics](https://github.com/navikt/arbeidsplassen-topics)
* [arbeidsplassen-occupations](https://arbeidsplassen-api.nav.no/stillingsimport/api/v1/categories/pyrk/occupations)
