package com.template.contracts

import com.template.states.GrapeState
import net.corda.core.contracts.*
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Diese Klasse ist die Implementierung der Contracts,
 * die für das Erstellen und Handeln einer Traubenkiste
 * notwendig sind
 * */
class GrapeContract : Contract {

    companion object {
        private val logger = LoggerFactory.getLogger(GrapeContract::class.java)
        val GRAPE_CONTRACT_ID = GrapeContract::class.qualifiedName!!
    }

    override fun verify(tx: LedgerTransaction) {

        logger.info("GrapeContract.verify aufgerufen")
        val command = tx.commands.requireSingleCommand<Commands>().value
        when (command) {

            is Commands.Produce -> requireThat {
                logger.info("Contract:  Commands.Create aufgerufen")

                "There should be no input state" using (tx.inputs.isEmpty())
                logger.info("Contract:  Tx hat keine InputStates")

                "The output state must be of type GrapeState" using (tx.outputs[0].data is GrapeState)
                val outputState = tx.outputs[0].data as GrapeState
                logger.info("Contract:  Der OutputState ist GrapeState")

                "The amount should be higher than zero" using (outputState.weight.toInt() > 0)
                logger.info("Contract:  Das Gewicht ist größer als 0")

                "The Creator should be a Farmer" using (outputState.owner.name.commonName == "Farmer")
                logger.info("Contract:  Creator ist ein Farmer")

            }

            is Commands.Check -> requireThat {
                logger.info("Contract:  Commands.Check aufgerufen")
                "There must only be one input state" using (tx.inputs.size == 2)
                logger.info("Contract:  Es gibt einen InputState")

                "The Input State must be of type GrapeState" using (tx.inputStates[0] is GrapeState)
                logger.info("Contract:  Der InputState ist vom Typ GrapeState")
            }

            is Commands.Trade -> requireThat {
                logger.info("Contract:  Commands.Check aufgerufen")

                val inputState = tx.inputStates.single() as GrapeState
                val outputState = tx.outputs.single().data as GrapeState

                "There must only be one input state" using (tx.inputs.size == 1)
                logger.info("Contract:  Es gibt einen InputState")

                "There must only be one input state" using (tx.outputs.size == 1)
                logger.info("Contract:  Es gibt einen InputState")

                "The Input State must be of type GrapeState" using
                        (tx.inputStates[0] is GrapeState)
                logger.info("Contract:  Der InputState ist vom Typ GrapeState")

                "The Output State must be of type GrapeState" using (tx.outputs[0].data is GrapeState)
                logger.info("Contract:  Der OutputState ist vom Typ GrapeState")

                "The Invoker should be a Farmer or a Trader" using (inputState.owner.name.commonName == "Farmer" || inputState.owner.name.commonName == "Trader")
                logger.info("Contract:  Invoker ist ein Farmer oder ein Händler")

                "The Recipient should be a Trader or the original Farmer" using (outputState.owner.name.commonName == "Trader" || outputState.owner == outputState.manufacturer)
                logger.info("Contract:  Recipient ist ein Händler")

                require(verifyAttachment(tx)) {
                    "The inputs must be contained within the attachment"
                }
            }

        }
        logger.info("Ende verify\n")
    }

    private fun verifyAttachment(tx: LedgerTransaction): Boolean {
        val attID = tx.commandsOfType<CommandWithAttachmentId>().single().value.attachmentId

        val att = tx.getAttachment(attID).openAsJAR()

        val output = tx.outputs[0].data as GrapeState

        val reader = StringBuilder()
        att.use {
            while (it.nextJarEntry != null) {
                reader.append(it.bufferedReader(Charsets.UTF_8).readText())
            }
        }

        logger.info(reader.toString())
        val grapeStateJSON = GrapeStateTMP().create(reader.toString())
        logger.info(grapeStateJSON.toString())

        var result = true

        if (grapeStateJSON.oldOwner != output.manufacturer.name.organisation) {
            result = false
            logger.info("oldOwner not matching")
        } else {
            logger.info("JSONObjekt: Alter Besitzer stimmt überein, ${output.manufacturer.name.organisation}")
        }

        if (grapeStateJSON.weight != output.weight) {
            result = false
            logger.info("weight not matching")
        } else {
            logger.info("JSONObjekt: Gewicht stimmt überein, ${output.weight}")
        }

        if (LocalDateTime.parse(grapeStateJSON.harvest_date) != output.harvest_date) {
            result = false
            logger.info("harvest_date not matching")
        } else {
            logger.info("JSONObjekt: Erntedatum stimmt überein, ${output.harvest_date}")
        }

        if (grapeStateJSON.certified.toBoolean() != output.certified) {
            result = false
            logger.info("certified not matching")
        } else {
            logger.info("JSONObjekt: Zertifiziert stimmt überein, ${output.certified}")
        }

        logger.info("verifyAttachment: $result")
        logger.info("Extravalues : ${grapeStateJSON.values}")
        val otherResult = compareMaps(grapeStateJSON.values, output.values)
        logger.info("compareMaps: $otherResult")
        return result && otherResult
    }

    private fun compareMaps(first: Map<String, String>, second: Map<String, String>): Boolean {

        logger.info("ersteMap: $first")
        logger.info("zweiteMap: $second")
        if (first.size != second.size) return false

        var firstValues = ""
        var secondValues = ""

        first.forEach {
            firstValues += it.value + ";"
        }

        second.forEach {
            secondValues += it.value + ";"
        }

        logger.info("Erster String: $firstValues")
        logger.info("Zweiter String: $secondValues")

        return firstValues == secondValues
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Produce : CommandData, Commands
        class Check : TypeOnlyCommandData(), Commands
        class Trade(attachmentId: AttachmentId) : CommandWithAttachmentId(attachmentId), Commands

    }

    abstract class CommandWithAttachmentId(val attachmentId: AttachmentId) : CommandData {
        val attachmentID = this.attachmentId
        override fun equals(other: Any?) = other?.javaClass == javaClass
        override fun hashCode() = javaClass.name.hashCode()
    }
}