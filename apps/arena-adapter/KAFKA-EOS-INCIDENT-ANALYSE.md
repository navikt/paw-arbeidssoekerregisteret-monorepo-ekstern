# Incident-analyse: TopicsJoin-invariant brutt i produksjon

## Bakgrunn

I produksjon ble følgende warning logget av `forsinkelsePunctuation`:

```
TopicJoin invariant brutt: periode=null, profilering=true,
metadata=ForsinkelseMetadata(...), partition=0
```

**Invarianten som ble brutt:**
> Hvis `ventendePeriodeStore[X]` eksisterer, skal `periodeStateStore[X].periode != null` alltid gjelde.

Fuzz-tester og kodegjennomgang har bekreftet at **invarianten ikke kan brytes gjennom normal kodeflyt** — verken via meldingsrekkefølge, timing eller punctuator-kjøring. Se `STATE-STORE-FLOW.md` for beslutningstrær og testene i `FuzzInvariantVentendeStorePeriodeErAldriNull.kt`.

## Infrastruktur-kontekst

Hendelsen samsvarte i tid med en Aiven Kafka-hendelse der:
- Et topic ble redusert fra 2,6 TB til 0,3 TB, noe som trigget disk-rebalansering i clusteret
- Noen brokere klarte ikke å replikere data som forventet
- Clusteret brukte uvanlig lang tid på å gjenopprette normal replikering
- Aiven er kontaktet for å avklare nøyaktig hva som skjedde

## Kritisk kontekst om appen

| Egenskap | Verdi |
|---|---|
| Processing guarantee | `EXACTLY_ONCE_V2` (`withExactlyOnce()` i `App.kt`) |
| Lokal state (RocksDB) | Ephemeral (ingen PVC i `nais.yaml`) → full restore fra changelogs ved restart |
| Changelog-topics | Opprettes og eies av Aiven via `kafka.streams: true` |

Med EOS V2 skrives `periodeStateStore-changelog` og `ventendePeriodeStore-changelog` i **samme Kafka-transaksjon** per commit-interval. Under normale omstendigheter er begge synlige (`read_committed`) eller ingen — noe som forklarer hvorfor invarianten aldri kan brytes av koden alene.

---

## Mulige årsaker

### Scenario 1: `WriteTxnMarkers` partial delivery ⭐ Mest sannsynlig

EOS-transaksjonens commit-protokoll har ett strukturelt svakt punkt:

```
1. Producer kaller commitTransaction()
2. Koordinator skriver "PrepareCommit" til __transaction_state
3. Koordinator sender WriteTxnMarkers til ALLE involverte partition-ledere parallelt:
   a. periodeStateStore-changelog    [broker A – under replikeringspress]
   b. ventendePeriodeStore-changelog [broker B – stabil]
4. Koordinator skriver "CompleteCommit" til __transaction_state
```

**Bruddpunktet:** Hvis broker A mottar commit-markøren men ikke rekker å replikere den til ISR
*før broker A mister leadership* (p.g.a. disk-rebalansering):

- Ny leader for changelog-partition A velges fra ISR — ISR mangler commit-markøren
- `periodeStateStore-changelog`: data-records finnes, men **ingen COMMIT-markør**
- `ventendePeriodeStore-changelog`: COMMIT-markør er replikert → data **synlig**

Ved neste restore (app-restart):
- `periodeStateStore` mangler transaksjonen → `topicsJoin[X] = null`
- `ventendePeriodeStore` har transaksjonen → `ventende[X] = ForsinkelseMetadata(...)`
- **→ Invariant brutt**

Direkte kobling til hendelsen: "noen brokere klarte ikke å replikere data som forventet"
= ISR-replika mangler commit-markører for transaksjoner som var i `WriteTxnMarkers`-fasen
da leader-byttet skjedde.

---

### Scenario 2: `__transaction_state`-koordinator failover

Koordinatoren for EOS-transaksjoner er selv en Kafka-partition (`__transaction_state`).
Aivens disk-rebalansering kan ha:

