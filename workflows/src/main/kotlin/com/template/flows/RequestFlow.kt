package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RequestContract
import com.template.states.RequestState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

/**
 * Dieser Flow erzeugt einen oder mehrere RequestStates
 * */
@InitiatingFlow
@StartableByRPC
class RequestFlow(
        private val description: String,
        private val certificationBody: Party,
        private val number: Int
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Request Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Notar ausgewählt")

        val cmd = Command(RequestContract.Commands.Request(), listOf(this.ourIdentity.owningKey,certificationBody.owningKey))
        logger.info("Command erstellt")

        var requestList = emptyList<RequestState>()

        for(i in 1..number){
            requestList = requestList.plus(RequestState(
                    description,
                    certificationBody,
                    this.ourIdentity,
                    UniqueIdentifier()))
        }


        logger.info("Requeststate(s) angelegt")

        val txBuilder = TransactionBuilder(notary)
                .addCommand(cmd)

        for (i in 0 until number){
            txBuilder.addOutputState(requestList[i],RequestContract.REQUEST_CONTRACT_ID)
        }
        txBuilder.verify(serviceHub)

        val tx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = initiateFlow(certificationBody)

        logger.info("Teilnehmer der Flows erhalten")
        val stx = subFlow(CollectSignaturesFlow(tx, listOf(sessions)))
        logger.info("Signaturen gesammelt")
        logger.info("Flow beendet\n")
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(RequestFlow::class)
class RequestFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Flow:  Aufruf Call RequestFlowResponder")

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                logger.info("Flow:  Aufruf checkTransaction")

            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
