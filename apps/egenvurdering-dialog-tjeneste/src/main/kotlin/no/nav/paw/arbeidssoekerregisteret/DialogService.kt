package no.nav.paw.arbeidssoekerregisteret

import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecords

private val logger = buildApplicationLogger

class DialogService {
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        logger.info("Received ${records.count()} records from Kafka")
    }
}

/*
* Dialogmelding ved egenvurdering

En oversikt over hvilke dialogmelding som skal sendes på bakgrunn av egenvurderingen fra arbeidssøker

ANTATT_GODE_MULIGHETER

Dersom personen er profilert til ANTATT_GODE_MULIGHETER skal følgende dialogmelding sendes dersom egenvurderingen også er ANTATT_GODE_MULIGHETER.

Overskrift:

Egenvurdering <dagens dato>

tekst:

Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav.

Min vurdering: Jeg klarer meg uten veileder

Dette er en automatisk generert melding
venterPaaSvarFraNav:
false



Dersom personen er profilert til ANTATT_GODE_MULIGHETER skal følgende dialogmelding sendes dersom egenvurderingen er ANTATT_BEHOV_FOR_VEILEDNING.

Overskrift:

Egenvurdering <dagens dato>

tekst:

Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav.

Min vurdering: Jeg trenger en veileder for å komme i arbeid

Dette er en automatisk generert melding
venterPaaSvarFraNav:
true



ANTATT_BEHOV_FOR_VEILEDNING

Dersom personen er profilert til ANTATT_BEHOV_FOR_VEILEDNING skal følgende dialogmelding sendes dersom egenvurderingen også er ANTATT_BEHOV_FOR_VEILEDNING.

Overskrift:

Egenvurdering <dagens dato>

tekst:

Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.

Min vurdering: Ja, jeg ønsker hjelp

Dette er en automatisk generert melding
venterPaaSvarFraNav:
true



Dersom personen er profilert til ANTATT_BEHOV_FOR_VEILEDNING skal følgende dialogmelding sendes dersom egenvurderingen er ANTATT_GODE_MULIGHETER.

Overskrift:

Egenvurdering <dagens dato>

tekst:

Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.

Min vurdering: Nei, jeg vil gjerne klare meg selv

Dette er en automatisk generert melding
venterPaaSvarFraNav:
false


* */
