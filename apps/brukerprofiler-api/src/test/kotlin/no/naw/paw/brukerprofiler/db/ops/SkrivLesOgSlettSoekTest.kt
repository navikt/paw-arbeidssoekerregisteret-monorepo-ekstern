package no.naw.paw.brukerprofiler.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.brukerprofiler.db.initDatabase
import no.naw.paw.brukerprofiler.domain.Fylke
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.Kommune
import no.naw.paw.brukerprofiler.domain.StedSoek
import no.naw.paw.brukerprofiler.domain.stedSoek
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

class SkrivLesOgSlettSoekTest : FreeSpec({
    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    val periodeFactory = PeriodeFactory.create()
    val metadataFactory = MetadataFactory.create()
    beforeSpec { Database.connect(dataSource) }

    val periode = periodeFactory.build(avsluttet = null)
    "Verifiser at vi kan skrive, hente og slette soek" - {
        "Vi oppretter en ny som vi kan koble soekene mot" {
            transaction {
                opprettOgOppdaterBruker(periode)
            }
        }
        val brukerId = transaction {
            requireNotNull(hentBrukerProfil(Identitetsnummer(periode.identitetsnummer))) {
                "Bruker som skulle ha blitt opprettet i forrige steg finnes ikke"
            }.id
        }
        "Vi får en tom liste når vi henter soek for en bruker uten soek" {
            val soek = transaction {
                hentSoek(brukerId)
            }
            soek.size shouldBe 0
        }
        "Vi kan lagre et StedSoek og hente det ut igjen" - {
            val soek = stedSoek(
                fylker = listOf(
                    Fylke(
                        navn = "Vestland",
                        kommuner = listOf(
                            Kommune(
                                navn = "Bergen",
                                kommunenummer = "4601"
                            ),
                            Kommune(
                                navn = "Askøy",
                                kommunenummer = "4604"
                            )
                        ),
                        fylkesnummer = "46"
                    )
                ),
                soekeord = listOf("Utvikler", "Rust")
            )
            val tidspunkt = Instant.now()
            "Vi lagrer soeket uten feil" {
                transaction {
                    lagreSoek(brukerId, tidspunkt, soek)
                }
            }
            "Vi kan hente soeket vi nettopp lagret" {
                val hentetSoek = transaction {
                    hentSoek(brukerId)
                }
                hentetSoek.size shouldBe 1
                val lagretSoek = hentetSoek[0]
                lagretSoek.brukerId shouldBe brukerId
                lagretSoek.opprettet shouldBe tidspunkt.truncatedTo(ChronoUnit.MILLIS)
                lagretSoek.soek shouldBe soek
            }
            "Vi kan slette soeket vi nettopp lagret" {
                val soekId = transaction {
                    hentSoek(brukerId)
                }.firstOrNull()?.id!!
                val antallSlettet = transaction {
                    slettSoek(brukerId, soekId)
                }
                antallSlettet shouldBe 1
                val hentetEtterSletting = transaction {
                    hentSoek(brukerId)
                }
                hentetEtterSletting.size shouldBe 0
            }
        }
    }
})
