package com.template.states

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.template.contracts.RequestContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(RequestContract::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)


data class RequestState(val description: String,
                        val certificationBody: Party,
                        val owner: Party,
                        override val linearId: UniqueIdentifier) : LinearState {

    override val participants: List<AbstractParty> = listOf(owner, certificationBody)

    /**
     * Erstellt eine Map, die vom Server in ein JSON Format gebracht wird
     * */
    fun toJSON(consumed: Boolean): Map<String, Any> {
        return mapOf("ID" to this.linearId.toString(),
                "Besitzer" to this.owner.toString(),
                "Aussteller" to this.certificationBody.toString(),
                "Beschreibung" to this.description,
                "consumed" to consumed
        )


    }
}
