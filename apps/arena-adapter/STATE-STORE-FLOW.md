# State store flow — arena-adapter

Beslutningstrær for de tre prosessorene. `store` = `topicsJoinStore`, `ventende` = `ventendePeriodeStore`.

---

## 1. Periode lest (`haandter_periode`)

```
periode lest
├── avsluttet != null
│   ├── store.delete(X)
│   ├── ventende.delete(X)
│   └── → forward {periode, null}
│       store: -, ventende: -
│
└── avsluttet == null
    ├── store[X] == null  (ingen kjent tilstand)
    │   ├── store.put(X, {periode, null, null})
    │   └── ventende.putIfAbsent(X, metadata)
    │       store: {periode, null}, ventende: metadata
    │
    └── store[X] != null
        ├── store[X].periode != null  → ingenting (idempotent)
        │
        └── store[X].periode == null  (profilering kom først)
            ├── store.put(X, {periode, profilering, null})
            ├── profilering != null → forward, ingen ventende-entry
            │   store: {periode, profilering}, ventende: uendret
            └── profilering == null → ventende.putIfAbsent(X, metadata)
                store: {periode, null}, ventende: metadata
```

---

## 2. Profilering lest (`haandter_profilering`)

```
profilering lest
├── store[X].profilering != null  → ingenting (idempotent)
│
└── store[X].profilering == null  (inkl. store[X] == null)
    ├── store.put(X, {store[X].periode, profilering, null})
    │
    ├── store[X].periode != null
    │   ├── ventende.delete(X)
    │   └── → forward {periode, profilering}
    │       store: {periode, profilering}, ventende: -
    │
    └── store[X].periode == null  (periode ikke mottatt ennå)
        └── ingen ventende-entry, ingen forward
            store: {null, profilering}, ventende: uendret
```

---

## 3. Punctuator kjører (`forsinkelsePunctuation`, hvert 2s, terskel 5s)

```
for each X i ventende (eldre enn 5s):
│
├── store[X].periode != null  (normaltilfellet)
│   ├── ventende.delete(X)
│   └── → forward {periode, null}
│
└── store[X].periode == null  ← ⚠️ prod-case
    └── LOG WARNING, hopp over (ingenting forwardes)
```

---

## Invariant

Alle stier som legger noe i `ventende` setter **samtidig** `store[X].periode != null`.
Det finnes ingen normal kodesti som ender med `ventende[X] != null` og `store[X].periode == null`
på samme tid — tilstanden som trigger `LOG WARNING` kan ikke oppstå via gjeldende kode alene.
