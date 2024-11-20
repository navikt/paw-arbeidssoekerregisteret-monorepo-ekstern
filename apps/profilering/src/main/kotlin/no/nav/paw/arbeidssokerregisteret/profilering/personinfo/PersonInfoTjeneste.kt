package no.nav.paw.arbeidssokerregisteret.profilering.personinfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.paw.aareg.model.Result
import no.nav.paw.aareg.factory.createAaregClient
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import no.nav.paw.pdl.factory.createPdlClient
import no.nav.paw.pdl.hentFoedselsdato
import java.time.LocalDate
import java.util.*

const val BEHANDLINGSNUMMER = "B452"

fun interface PersonInfoTjeneste {
    fun hentPersonInfo(identitetsnummer: String, opplysningsId: UUID): PersonInfo

    companion object {
        fun create(): PersonInfoTjeneste {
            val aaregClient = createAaregClient()
            val pdlClient = createPdlClient()
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
                            arbeidsforhold = arbeidsforholdDeferred.await().let { result ->
                                when (result) {
                                    is Result.Success -> result.arbeidsforhold
                                    is Result.Failure -> emptyList()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
