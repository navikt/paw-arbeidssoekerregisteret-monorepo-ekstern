# AIA Microfrontend Toggler

Denne applikasjonen skal lytte på hendelser i arbeidssøkerdomenet, spesielt start/stopp av perioder. Hendelsene skal
tolkes for å bestemme hvilke microfrontends som skal være aktivert eller deaktivert på "Min Side" for
respektive brukere. Dette kontrolleres ved å sende en ny hendelse på `aapen-microfrontend-v1` topic'en
i Kafka. Microfrontenden som skal toggles har ID `aia-min-side`.

* [Repo for topic](https://github.com/navikt/min-side-microfrontend-topic-iac)
* [Beskrivelse av toggle-funksjonalitet](https://navikt.github.io/tms-dokumentasjon/microfrontend/#toggle-pa-microfrontend)


```
 rapportering-endringslogg topic
    rapportering.tilgjengelig event mottatt
       :send varsel(name=opprett type=oppgave) event
    rapportering.melding_mottatt event mottatt
       vil fortsette som arbeidssøker
          :send varsel(name=inaktiver type=oppgave) event
       vil slutte som arbeidssøker
          for alle aktive oppgaver
             :send varsel(name=inaktiver type=oppgave) event
    rapportering.leveringsfrist_naermer_seg event mottatt TODO: Definisjon mangler?
       :send varsel(event_name=opprett type=innboks eksternVarsel=false varselId=2) event
    rapportering.leveringsfrist_utloept event mottatt
       :send varsel(event_name=inaktiver type=innboks) event
       :send varsel(event_name=opprett type=innboks eksternVarsel=true) event
    rapportering.grace_periode_utloept event mottatt
```