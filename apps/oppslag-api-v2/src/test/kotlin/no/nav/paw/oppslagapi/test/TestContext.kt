package no.nav.paw.oppslagapi.test

import io.mockk.mockk
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.factory.mockKafkaKeysClient
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.DatabaseQuerySupport
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val testLogger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.test")

data class TestContext(
    val mockOAuthServer: MockOAuth2Server = MockOAuth2Server(),
    val mockKafkaKeysClient: KafkaKeysClient = mockKafkaKeysClient(),
    val kafkaKeysClientMock: KafkaKeysClient = mockk(),
    val tilgangsTjenesteForAnsatteMock: TilgangsTjenesteForAnsatte = mockk(),
    val autorisasjonsTjeneste: AutorisasjonsTjeneste = AutorisasjonsTjeneste(
        kafkaKeysClient = mockKafkaKeysClient,
        tilgangsTjenesteForAnsatte = tilgangsTjenesteForAnsatteMock,
        auditLogger = AuditLogger.getLogger()
    ),
    val databaseQuerySupportMock: DatabaseQuerySupport = mockk(),
    val mockedQueryLogic: ApplicationQueryLogic = ApplicationQueryLogic(
        kafkaKeysClient = mockKafkaKeysClient,
        autorisasjonsTjeneste = autorisasjonsTjeneste,
        databaseQuerySupport = databaseQuerySupportMock
    )
)
