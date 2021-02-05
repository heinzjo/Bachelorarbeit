package com.template.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory

/**
 * Diese Klasse ist die Implementierung der Contracts
 * die für das Anfragen von Zertifikaten genutzt werden
 * */
class RequestContract : Contract {

    companion object {
        private val logger = LoggerFactory.getLogger(RequestContract::class.java)
        val REQUEST_CONTRACT_ID = RequestContract::class.qualifiedName!!
    }

    override fun verify(tx: LedgerTransaction) {

        logger.info("RequestContract.verify aufgerufen")
        val command = tx.commands.requireSingleCommand<Commands>().value
        when (command) {

            is Commands.Request -> requireThat {
                logger.info("Contract:  Commands.Create aufgerufen")

            }
        }
        when (command) {

            is Commands.Use -> requireThat {
                logger.info("Contract:  Commands.Use aufgerufen")

            }
        }
    }


    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Request : CommandData, Commands
        class Use : TypeOnlyCommandData(), Commands
    }
}