package com.template.states

import com.template.contracts.CertContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.contracts.StateRef
import java.time.LocalDateTime
import java.time.ZoneOffset

@BelongsToContract(CertContract::class)
data class CertState(
                     val certificationBody: Party,
                     val owningParty: Party,
                     val created: LocalDateTime,
                     val expires: LocalDateTime,
                     val revoked: Boolean,
                     val revocationTimestamp: LocalDateTime? = null,
                     val description: String,
                     val filehash: String,
                     override val linearId: UniqueIdentifier) : LinearState, SchedulableState {

    override val participants: List<AbstractParty> = listOf(owningParty, certificationBody)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create("com.template.flows.CertExpiredFlow", thisStateRef), expires.toInstant(ZoneOffset.UTC))

    }

    /**
     * Erstellt eine Map, die vom Server in ein JSON Format gebracht wird
     * */
    fun toJSON(consumed: Boolean): Map<String, Any> {
        return mapOf("ID" to this.linearId.toString(),
                "Besitzer" to this.owningParty.toString(),
                "Aussteller" to this.certificationBody.toString(),
                "Erstellt am" to this.created.toString(),
                "Läuft ab am" to this.expires.toString(),
                "Widerrufen" to this.revoked,
                "Widerrufen am" to revocationTimestamp.toString(),
                "Beschreibung" to this.description,
                "consumed" to consumed,
                "fileHash" to this.filehash
        )
    }
    /**
     * Erstellt eine Kopie des States mit einem neuen Wert für revoked
     * */
    fun withNewValue(): CertState {
        return this.copy(revoked = true, revocationTimestamp = LocalDateTime.now())
    }

}
