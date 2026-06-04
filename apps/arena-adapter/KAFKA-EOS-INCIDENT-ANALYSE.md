# Incident-analyse: TopicsJoin-invariant brutt i produksjon

## Sammendrag

`paw-arbeidssokerregisteret-arena-adapter` gikk i crash-loop i produksjon på grunn av en
`NullPointerException` i kode som antok at `periodeStateStore[X].periode` aldri kunne være
`null` når `ventendePeriodeStore[X]` eksisterte. Feilen var en direkte konsekvens av en
Kafka EOS-garantisvikt i Aiven-clusteret — ikke en bug i applikasjonskoden.
Årsak er bekreftet gjennom Aiven-logganalyse og tverrteam-rapporter.

---

## Hendelsesforløp

### Fase 1: Crash-loop oppdages

Appen krasjet gjentatte ganger med `NullPointerException` i `forsinkelsePunctuation`.
Koden antok at `topicsJoin.periode != null` alltid holdt når en nøkkel fantes i
`ventendePeriodeStore`, men observerte `periode == null`.

Kodegjennomgang og fuzz-testing (se under) bekreftet at **invarianten ikke kan brytes
gjennom normal kodeflyt**. Dette pekte mot en infrastrukturårsak.

### Fase 2: Infrastruktur-korrelasjon

Krashtidspunktet samsvarte med en Aiven Kafka-hendelse 2026-06-01:
- Et topic ble redusert fra 2,6 TB til 0,3 TB → trigget disk-rebalansering i clusteret
- Brokere klarte ikke å replikere data som forventet
- Clusteret brukte uvanlig lang tid på å gjenopprette normal replikering

Aiven ble kontaktet og svarte med logg-analyse som bekreftet hendelsesforløpet nedenfor.

### Bekreftet hendelsesforløp (alle tider UTC)

```
11:08  __transaction_state-0: leder-bytte broker 43 → broker 38
       broker 38 trunkerer __transaction_state-0 til offset 0
       → ny koordinator kjenner IKKE til pågående transaksjoner

11:13  LeaderEpoch-bump på v1/v2/v4-periodeStateStore-changelog
       Replika-flytt (broker 42 inn, broker 36 ut) — samme leder (broker 35) beholdt

11:19  ISR krymper til 1 på changelog-partitions
       Broker 41 fjernes (overbelastet) → acks=all gir garanti fra kun én broker

11:20  Arena-adapter restarter (bekreftet via "Responding at http://0.0.0.0:8080" i logg)
       State-restore kjøres mot changelog under/rett etter ISR=1-vinduet

12:30  Andre bølge: flere team rapporterer Kafka-problemer
       EXACTLY_ONCE_V2 så util å skurre lit for andre team også

13:32  Første event for ny nøkkel X ankommer Kafka (15:32 CEST)
       Prosessert under eller etter andre bølge — forklarer brudd på nye nøkler
```

---

## Infrastrukturkonfigurasjon under hendelsen

Dette er en kritisk bidragsyter til alvorlighetsgraden:

| Topic-type | Replication factor | min.insync.replicas | Konfigurert av |
|---|---|---|---|
| Brukerstyrte topics (f.eks. `opplysninger-v1`) | 3 | 2 | Nais Topic-ressurs |
| Kafka Streams changelog-topics (`*-changelog`) | **1** | **1** | Broker-default (auto-opprettet) |
| `__transaction_state` | Broker-styrt | Broker-styrt | Aiven |
| `__consumer_offsets` | Broker-styrt | Broker-styrt | Aiven |

Changelog-topics ble auto-opprettet av Kafka Streams med broker-default replication factor 1.
Med én replika og `min.insync.replicas=1` gir `acks=all` garanti fra kun én broker — og ISR
kan aldri falle under 1 og gi feil. Stille datatap ved broker-failure er mulig.

**Dette er nå utbedret:** Eksisterende changelog-topics er oppdatert til
`replication=3, min.insync.replicas=2`.

---

## Applikasjonskontekst

| Egenskap | Verdi |
|---|---|
| Processing guarantee | `EXACTLY_ONCE_V2` (`withExactlyOnce()` i `App.kt`) |
| Lokal state (RocksDB) | Ephemeral (ingen PVC) → full restore fra changelogs ved restart |
| Changelog-topics | Auto-opprettet av Kafka Streams |

Med EOS V2 skrives `periodeStateStore-changelog` og `ventendePeriodeStore-changelog` i
**samme Kafka-transaksjon** per commit-interval. Under normale omstendigheter er begge
synlige (`read_committed`) eller ingen — derfor kan invarianten aldri brytes av koden alene.

---

## Bekreftet årsaksmekanisme

Aiven-undersøkelsen og tverrteam-rapporter bekrefter at koordinator-trunkering (Scenario 2)
utløste asymmetrisk WriteTxnMarkers-levering (Scenario 1). ISR=1 var en forsterkende faktor.

### Primær mekanisme: koordinator-trunkering → asymmetrisk WriteTxnMarkers

