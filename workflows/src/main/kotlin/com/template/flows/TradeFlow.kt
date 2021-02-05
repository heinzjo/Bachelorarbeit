package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GrapeContract
import com.template.states.GrapeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

/**
 * Dieser Flow erzeugt einen neuen GrapeState und
 * archiviert dabei den GrapeState des alten Besitzers
 * */
@InitiatingFlow
@StartableByRPC
class TradeFlow(
        private val newOwner: Party,
        private val grapeId: String,
        private val attachmentID: SecureHash
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Flow:  Trade Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Flow:  Notar ausgewählt")

        val grapeCmd = Command(GrapeContract.Commands.Trade(attachmentID), listOf(this.ourIdentity.owningKey, newOwner.owningKey))
        logger.info("Flow:  GrapeCommand erstellt")

        val queryGrape = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(grapeId)))
        logger.info("Flow:  QueryCriteria bzgl. grapeState gefunden")

        val inputGrape = serviceHub.vaultService.queryBy<GrapeState>(queryGrape).states.single()
        logger.info("Flow:  InputGrapeState ausgelesen")

        val outputGrape = inputGrape.state.data


        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputGrape)
                .addOutputState(outputGrape.withNewOwner(newOwner, attachmentID), GrapeContract.GRAPE_CONTRACT_ID)
                .addCommand(grapeCmd)
                .addAttachment(attachmentID)

        txBuilder.verify(serviceHub)

        logger.info("Flow:  txBuilder.verify aufgerufen")

        val tx = serviceHub.signInitialTransaction(txBuilder)
        logger.info("Flow:  Erstes Signieren der Transaktion")

        //val sessions = (grapeState.participants + newOwner).map { initiateFlow(it as Party) }
        val sessions = initiateFlow(newOwner)
        logger.info("Flow:  Teilnehmer der Flows erhalten")

        val stx = subFlow(CollectSignaturesFlow(tx, listOf(sessions)))
        logger.info("Flow:  Signaturen gesammelt")

        logger.info("Flow beendet\n")

        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(TradeFlow::class)
class TradeFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Flow:  Aufruf Call TradeFlowResponder")

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                logger.info("Flow:  Aufruf checkTransaction")

                val output = stx.tx.outputs.single().data
                "The output must be a GrapeState" using (output is GrapeState)
                logger.info("Flow:  Responder: Der OutputState ist vom Typ GrapeState")

            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))


    }
}