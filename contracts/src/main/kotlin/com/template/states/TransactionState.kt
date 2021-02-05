package com.template.states

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.template.contracts.TransactionContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party


@BelongsToContract(TransactionContract::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class TransactionState(
        val id: String,
        val flow: String,
        val input: String,
        val output: String,
        val notary: String,
        val commands: String,
        val fileHash: String,
        val owner: Party,
        override val linearId: UniqueIdentifier) : LinearState {

    override val participants: List<AbstractParty> = listOf(owner)

    /**
     * Erstellt eine Map, die vom Server in ein JSON Format gebracht wird
     * */
    fun toJson(consumed: Boolean): Map<String, Any> {
        return mapOf(
                "id" to this.id,
                "flow" to this.flow,
                "input" to this.input,
                "output" to this.output,
                "notary" to this.notary,
                "commands" to this.commands,
                "fileHash" to this.fileHash,
                "linearId" to this.linearId.toString(),
                "consumed" to consumed
        )
    }
}