```
1. Producer kaller commitTransaction()
2. Koordinator (broker 43) skriver "PrepareCommit" til __transaction_state-0

   ── KOORDINATOR-FAILOVER ──
   broker 43 fjernes fra __transaction_state-0
   broker 38 tar over som ny leder
   broker 38 trunkerer __transaction_state-0 til offset 0
   → ny koordinator kjenner IKKE til pågående transaksjoner

3. WriteTxnMarkers sendes til partition-ledere:
   a. periodeStateStore-changelog    → commit-markør IKKE levert  ✗
   b. ventendePeriodeStore-changelog → commit-markør LEVERT       ✓
   c. __consumer_offsets             → commit-markør LEVERT       ✓

4. "CompleteCommit" skrives aldri (koordinator manglet PrepareCommit-record)
```

**Konsekvens ved restart (state restore):**

```
periodeStateStore-changelog:     ingen commit-markør → store[X] = null  (read_committed)
ventendePeriodeStore-changelog:  commit-markør OK    → ventende[X] = ForsinkelseMetadata(...)
consumer offset for Periode:     commit-markør OK    → Periode reprocesses IKKE etter restart
```

**→ Invariant brutt: ventende[X] != null og store[X].periode == null**

#### Hvorfor Profilering-reprocessing ikke hjelper

Profilering er arkitekturelt alltid produsert *etter* Periode (krever Periode + et tredje topic
som input). Første event for en ny nøkkel er derfor alltid Periode. Etter restart er
Periode-offseten committed — den reprocesses ikke. Profilering kan komme inn og oppdatere
`store[X].profilering`, men `store[X].periode` forblir null og `ventende[X]` forblir satt.
Invarianten helbredes ikke av reprocessing alene.

### Forsterkende faktor: ISR=1

`acks=all` betyr "alle i ISR". Når ISR=1 er dette funksjonelt likt `acks=1`. Med
`min.insync.replicas=1` (broker-default for changelog-topics) ga Kafka ingen feil ved
ISR-degradering — kun stille aksept med redusert garanti.

Med `min.insync.replicas=2` ville ISR=1 gitt `NotEnoughReplicasException` til klienten
i stedet for stille suksess.

### Aiven-undersøkelse: svar på fire spørsmål

| Spørsmål | Svar | Betydning |
|---|---|---|
| Leader elections på changelog-topics? | Epoch-bump, men **samme leder** — replika-*flytt*, ikke leder-bytte | Scenario 1 drives av koordinator-svikt, ikke changelog-leder-bytte |
| ISR-størrelse under hendelsen? | **ISR=1 bekreftet** 11:19–11:20 UTC (broker 41 overbelastet) | ISR-garantier reelt fraværende i dette vinduet |
| `__transaction_state` påvirket? | **Ja — bekreftet** leder-bytte + trunkering til offset 0 | Primær utløser bekreftet |
| Log-trunkering på changelog-partitions? | Ingen `OffsetForLeaderEpoch` på changelog | Koordinator-tapet er mekanismen, ikke direkte changelog-trunkering |

---

## Kodeundersøkelse

### Invarianten

> Hvis `ventendePeriodeStore[X]` eksisterer, skal `periodeStateStore[X].periode != null` alltid gjelde.

Se `STATE-STORE-FLOW.md` for komplette beslutningstrær. Alle kodeveier som setter
`ventende[X]` setter **simultant** `store[X].periode != null`. Det finnes ingen normal
kodesti som etterlater tilstanden `ventende[X] != null, store[X].periode == null`.

### Testing

- **9 strukturerte tester** (`VerifiserInvariantVentendeStorePeriodeErAldriNull.kt`):
  alle meldingsrekkefølger og timing-kombinasjoner
- **Fuzz-tester** (`FuzzInvariantVentendeStorePeriodeErAldriNull.kt`):
  1400+ tilfeldige topologi-kjøringer + direkte state-injeksjon som forsøker å sette
  `ventende[X]` uten tilhørende `store[X].periode`

Ingen av testene klarte å bryte invarianten → koden er ikke årsaken.

---

## Tiltak

### Gjennomført

| Tiltak | Beskrivelse |
|---|---|
| `NPE` | `logger.warn` erstatter NPE i `forsinkelsePunctuation` erstattet med — invariantbrudd oppdages nå uten å ta ned applikasjonen |
| Changelog-topic replication oppgradert | Eksisterende `*-changelog`-topics oppdatert til `replication=3, min.insync.replicas=2` |

### Gjenværende åpne spørsmål

## Konklusjon

Feilen skyldes **ikke en bug i applikasjonskoden**.

Årsaken er bekreftet: **EOS-garantier ble brutt i Aiven-clusteret** som følge av at
`__transaction_state-0` ble trunkert til offset 0 ved koordinator-failover. Den nye
koordinatoren kjente ikke til pågående transaksjoner og sendte aldri fullstendige
commit-markører. Kombinert med at changelog-topics hadde `replication=1` (broker-default)
var det ingen beskyttelse mot asymmetrisk datatap.

EOS-bruddet var **cluster-bredt**.

---

## Relaterte filer

| Fil | Innhold |
|---|---|
| `STATE-STORE-FLOW.md` | Beslutningstrær for alle kodeveier i topologien |
| `src/test/.../VerifiserInvariantVentendeStorePeriodeErAldriNull.kt` | 9 strukturerte tester |
| `src/test/.../FuzzInvariantVentendeStorePeriodeErAldriNull.kt` | Fuzz-tester (1400+ kjøringer) + direkte state-injeksjonstest |
| `src/main/.../TopologyPunctuation.kt` | Invariant-sjekk med `throw IllegalStateException` |
