package com.template.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory

/**
 * Diese Klasse ist die Implementierung der Contracts
 * des TransactionStates
 * */
class TransactionContract : Contract {

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionContract::class.java)
        val TRANSACTION_CONTRACT_ID = TransactionContract::class.qualifiedName!!
    }

    override fun verify(tx: LedgerTransaction) {

        logger.info("Transaction Contract.verify aufgerufen")
        val command = tx.commands.requireSingleCommand<Commands>().value
        when (command) {
            is Commands.Create -> requireThat {
                logger.info("Contract:  Commands.Create aufgerufen")

            }
        }
        when (command) {
            is Commands.Use -> requireThat {
                logger.info("Contract:  Commands.Use aufgerufen")

            }
        }
    }

    interface Commands : CommandData {
        class Create : CommandData, Commands
        class Use : TypeOnlyCommandData(), Commands
    }
}