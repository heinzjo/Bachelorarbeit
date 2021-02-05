package com.template.contracts

import com.google.gson.annotations.SerializedName
import net.corda.core.serialization.CordaSerializable

/**
 * Diese Klasse ist die Implementierung der Contracts
 * die für das Zertifizieren benötigt werden
 * */
@CordaSerializable
data class GrapeStateTMP(
        @SerializedName("Besitzer_Alt") val oldOwner: String,
        @SerializedName("Besitzer_Neu") val newOwner: String,
        @SerializedName("Gewicht") val weight: String,
        @SerializedName("Beschreibung") val desc: String,
        @SerializedName("Erntedatum") val harvest_date: String,
        @SerializedName("Zertifiziert") val certified: String,
        @SerializedName("Sonstiges") val values: Map<String, String>
) {
    constructor(
            oldOwner: String,
            newOwner: String,
            weight: String,
            desc: String,
            harvest_date: String,
            certified: String,
            values: String) :
            this(
                    oldOwner = oldOwner,
                    newOwner = newOwner,
                    weight = weight,
                    desc = desc,
                    harvest_date = harvest_date,
                    certified = certified,
                    values = createMap(values))

    constructor() : this("", "", "", "", "", "", emptyMap())


    fun create(input: String): GrapeStateTMP {
        val short = input.replace("\n", "")
                .replace(" ", "")
                .substringAfter("{")
                .substringBeforeLast("}")

        val replaceComma = short.replace(",\"", "(,)\"")
        val newComma = replaceComma.replace("\"", "")

        val end = newComma.substringAfter("Sonstiges:")

        val map = newComma.split("(,)")
                .map { it.substringBefore(":") to it.substringAfter(":") }
                .toMap() + mapOf("Sonstiges" to end)

        return GrapeStateTMP(
                map["Besitzer_Alt"].toString(),
                map["Besitzer_Neu"].toString(),
                map["Gewicht"].toString(),
                map["Beschreibung"].toString(),
                map["Erntedatum"].toString(),
                map["Zertifiziert"].toString(),
                map["Sonstiges"].toString())
    }

}

private fun createMap(input: String): Map<String, String> {
    val tmp = input.substringAfter("{").substringBefore("}")
    return if (tmp == "" || tmp == "{}") emptyMap()
    else {
        tmp.split("(,)")
                .map { it.substringBefore(":") to it.substringAfter(":") }
                .toMap()

    }
}