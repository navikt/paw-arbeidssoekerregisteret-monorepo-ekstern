# paw-aareg-client

Henter arbeidsforhold fra Arbeidsgiver- og arbeidstakerregisteret ([aareg](https://navikt.github.io/aareg/)).

Se URL-er på https://github.com/navikt/aareg-services

Se dokumentasjon for aareg på https://aareg-services.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2

### Bruk av paw-aareg-client

**_gradle.build.kts_**

```kts
val tokenproviderVersion: String by project
val aaregClientVersion: String by project

dependencies {
    implementation(project(":lib:aareg-client-v2"))
}
```

### Klienten instansieres slik

```kt
import kotlinx.coroutines.runBlocking
import no.nav.paw.tokenprovider.OAuth2TokenProvider
import no.nav.paw.aareg.AaregClient

fun main() {
    val url = "https://aareg-services.intern.dev.nav.no/api/v2/arbeidstaker/arbeidsforhold"
    val tokenProvider = OAuth2TokenProvider(
        // Token config
    )

    val aaregClient = AaregClient(url) { tokenProvider.getToken() }

    val arbeidsforhold = runBlocking { aaregClient.hentArbeidsforhold("fnr", "callId") }
    println(arbeidsforhold)
}
```