1. Flyttet leader for `__transaction_state`-partition
2. Koordinator-failover i vinduet mellom `PrepareCommit` og `CompleteCommit`
3. Ny koordinator ser `PrepareCommit` og forsøker å fullføre transaksjonen
4. Hvis en changelog-partition er utilgjengelig → commit-markør sendt til noen men ikke alle
5. Asymmetrisk synlighet → invariant brutt

---

### Scenario 3: Under-replikert ISR + data-tap etter ACK

EOS bruker `acks=all`, men dette betyr "alle i ISR" — **ikke alle replika**.
Hvis ISR krympet til 1 (kun leader) under disk-rebalanseringen:

- `acks=all` returnerer suksess med én broker
- Leader-broker mister data etter å ha sendt ACK (flyttes eller krasjer)
- Log-trunkering til ISR-replika som aldri hadde dataene
- Neste restore: changelog mangler de siste committede records

De to changelog-topics kan ligge på forskjellige brokere med ulik ISR-situasjon,
noe som gir asymmetrisk tap og kan bryte invarianten.

---

### Scenario 4: Zombie-fencing svikt (lavere sannsynlighet)

Dersom `replicas > 1` og Aiven-hendelsen forårsaket nettverksisolasjon:

1. Consumer group rebalance → ny instans tar over partition
2. Gammel instans fikk ikke beskjed (nettverksproblemer)
3. To instanser skriver til de to changelog-topics i konflikt
4. EOS V2 skal fence ut gammel instans, men hvis fencing-broker selv var utilgjengelig...
5. Overlappende skrivinger kan skape inkonsistent state

---

## Prioritering

| Scenario | Sannsynlighet | Kobling til Aiven-hendelsen |
|---|---|---|
| 1. `WriteTxnMarkers` partial delivery | **Høy** | Direkte: leader-bytte under replikeringspress |
| 2. `__transaction_state` koordinator-failover | **Middels** | Direkte: enhver broker-flytt kan ramme koordinatoren |
| 3. Under-replikert ISR + data-tap etter ACK | **Middels** | Direkte: "klarte ikke å replikere data" |
| 4. Zombie-fencing svikt | **Lav** | Indirekte: krever nettverksisolasjon i tillegg |

---

## Konklusjon

Feilen skyldes **ikke en bug i applikasjonskoden**. Fuzz-tester med over 1400 topologi-kjøringer
samt direkte state-injeksjon bekrefter at invarianten aldri kan brytes via normal kodeflyt.

Årsaken er en **garanti-svikt i Kafkas EOS storage-lag** under en uvanlig klynge-hendelse
— mest sannsynlig asymmetrisk tap av transaksjons-commit-markører mellom to changelog-partitions
som lå på brokere med ulik replikeringsstatus.

## Hva dere kan be Aiven om å avklare

1. **Ble leader elections utført for changelog-topics til `paw-arbeidssokerregisteret-arena-adapter`?**
   Se etter `LeaderEpoch`-endringer i topic metadata for de interne Kafka Streams-topicsene.

2. **Hva var ISR-størrelsen for disse changelog-partitions under hendelsen?**
   Falt ISR til 1?

3. **Ble `__transaction_state`-partitions påvirket av rebalanseringen?**
   (Koordinator-failover)

4. **Ble det utført log-trunkering på noen changelog-partitions?**
   Se etter `OffsetForLeaderEpoch`-kall i broker-logger.

## Relaterte filer

| Fil | Innhold |
|---|---|
| `STATE-STORE-FLOW.md` | Beslutningstrær for alle kodeveier i topologien |
| `src/test/.../VerifiserInvariantVentendeStorePeriodeErAldriNull.kt` | 9 strukturerte tester for alle meldingsrekkefølger og timing-kombinasjoner |
| `src/test/.../FuzzInvariantVentendeStorePeriodeErAldriNull.kt` | Fuzz-tester (1400+ kjøringer) + direkte state-injeksjonstest |
| `src/main/.../TopologyPunctuation.kt` | `logger.warn` erstattet med `throw IllegalStateException` for tidlig deteksjon |
