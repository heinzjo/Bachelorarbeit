package com.template.contracts

import com.template.states.CertState
import com.template.states.GrapeState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Diese Klasse ist die Implementierung der Contracts,
 * die für das Zertifizieren benötigt werden
 * */
class CertContract : Contract {

    companion object {
        private val logger = LoggerFactory.getLogger(CertContract::class.java)
        val Cert_CONTRACT_ID = CertContract::class.qualifiedName!!
    }

    override fun verify(tx: LedgerTransaction) {

        logger.info("CertContract.verify aufgerufen")
        val command = tx.commands.requireSingleCommand<Commands>().value
        when (command) {

            is Commands.Cert -> requireThat {
                logger.info("Contract:  Command.create aufgerufen")

                "The output state must be of type CertState" using (tx.outputs[0].data is CertState)
                val outputState = tx.outputs[0].data as CertState
                logger.info("Contract:  Der OutputState ist CertState")

                "The certificationbody party must be ZS" using (outputState.certificationBody.name.commonName == "ZS")
                logger.info("Contract:  CertificationBody ist ZS")

                "The owningParty is a Farmer" using (outputState.owningParty.name.commonName == "Farmer")
                logger.info("Contract:  OwningParty ist ein Farmer")

                "The certificate is not revoked" using !outputState.revoked
                logger.info("Contract:  Das Zertifikat ist nicht widerrufen worden")
            }


            is Commands.Check -> requireThat {
                logger.info("Contract:  Command.check aufgerufen")

                "There must be two input states" using (tx.inputs.size == 2)
                logger.info("Contract:  Es gibt zwei InputStates")
                val inputCertState = tx.inputsOfType<CertState>().single()
                val inputGrapeState = tx.inputsOfType<GrapeState>().single()
                val outputGrapeState = tx.outputsOfType<GrapeState>().single()

                "The invoker is the certificate holder" using (inputCertState.owningParty == outputGrapeState.owner)
                logger.info("Contract:  Party besitzt Zertifikat")

                "The certificate is not revoked" using !inputCertState.revoked
                logger.info("Contract:  Das Zertifkat wurde nicht widerrufen")

                "The certificate should not be expired" using LocalDateTime.now().isBefore(inputCertState.expires)
                logger.info("Contract:  Das Zertifikat ist aktuell")
            }

            is Commands.Revoke -> requireThat {
                logger.info("Contract:  Command.revoke aufgerufen")

                "There must be two input states" using (tx.inputs.size == 1)
                logger.info("Contract:  Es gibt zwei InputStates")
            }

        }
        logger.info("Ende verify\n")
    }

    interface Commands : CommandData {
        class Cert : CommandData, Commands
        class Check : TypeOnlyCommandData(), Commands
        class Revoke : TypeOnlyCommandData(), Commands
    }
}