package no.nav.paw.hwm

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

class DbTests : FreeSpec({
    "Databasetester for Hwm" - {
        val database = "testdb"
        val username = "testuser"
        val password = "testpass"
        val dbContainer = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName(database)
            .withUsername(username)
            .withPassword(password)
            .withExposedPorts(5432)
            .waitingFor(HostPortWaitStrategy())
        dbContainer.start()
        val dbConfig = DatabaseConfig(
            username = username,
            password = password,
            database = database,
            host = dbContainer.host,
            port = dbContainer.getMappedPort(5432)
        )
        val dataSource = createHikariDataSource(dbConfig)
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(HwmTable)
        }

        "getHWM på tom db returnerer null" {
            transaction {
                val hwm = getHwm(1, "topic", 0)
                hwm shouldBe null
            }
        }
        "Vi kan kjøre insert, les og update" - {
            "insert hwm på tom db fullføres uten feil" {
                transaction {
                    insertHwm(consumerVersion = 1, topic = "topic", partition = 0, offset = 100)
                }
            }
            "insert hwm på en annen partisjon fullføres uten feil" {
                transaction {
                    insertHwm(consumerVersion = 1, topic = "topic", partition = 1, offset = 21)
                }
            }
            "getHwm på topic:1 returnerer offset 100" {
                transaction {
                    val hwm = getHwm(1, "topic", 0)
                    hwm shouldBe 100
                }
            }
            "getHwm på topic:2 returnerer offset 21" {
                transaction {
                    val hwm = getHwm(1, "topic", 1)
                    hwm shouldBe 21
                }
            }
            "update av topic:1 med høyere offset returnerer true" {
                transaction {
                    val updated = updateHwm(1, "topic", 0, 150)
                    updated shouldBe true
                }
            }
            "update av topic:1 med lavere offset returnerer false" {
                transaction {
                    val updated = updateHwm(1, "topic", 0, 120)
                    updated shouldBe false
                }
            }
            "getHwm på topic:1 returnerer offset 150" {
                transaction {
                    val hwm = getHwm(1, "topic", 0)
                    hwm shouldBe 150
                }
            }
            "getHwm på topic:2 returnerer fortsatt offset 21" {
                transaction {
                    val hwm = getHwm(1, "topic", 1)
                    hwm shouldBe 21
                }
            }
        }
    }

})
