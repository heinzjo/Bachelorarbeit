package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CertContract
import com.template.contracts.RequestContract
import com.template.states.CertState
import com.template.states.RequestState
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
import java.time.LocalDateTime

/**
 * Dieser Flow erzeugt ein CertState und archiviert dabei einen RequestState
 * */
@InitiatingFlow
@StartableByRPC
class CertFlow(
        private val description: String,
        private val owningParty: Party,
        private val period: Long,
        private val filehash: String,
        private val number: Int,
        private val requestID: String
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Flow:  Cert Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Flow:  Notar ausgewählt")

        val commandCert = Command(CertContract.Commands.Cert(), listOf(this.ourIdentity.owningKey, owningParty.owningKey))
        logger.info("FLow:  Command erstellt")

        val commandRequest = Command(RequestContract.Commands.Use(),listOf(this.ourIdentity.owningKey,owningParty.owningKey))

        val queryReq = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(requestID)))
        logger.info("Flow:  QueryCriteria bzgl. requestState gefunden")

        val inputReq = serviceHub.vaultService.queryBy<RequestState>(queryReq).states.single()
        logger.info("Flow:  InputRequestState ausgelesen")

        val currentTime = LocalDateTime.now()
        logger.info("Flow:  Zeit ausgelesen: $currentTime")

        logger.info("serviceHub " + serviceHub.validatedTransactions.toString())

        var certList = emptyList<CertState>()

        for(i in 1..number){
            certList = certList.plus(CertState(
                    this.ourIdentity,
                    owningParty,
                    currentTime,
                    currentTime.plusDays(period),
                    false,
                    null,
                    description,
                    filehash,
                    UniqueIdentifier()))
        }

        val txBuilder = TransactionBuilder(notary)
                .addCommand(commandCert)
                .addCommand(commandRequest)
                .addInputState(inputReq)

        for(i in certList){
            txBuilder.addOutputState(i, CertContract.Cert_CONTRACT_ID)
        }

        txBuilder.verify(serviceHub)

        logger.info("Flow:  txBuilder.verify aufgerufen")

        val tx = serviceHub.signInitialTransaction(txBuilder)
        logger.info("Flow:  Erstes Signieren der Transaktion")

        val sessions = initiateFlow(owningParty)
        logger.info("Flow:  Teilnehmer der Flows erhalten")
        logger.info("sessions: ${listOf(sessions.toString())}")
        val stx = subFlow(CollectSignaturesFlow(tx, listOf(sessions)))
        logger.info("Flow:  Signaturen gesammelt")

        logger.info("Flow beendet\n")

        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(CertFlow::class)
class CertFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Flow:  Aufruf Call CertFlowResponder")

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                logger.info("Flow:  Aufruf checkTransaction")
                //todo

            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}