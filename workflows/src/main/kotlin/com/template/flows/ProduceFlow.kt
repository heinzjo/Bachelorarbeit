package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GrapeContract
import com.template.states.GrapeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Dieser Flow erzeugt einen oder mehrere GrapeStates
 * */
@InitiatingFlow
@StartableByRPC
class ProduceFlow(
        private val weight: Int,
        private val number: Int,
        private val desc: String,
        private val values: String
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val logger = LoggerFactory.getLogger(this.javaClass.name)
        logger.info("Produce Flow gestartet")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        logger.info("Notar ausgewählt")

        val currentTime = LocalDateTime.now()
        logger.info("CurrentTime: $currentTime")

        val cmd = Command(GrapeContract.Commands.Produce(), listOf(this.ourIdentity.owningKey))
        logger.info("Command erstellt")

        val map = if (values == "") {
            emptyMap()
        } else {
            values.replace(" ", "").split("(,)")
                    .map { it.substringBefore(":") to it.substringAfter(":") }
                    .toMap()
        }

        var grapeList = emptyList<GrapeState>()

        for (i in 1..number) {
            grapeList = grapeList.plus(GrapeState(
                    this.ourIdentity,
                    this.ourIdentity,
                    weight.toString(),
                    desc,
                    currentTime,
                    currentTime,
                    map,
                    false,
                    null,
                    UniqueIdentifier()))
        }

        logger.info("Grapestate(s) angelegt")

        val txBuilder = TransactionBuilder(notary)
                .addCommand(cmd)

        for (i in 0 until number) {
            txBuilder.addOutputState(grapeList[i], GrapeContract.GRAPE_CONTRACT_ID)

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