package com.template.states

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.template.contracts.GrapeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import java.time.LocalDateTime

@BelongsToContract(GrapeContract::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class GrapeState(
        val owner: Party,
        val manufacturer: Party,
        val weight: String,
        val description :String,
        val harvest_date: LocalDateTime,
        val current_date: LocalDateTime,
        val values: Map<String, String>,
        val certified: Boolean,
        val fileHash: AttachmentId?,
        override val linearId: UniqueIdentifier) : LinearState {

    /**
     * Erstellt eine Map, die vom Server in ein JSON Format gebracht wird
     * */
    fun toJSON(consumed: Boolean): Map<String, Any> {

        return if (this.values.isEmpty()) {
            mapOf("ID" to this.linearId.toString(),
                    "Besitzer" to this.owner.toString(),
                    "Erzeuger" to this.manufacturer.toString(),
                    "Gewicht (kg)" to this.weight,
                    "Beschreibung" to this.description,
                    "Erntedatum" to this.harvest_date.toString(),
                    "Letzte Änderung" to this.current_date.toString(),
                    "Zertifiziert" to this.certified.toString(),
                    "consumed" to consumed,
                    "fileHash" to fileHash.toString()
                    )
        } else {
            mapOf("ID" to this.linearId.toString(),
                    "Besitzer" to this.owner.toString(),
                    "Erzeuger" to this.manufacturer.toString(),
                    "Gewicht (kg)" to this.weight,
                    "Beschreibung" to this.description,
                    "Erntedatum" to this.harvest_date.toString(),
                    "Letzte Änderung" to this.current_date.toString(),
                    "Zertifiziert" to this.certified.toString(),
                    "Sonstiges" to this.values,
                    "consumed" to consumed,
                    "fileHash" to fileHash.toString()
            )
        }
    }
    /**
     * Erstellt eine Kopie des States mit einem neuen Nutzer
     * */
    fun withNewOwner(party: Party, attachmentID: AttachmentId): GrapeState {
        return this.copy(owner = party, current_date = LocalDateTime.now(), fileHash = attachmentID)
    }

    /**
     * Erstellt eine Kopie des States mit einem neuen Wert für certified
     * */
    fun withNewValue(value: Boolean): GrapeState {
        return this.copy(certified = value, current_date = LocalDateTime.now())
    }

    override val participants: List<AbstractParty> = listOf(owner)
}
