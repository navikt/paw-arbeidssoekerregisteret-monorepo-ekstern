package no.nav.paw.database.event

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application

val DataSourceReady: EventDefinition<Application> = EventDefinition()
val FlywayMigrationCompleted: EventDefinition<Application> = EventDefinition()