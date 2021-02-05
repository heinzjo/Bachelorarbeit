package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TransactionContract
import com.template.states.TransactionState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.internal.dependencies
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

/**
 * Dieser Flow erzeugt zu jeder neuen Transaktion im
 * Vault des ausführenden Knotens ein TransactionState
 * */
@InitiatingFlow
@StartableByRPC
class TraceFlow: FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Trace Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Notar ausgewählt")

        val cmd = Command(TransactionContract.Commands.Create(), listOf(this.ourIdentity.owningKey))
        logger.info("Command erstellt")

        val signedList = serviceHub.validatedTransactions.track().snapshot
        var ledgerList = emptyList<Pair<LedgerTransaction, String>>()

        val statesInVaultTX = serviceHub.vaultService.queryBy<TransactionState>().states

        var statesInVaultID = emptyList<String>()

        for (tmp in statesInVaultTX) {
            statesInVaultID = statesInVaultID.plus(tmp.state.data.id)
        }


        for (elem in signedList) {
            ledgerList = ledgerList + (Pair(elem.toLedgerTransaction(serviceHub), removeBraces(elem.dependencies.toString())))
        }

        val txBuilder = TransactionBuilder(notary)
                .addCommand(cmd)

        for (elem in ledgerList) {
            if (elem.first.commands.toString().contains("TransactionContract")) {
                continue
            }

            if (statesInVaultID.contains(elem.first.id.toString())) {
                logger.info("Continue")
                continue
            }

            var flow: String

            val com = elem.first.commands

            var resultList = emptyList<String>()
            for (tmp in com.indices) {
                val comShort = com.map { it.value.toString().substringAfter("contracts.").substringBefore("@") }
                val com2 = comShort[tmp].substringAfter("Commands$")
                resultList = resultList.plus(com2)
            }

            when {
                resultList.contains("Produce") -> {
                    flow = "ProduceFlow"
                }
                resultList.contains("Cert") -> {
                    flow = "CertFlow"
                }
                resultList.contains("Check") -> {
                    flow = "CertGrapeFlow"
                }
                (resultList.contains("Request")) -> {
                    flow = "RequestFlow"
                }
                (resultList.contains("Trade")) -> {
                    flow = "TradeFlow"
                }
                else -> {
                    flow = "undefined"
                }
            }
            val resInput = if(removeBraces(elem.second) == ""){
                "Keine"
            } else {
                elem.second
            }
            val outputs = removeBraces(elem.first.outputStates.toString())
            val fileHashTmp = outputs.substringAfter("fileHash=").substringBefore(",")
            val fileHash = if (outputs.contains("fileHash") && fileHashTmp != "null") {
                fileHashTmp
            } else {
                "Kein"
            }

            txBuilder.addOutputState(TransactionState(
                    id = elem.first.id.toString(),
                    flow = flow,
                    input = resInput,
                    output = outputs,
                    notary = elem.first.notary?.name.toString(),
                    commands = removeBraces(resultList.toString()),
                    fileHash = fileHash,
                    owner = this.ourIdentity,
                    linearId = UniqueIdentifier()
            ), TransactionContract.TRANSACTION_CONTRACT_ID)
        }


        txBuilder.verify(serviceHub)

        val tx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = emptyList<FlowSession>()

        logger.info("Teilnehmer der Flows erhalten")
        val stx = subFlow(CollectSignaturesFlow(tx, sessions))
        logger.info("Signaturen gesammelt")
        logger.info("Flow beendet\n")
        return subFlow(FinalityFlow(stx, sessions))
    }
}

fun removeBraces(elem: String): String{
    return elem.substringAfter("[").substringBefore("]")
}
