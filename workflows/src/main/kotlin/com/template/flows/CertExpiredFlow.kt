package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CertContract
import com.template.states.CertState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

/**
 * Dieser Flow wird automatisch gestartet, wenn ein
 * Zertifikat abläuft und archiviert dabei einen CertState
 * */
@InitiatingFlow
@SchedulableFlow
class CertExpiredFlow(
        private val stateRef: StateRef
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Flow:  CertExpired Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Flow:  Notar ausgewählt")

        val inputCert = serviceHub.toStateAndRef<CertState>(stateRef)
        logger.info("Flow:  CertState gefunden")

        val outputCert = inputCert.state.data.withNewValue()
        logger.info("Flow:  Entsprechender State ausgelesen")

        val certCmd = Command(CertContract.Commands.Revoke(),listOf(this.ourIdentity.owningKey))

        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputCert)
                .addOutputState(outputCert, CertContract.Cert_CONTRACT_ID)
                .addCommand(certCmd)
        logger.info("Flow:  TxBuilder wurde erstellt\n")

        txBuilder.verify(serviceHub)

        logger.info("Flow:  txBuilder.verify aufgerufen")

        val tx = serviceHub.signInitialTransaction(txBuilder)
        logger.info("Flow:  Erstes Signieren der Transaktion")

        val sessions = (outputCert.participants - this.ourIdentity).map { initiateFlow(it as Party) }
        logger.info("Flow:  Teilnehmer der Flows erhalten")

        val stx = subFlow(CollectSignaturesFlow(tx, sessions))
        logger.info("Flow:  Signaturen gesammelt")

        logger.info("Flow beendet\n")

        return subFlow(FinalityFlow(stx, sessions))
    }
}