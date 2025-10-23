package no.naw.paw.minestillinger.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.Fylke
import no.naw.paw.minestillinger.domain.Kommune
import no.naw.paw.minestillinger.domain.stedSoek
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

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
            requireNotNull(hentBrukerProfilUtenFlagg(Identitetsnummer(periode.identitetsnummer))) {
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
                soekeord = listOf("Utvikler", "Rust"),
                styrk08 = listOf("123", "42")
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
                lagretSoek.brukerId shouldBe brukerId.verdi
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
