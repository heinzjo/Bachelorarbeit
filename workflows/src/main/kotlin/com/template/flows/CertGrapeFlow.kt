package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GrapeContract
import com.template.states.GrapeState
import com.template.contracts.CertContract
import com.template.states.CertState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

/**
 * Dieser Flow zertifiziert einen GrapeState und
 * archiviert dabei einen GrapeState und einen CertState
 * */
@InitiatingFlow
@StartableByRPC
class CertGrapeFlow(
        private val grapeId: String,
        private val certId: String,
        private val description: Boolean
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Flow:  CertGrape Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Flow:  Notar ausgewählt")

        val commandGrapeState = Command(GrapeContract.Commands.Check(), listOf(this.ourIdentity.owningKey))
        logger.info("FLow:  Command erstellt")

        val queryGrape = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(grapeId)))
        logger.info("Flow:  QueryCriteria bzgl. grapeState gefunden")

        val inputGrape = serviceHub.vaultService.queryBy<GrapeState>(queryGrape).states.single()
        logger.info("Flow:  GrapeState gefunden")

        val grapeState = inputGrape.state.data
        logger.info("Flow:  Entsprechender State ausgelesen")


        val queryCert = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(certId)))
        logger.info("Flow:  QueryCriteria bzgl. grapeState gefunden")

        val inputCert = serviceHub.vaultService.queryBy<CertState>(queryCert).states.single()
        logger.info("Flow:  GrapeState gefunden")

        val commandCertState = Command(CertContract.Commands.Check(), listOf(this.ourIdentity.owningKey,inputCert.state.data.certificationBody.owningKey))

        inputCert.state.data.participants

        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputGrape)
                .addInputState(inputCert)
                .addOutputState(grapeState.withNewValue(description), GrapeContract.GRAPE_CONTRACT_ID)
                .addCommand(commandGrapeState)
                .addCommand(commandCertState)

        logger.info("Flow:  TxBuilder wurde erstellt\n")
        logger.info("   Notar: " + notary.name + ", State: " + grapeState.toString() + ", Contract: " + GrapeContract.GRAPE_CONTRACT_ID + ", Command: " + commandGrapeState.value.toString())

        txBuilder.verify(serviceHub)

        logger.info("Flow:  txBuilder.verify aufgerufen")

        val tx = serviceHub.signInitialTransaction(txBuilder)
        logger.info("Flow:  Erstes Signieren der Transaktion")

        //val sessions = (grapeState.participants + newOwner).map { initiateFlow(it as Party) }
        val sessions = (inputCert.state.data.participants - this.ourIdentity).map { initiateFlow(it as Party) }
        logger.info("Flow:  Teilnehmer der Flows erhalten")

        val stx = subFlow(CollectSignaturesFlow(tx, sessions))
        logger.info("Flow:  Signaturen gesammelt")

        logger.info("Flow beendet\n")

        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(CertGrapeFlow::class)
class CertGrapeFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Flow:  Aufruf Call TradeFlowResponder")

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                logger.info("Flow:  Aufruf checkTransaction")

                val output = stx.tx.outputs.single().data
                "The output must be a GrapeState" using (output is GrapeState)
                logger.info("Flow:  Responder: Der OutputState ist vom Typ CertState")

            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}