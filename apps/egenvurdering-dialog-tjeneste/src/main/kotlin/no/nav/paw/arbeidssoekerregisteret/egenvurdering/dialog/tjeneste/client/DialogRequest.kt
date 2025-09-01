package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client

@JvmInline
value class DialogId(val value: String)

@ConsistentCopyVisibility
data class DialogRequest private constructor(
    val tekst: String,
    val dialogId: String? = null,
    val overskrift: String? = null,
    val fnr: String? = null,
    val venterPaaSvarFraNav: Boolean
) {
    companion object {
        fun nyTråd(
            tekst: String,
            overskrift: String,
            fnr: String? = null,
            venterPaaSvarFraNav: Boolean
        ) = DialogRequest(
            tekst = tekst,
            dialogId = null,
            overskrift = overskrift,
            fnr = fnr,
            venterPaaSvarFraNav = venterPaaSvarFraNav
        )

        fun nyMelding(
            tekst: String,
            dialogId: DialogId,
            fnr: String? = null,
            venterPaaSvarFraNav: Boolean
        ) = DialogRequest(
            tekst = tekst,
            dialogId = dialogId.value,
            overskrift = null,
            fnr = fnr,
            venterPaaSvarFraNav = venterPaaSvarFraNav
        )
    }
}

