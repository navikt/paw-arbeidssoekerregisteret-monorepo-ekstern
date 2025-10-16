package no.nav.paw.ledigestillinger.model.dao

import java.time.Instant

data class MetadataRow(
    val id: Long,
    val parentId: Long,
    val status: String, // TODO: Enum
    val recordTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant?
)
