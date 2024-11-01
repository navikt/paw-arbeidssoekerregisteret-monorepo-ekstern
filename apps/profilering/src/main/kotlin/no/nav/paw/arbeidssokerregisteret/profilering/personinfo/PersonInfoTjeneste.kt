package no.nav.paw.arbeidssokerregisteret.profilering.personinfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.paw.aareg.AaregClient
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.aareg.AAREG_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.aareg.AaRegClientConfig
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication.AZURE_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication.m2mTokenFactory
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl.PDL_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl.PdlConfig
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl.createHttpClient
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.hentFoedselsdato
import java.time.LocalDate
import java.util.*

const val BEHANDLINGSNUMMER = "B452"

fun interface PersonInfoTjeneste {
    fun hentPersonInfo(identitetsnummer: String, opplysningsId: UUID): PersonInfo

    companion object {
        fun create(): PersonInfoTjeneste {
            val aaregConfig = loadNaisOrLocalConfiguration<AaRegClientConfig>(AAREG_CONFIG_FILE)
            val pdlConfig = loadNaisOrLocalConfiguration<PdlConfig>(PDL_CONFIG_FILE)
            val m2mTokenFactory = m2mTokenFactory(loadNaisOrLocalConfiguration(AZURE_CONFIG_FILE))
            val aaregClient = AaregClient(aaregConfig.url) { m2mTokenFactory.create(aaregConfig.scope) }
            val pdlClient = PdlClient(
                url = pdlConfig.url,
                tema = pdlConfig.tema,
                httpClient = createHttpClient()
            ) { m2mTokenFactory.create(pdlConfig.scope) }
            return PersonInfoTjeneste { identitetsnummer, opplysningsId ->
                runBlocking(context = Dispatchers.IO) {
                    val arbeidsforholdDeferred = async {
                        aaregClient.hentArbeidsforhold(
                            ident = identitetsnummer,
                            callId = opplysningsId.toString()
                        )
                    }
                    val foedselsdatoDeferred = async {
                        pdlClient.hentFoedselsdato(
                            ident = identitetsnummer,
                            callId = opplysningsId.toString(),
                            navConsumerId = ApplicationInfo.name,
                            behandlingsnummer = BEHANDLINGSNUMMER
                        )
                    }
                    foedselsdatoDeferred.await().let { foedselsdato ->
                        PersonInfo(
                            foedselsdato = foedselsdato?.foedselsdato?.let(LocalDate::parse),
                            foedselsAar = foedselsdato?.foedselsaar,
                            arbeidsforhold = arbeidsforholdDeferred.await()
                        )
                    }
                }
            }
        }
    }
}
