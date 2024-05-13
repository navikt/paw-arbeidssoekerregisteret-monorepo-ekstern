```mermaid
sequenceDiagram
    Registeret->>PeriodeTopic: Periode startet
    PeriodeTopic-->>MeldepliktTjeneste: Periode startet
    MeldepliktTjeneste-->>EndringerTopic: Melding X dager før forfall
    MeldepliktTjeneste-->>EndringerTopic: Melding OK ved innsending
    MeldepliktTjeneste-->>EndringerTopic: Frist utløpt
    MeldepliktTjeneste-->>EndringerTopic: Graceperiode utløpt
    PeriodeTopic-->>MeldepliktTjeneste: Periode avsluttet
    MeldepliktTjeneste-->>EndringerTopic: Periode avsluttet
```

```mermaid
sequenceDiagram
    Registeret->>PeriodeTopic: Periode startet
    Dagpenger-->>AnsvarsTopic: Dagpenger startet
    AnsvarsTopic-->>MeldepliktTjeneste: Dagpenger tar over
    MeldepliktTjeneste-->>EndringerTopic: 
```