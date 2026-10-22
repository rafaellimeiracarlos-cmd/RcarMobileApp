package com.example.rcarpremium

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Locale

/** Apenas NF-e/NFC-e XML de produtos: não valida assinatura, autorização ou situação fiscal em tempo real. */
data class NfeLine(
    val line: Int,
    val description: String,
    val vendorCode: String,
    val gtin: String,
    val quantity: BigDecimal,
    val unit: String,
    val total: BigDecimal,
    val ncm: String,
    val cfop: String
) {
    val unitCost: BigDecimal get() = if (quantity.signum() > 0) total.divide(quantity, 6, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
}

data class NfeInvoice(
    val key: String,
    val hash: String,
    val number: String,
    val supplier: String,
    val supplierCnpj: String,
    val authorizationStatus: String,
    val items: List<NfeLine>
) {
    val importId: String get() = if (key.length == 44) "NFE:$key" else "SHA256:$hash"
}

object NfeImportParser {
    const val MAX_BYTES = 4 * 1024 * 1024
    const val MAX_ITEMS = 400

    fun parse(xml: ByteArray): NfeInvoice {
        require(xml.isNotEmpty() && xml.size <= MAX_BYTES) { "O XML excede o limite de 4 MB ou está vazio." }
        val raw = xml.toString(Charsets.UTF_8)
        require(!raw.contains("<!DOCTYPE", true) && !raw.contains("<!ENTITY", true)) {
            "XML com declaração DTD/ENTITY não é aceito por segurança."
        }
        val sha = MessageDigest.getInstance("SHA-256").digest(xml).joinToString("") { "%02x".format(it) }
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(ByteArrayInputStream(xml), null)
        var documentRoot = ""
        var insideIssuer = false
        var insideDetail = false
        var insideProd = false
        var insideIde = false
        var insideProt = false
        var current = ""
        var key = ""
        var number = ""
        var supplier = ""
        var cnpj = ""
        var status = ""
        var itemIndex = 0
        var itemName = ""
        var vendorCode = ""
        var gtin = ""
        var quantity = ""
        var unit = ""
        var total = ""
        var ncm = ""
        var cfop = ""
        val lines = mutableListOf<NfeLine>()

        fun endItem() {
            val q = quantity.toBigDecimalOrNull()
            val v = total.toBigDecimalOrNull()
            require(itemName.isNotBlank() && q != null && q > BigDecimal.ZERO && v != null && v >= BigDecimal.ZERO) {
                "Item ${itemIndex.coerceAtLeast(1)} da nota incompleto: descrição, quantidade e valor são obrigatórios."
            }
            require(lines.size < MAX_ITEMS) { "A nota possui mais de $MAX_ITEMS itens; divida a importação." }
            lines.add(NfeLine(itemIndex.coerceAtLeast(lines.size + 1), itemName.take(200), vendorCode.take(80),
                gtin.take(40), q, unit.take(12), v, ncm.take(12), cfop.take(8)))
        }

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> error("Declarações DTD não são aceitas.")
                XmlPullParser.START_TAG -> {
                    current = parser.name.substringAfter(':')
                    if (documentRoot.isEmpty()) documentRoot = current
                    when (current) {
                        "infNFe" -> {
                            val rawId = parser.getAttributeValue(null, "Id") ?: parser.getAttributeValue(null, "id").orEmpty()
                            val candidate = rawId.removePrefix("NFe")
                            if (candidate.matches(Regex("[0-9]{44}"))) key = candidate
                        }
                        "emit" -> insideIssuer = true
                        "ide" -> insideIde = true
                        "infProt" -> insideProt = true
                        "det" -> {
                            insideDetail = true
                            itemIndex = parser.getAttributeValue(null, "nItem")?.toIntOrNull() ?: lines.size + 1
                            itemName = ""; vendorCode = ""; gtin = ""; quantity = ""; unit = ""; total = ""; ncm = ""; cfop = ""
                        }
                        "prod" -> if (insideDetail) insideProd = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val value = parser.text?.trim().orEmpty()
                    if (value.isNotBlank()) {
                        when {
                            insideProd -> when (current) {
                                "xProd" -> itemName += value
                                "cProd" -> vendorCode += value
                                "cEAN" -> gtin += value
                                "qCom" -> quantity += value
                                "uCom" -> unit += value
                                "vProd" -> total += value
                                "NCM" -> ncm += value
                                "CFOP" -> cfop += value
                            }
                            insideIssuer -> when (current) {
                                "xNome" -> supplier += value
                                "CNPJ" -> cnpj += value
                            }
                            insideIde && current == "nNF" -> number += value
                            insideProt && current == "cStat" -> status += value
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name.substringAfter(':')) {
                    "prod" -> insideProd = false
                    "det" -> { endItem(); insideDetail = false }
                    "emit" -> insideIssuer = false
                    "ide" -> insideIde = false
                    "infProt" -> insideProt = false
                }
            }
            parser.next()
        }
        require(documentRoot in setOf("nfeProc", "NFe")) { "O arquivo não é um XML de NF-e/NFC-e (nfeProc ou NFe)." }
        require(lines.isNotEmpty()) { "O XML não contém itens de mercadorias (det/prod)." }
        if (status.isNotBlank()) require(status in setOf("100", "150")) {
            "A nota indica cStat=$status. A importação foi bloqueada; verifique a situação fiscal."
        }
        return NfeInvoice(key, sha, number, supplier.ifBlank { "Fornecedor não identificado" }, cnpj, status, lines)
    }
}
