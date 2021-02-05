package com.template.webserver

import com.google.gson.GsonBuilder
import com.template.contracts.GrapeStateTMP
import com.template.flows.*
import com.template.states.CertState
import com.template.states.GrapeState
import com.template.states.RequestState
import com.template.states.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/")
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy: CordaRPCOps? = rpc.proxy //The used RPC Client
    private val nameProxy = proxy!!.nodeInfo().legalIdentities.first().name.organisation //The name of the client

    /**-----------------Get Mapping-----------------*/

    /**
    * Methode um die anderen Netzwerkteilnehmer zu erhalten
    * */
    @GetMapping(value = ["/peers"], produces = [APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        logger.info("/peers")
        val nodeInfo = proxy!!.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                .filter { it.organisation !in (proxy.nodeInfo().legalIdentities.first().name.organisation) })
    }

    /**
    * Methode um alle GrapeStates zu erhalten
    * */
    @GetMapping(value = ["/grapestates"], produces = [APPLICATION_JSON_VALUE])
    fun getGrapes(): Map<String, List<Map<String, Any>>> {
        logger.info("/grapestates")

        val criteriaUnconsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val stateInfoUnconsumed = proxy!!.vaultQueryBy<GrapeState>(criteriaUnconsumed).states.toMutableList()

        val criteriaConsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)
        val stateInfoConsumed = proxy.vaultQueryBy<GrapeState>(criteriaConsumed).states.toMutableList()

        var tmpList = listOf<Map<String, Any>>()

        for (x in stateInfoConsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(true))
        }

        for (x in stateInfoUnconsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(false))
        }

        return mapOf("grapestates" to tmpList)
    }

    /**
    * Methode um alle CertStates zu erhalten
    * */
    @GetMapping(value = ["/certstates"], produces = [APPLICATION_JSON_VALUE])
    fun getCerts(): Map<String, List<Map<String, Any>>> {
        logger.info("certstates")

        val criteriaUnconsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val stateInfoUnconsumed = proxy!!.vaultQueryBy<CertState>(criteriaUnconsumed).states.toMutableList()

        val criteriaConsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)
        val stateInfoConsumed = proxy.vaultQueryBy<CertState>(criteriaConsumed).states.toMutableList()

        var tmpList = listOf<Map<String, Any>>()

        for (x in stateInfoConsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(true))
        }

        for (x in stateInfoUnconsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(false))
        }

        return mapOf("certstates" to tmpList)
    }

    /**
    * Methode um alle RequestStates in der Vault zu erhalten
    * */
    @GetMapping(value = ["/reqstates"], produces = [APPLICATION_JSON_VALUE])
    fun getReqs(): Map<String, List<Map<String, Any>>> {
        logger.info("reqstates")

        val criteriaUnconsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val stateInfoUnconsumed = proxy!!.vaultQueryBy<RequestState>(criteriaUnconsumed).states.toMutableList()

        val criteriaConsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)
        val stateInfoConsumed = proxy.vaultQueryBy<RequestState>(criteriaConsumed).states.toMutableList()

        var tmpList = listOf<Map<String, Any>>()

        for (x in stateInfoConsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(true))
        }

        for (x in stateInfoUnconsumed) {
            tmpList = tmpList.plus(x.state.data.toJSON(false))
        }

        return mapOf("req" to tmpList)
    }

    /**
    * Methode um eigene Identität zu erhalten
    * */
    @GetMapping(value = ["/me"], produces = [APPLICATION_JSON_VALUE])
    private fun getMe(): Map<String, CordaX500Name> {
        logger.info("/me")
        return mapOf("me" to proxy!!.nodeInfo().legalIdentities.first().name)
    }

    /**
    * Methode um alle TransactionStates zu erhalten
    * */
    @GetMapping(value = ["/gettx"], produces = [APPLICATION_JSON_VALUE])
    private fun getTx(): Map<String, List<Map<String, Any>>> {
        logger.info("gettx")

        try {
            proxy!!.startTrackedFlow(::TraceFlow).returnValue.getOrThrow()
        } catch (ex: net.corda.core.CordaRuntimeException) {
            logger.info("Keine neuen Transaktionen vorhanden")
        } catch (ex: Exception) {
            logger.info(ex.message)
        }

        val criteriaUnconsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val stateInfoUnconsumed = proxy!!.vaultQueryBy<TransactionState>(criteriaUnconsumed).states

        val criteriaConsumed = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)
        val stateInfoConsumed = proxy.vaultQueryBy<TransactionState>(criteriaConsumed).states

        var tmpList = listOf<Map<String, Any>>()

        for (x in stateInfoConsumed) {
            tmpList = tmpList.plus(x.state.data.toJson(true))
        }

        for (x in stateInfoUnconsumed) {
            tmpList = tmpList.plus(x.state.data.toJson(false))
        }

        return mapOf("tx" to tmpList)
    }

    /**
     * Der Downloadlink für eine jar/zip Datei
     *
     * @return Eine ResponseEntity die einen Download startet
     */
    @GetMapping(value = ["/dl/{hash}"])
    private fun downloadJar(@PathVariable(value = "hash", required = true) hash: String): ResponseEntity<Resource> {
        logger.info("downloadJar $hash")

        val inputStream = InputStreamResource(proxy!!.openAttachment(SecureHash.parse(hash)))
        val filename = "contract.zip"

        return try {
            ResponseEntity.ok().header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"$filename\""
            ).body(inputStream)
        } catch (ex: Exception) {
            logger.info(ex.message)
            ResponseEntity.badRequest().body(inputStream)
        }
    }


    /**-----------------Post Mapping-----------------*/
    /**
    * Startet den ProduceFlow
    * */
    @PostMapping(value = ["produceGrape"], produces = [APPLICATION_JSON_VALUE])
    private fun produceGrapes(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Methodenaufruf produceGrape")
        val weight = request.getParameter("weight").toInt()
        logger.info("weight request $weight")
        val number = request.getParameter("number").toInt()
        logger.info("number request $number")
        val description = request.getParameter("desc")
        logger.info("description request: $description")
        val values = if (request.getParameter("values") == null) {
            ""
        } else {
            request.getParameter("values")
        }
        logger.info("values request: $values")
        return try {
            val signedTx = proxy!!.startTrackedFlow(::ProduceFlow, weight, number, description, values).returnValue.getOrThrow()
            val msgSuccess = if (values == "") {
                "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nGewicht: $weight kg\r\nAnzahl: $number"
            } else {
                "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nGewicht: $weight kg\r\nAnzahl: $number\r\nSonstiges: ${values.replace("(,)",",")}"
            }
            val tmpMap = getTxValues(signedTx, msgSuccess, "ProduceFlow")
            ResponseEntity.status(HttpStatus.CREATED).body(tmpMap)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(mapOf("message" to "Fehler: $ex.message"))
        }
    }

    /**
     * Startet den TradeFlow und erzeugt ein Dokument, welches in die Vault hochgeladen wird
     * */
    @PostMapping(value = ["tradeGrape"], produces = [APPLICATION_JSON_VALUE])
    private fun tradeGrapes(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Methodenaufruf tradeGrape")

        val newOwner = request.getParameter("newOwner")
        logger.info("newOwner request $newOwner")

        val grapeId = request.getParameter("grapeId")
        logger.info("grapeId request $grapeId")

        val otherParty = proxy!!.partiesFromName(newOwner, false)
        logger.info(otherParty.single().toString())

        val queryGrape = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(grapeId)))

        val grapeState = proxy.vaultQueryBy<GrapeState>(queryGrape).states.single().state.data

        val gsonBuilder = GsonBuilder().setPrettyPrinting().create()
        val tmpGrape = GrapeStateTMP(
                nameProxy,
                newOwner,
                grapeState.weight,
                grapeState.description,
                grapeState.harvest_date.toString(),
                grapeState.certified.toString(),
                grapeState.values
        )

        val fileToWrite = gsonBuilder.toJson(tmpGrape)
        val fileName = "${nameProxy}_$newOwner"

        return try {
            //Dateipfade statisch
            val file = File(
                    "D:\\Uni\\Hiwi\\CordaBsp\\cordapp-template-kotlin\\clients\\build\\libs\\tmp\\$fileName.JSON")
            FileWriter(file).use { writer -> writer.write(fileToWrite) }

            val fis = FileInputStream(File(
                    "D:\\Uni\\Hiwi\\CordaBsp\\cordapp-template-kotlin\\clients\\build\\libs\\tmp\\$fileName.JSON"))

            val zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(
                    "D:\\Uni\\Hiwi\\CordaBsp\\cordapp-template-kotlin\\clients\\build\\libs\\tmp\\$fileName.zip")))

            zos.use { output ->
                fis.use { input ->
                    BufferedInputStream(input).use { origin ->
                        val entry = ZipEntry("$fileName.JSON")
                        output.putNextEntry(entry)
                        origin.copyTo(output, 1024)
                    }
                }
            }

            val attachmentID = proxy.uploadAttachmentWithMetadata(
                    jar = FileInputStream(File(
                            "D:\\Uni\\Hiwi\\CordaBsp\\cordapp-template-kotlin\\clients\\build\\libs\\tmp\\$fileName.zip")),
                    filename = "$fileName.zip",
                    uploader = nameProxy)

            val signedTx = proxy.startTrackedFlow(::TradeFlow, otherParty.single(), grapeId, attachmentID).returnValue.getOrThrow()
            val msgSuccess = "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nNeuer Besitzer: $newOwner\r\nTraubenID: $grapeId"

            val tmpMap = getTxValues(signedTx, msgSuccess, "TradeFlow")

            ResponseEntity.status(HttpStatus.CREATED).body(tmpMap)

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(mapOf("message" to "Fehler: $ex.message"))
        }
    }

    /**
     * Startet den CertFlow
     * */
    @PostMapping(value = ["createCert"], produces = [APPLICATION_JSON_VALUE])
    private fun createCert(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Methodenaufruf createCert")

        val owningParty = request.getParameter("owningParty").substringAfter("O=").substringBefore(",")
        logger.info("owningParty request $owningParty")
        val otherParty = proxy!!.partiesFromName(owningParty, false)
        logger.info(otherParty.single().toString())

        val description = request.getParameter("description")
        logger.info("description request $description")

        val period = request.getParameter("period").toLong()
        logger.info("period request $period")

        val requestId = request.getParameter("requestId")
        logger.info("requestId request $requestId")

        val number = request.getParameter("number").toInt()
        logger.info("number request $number")

        return try {
            val signedTx = proxy.startTrackedFlow(::CertFlow, description, otherParty.single(), period, requestId, number, requestId).returnValue.getOrThrow()
            val msgSuccess = "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nBesitzer: $owningParty \r\nBeschreibung: $description\nLäuft in $period Tagen ab.\n Anfrage: $requestId\nAnzahl: $number"

            val tmpMap = getTxValues(signedTx, msgSuccess, "CertFlow")

            ResponseEntity.status(HttpStatus.CREATED)
                    .body(tmpMap)

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(mapOf("message" to "Fehler: $ex.message"))
        }
    }

    /**
     * Startet den RequestFlow
     * */
    @PostMapping(value = ["createReq"], produces = [APPLICATION_JSON_VALUE])
    private fun createRequest(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Methodenaufruf createCert")

        val description = request.getParameter("description")
        logger.info("description request $description")

        val certificationBody = request.getParameter("certificationBody")
        logger.info("owningParty request $certificationBody")
        val otherParty = proxy!!.partiesFromName(certificationBody, false)
        logger.info(otherParty.single().toString())

        val number = request.getParameter("number").toInt()
        logger.info("number request $number")

        return try {
            val signedTx = proxy.startTrackedFlow(::RequestFlow, description, otherParty.single(), number).returnValue.getOrThrow()
            val msgSuccess = "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nZertifizierungsstelle: $certificationBody \r\nBeschreibung: $description\nAnzahl: $number"

            val tmpMap = getTxValues(signedTx, msgSuccess, "RequestFlow")

            ResponseEntity.status(HttpStatus.CREATED)
                    .body(tmpMap)

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(mapOf("message" to "Fehler: $ex.message"))
        }
    }

    /**
     * Startet den CertGrapeFlow
     * */
    @PostMapping(value = ["certifyGrape"], produces = [APPLICATION_JSON_VALUE])
    private fun certifyGrape(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Methodenaufruf certifyGrape")

        val grapeId = request.getParameter("grapeId")
        logger.info("grapeId request $grapeId")

        val certId = request.getParameter("certId")
        logger.info("certId request $certId")

        val description = request.getParameter("description")!!.toBoolean()
        logger.info("description request $description")

        return try {
            val signedTx = proxy!!.startTrackedFlow(::CertGrapeFlow, grapeId, certId, description).returnValue.getOrThrow()
            val msgSuccess = "Transaktion wurde erfolgreich ausgeführt.\r\nID: ${signedTx.id}\r\nTraubenID: $grapeId \r\nZertifikatID: $certId\n"

            val tmpMap = getTxValues(signedTx, msgSuccess, "CertGrapeFlow")

            ResponseEntity.status(HttpStatus.CREATED)
                    .body(tmpMap)

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(mapOf("message" to "Fehler: $ex.message"))
        }
    }

    /**
     * Startet den TraceFlow
     * */
    @PostMapping(value = ["traceTx"], produces = [APPLICATION_JSON_VALUE])
    private fun tracetx(request: HttpServletRequest): Map<String, Any?> {
        logger.info("Methodenaufruf tracetx")

        return try {
            val signedTx = proxy!!.startTrackedFlow(::TraceFlow).returnValue.getOrThrow()
            val msgSuccess = "TX wurden gesammelt."

            getTxValues(signedTx, msgSuccess, "TraceFlow")

        } catch (ex: Throwable) {
            mapOf("error" to "error")
        }
    }

    /**-----------------Other-----------------*/

    /**
     * Methode stellt die Transaktionsparameter in einer Map dar
     * */
    private fun getTxValues(tx: SignedTransaction, msgSuccess: String, flowName: String): Map<String, String> {
        var inputs = "Keine"
        if (tx.coreTransaction.inputs.isNotEmpty()) {
            inputs = tx.coreTransaction.inputs.toString()
        }
        val outputs = tx.coreTransaction.outputStates.toString()
        val notary = tx.coreTransaction.notary.toString()
        val id = tx.coreTransaction.id.toString()
        val commands = tx.tx.commands.map { it.toString().substringAfter("contracts.").substringBefore(" with") }.toString()

        return mapOf(
                "id" to id,
                "flow" to flowName,
                "inputs" to inputs,
                "outputs" to outputs,
                "notary" to notary,
                "message" to msgSuccess,
                "commands" to commands)
    }

    /**
     * Löscht die eventuell lokal erstellte Datei, beim Nutzen des TradeFlows
     * */
    @PostConstruct
    private fun deleteFiles() {
        //Die Dateipfade sind statisch
        val file = File("D:\\Uni\\Hiwi\\CordaBsp\\cordapp-template-kotlin\\clients\\build\\libs\\tmp")

        logger.info("Datei gelöscht: ${file.deleteRecursively()}")
        logger.info("Dir erstellt: ${file.mkdir()}")

    }
}