package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Plano confirmado pelo usuário; linha não marcada nunca modifica o catálogo. */
data class NfeChoice(
    val item: NfeLine,
    val selected: Boolean,
    val productName: String,
    val salePrice: Double,
    val quantity: Int,
    val category: String,
    val existingOriginalName: String? = null
)

data class NfeImportOutcome(val created: Int, val updated: Int, val receivedUnits: Int)

object NfeImportStore {
    private const val PREF = "rcar_admin"
    private const val KEY_LEDGER = "nfe_import_ledger_v1"
    private fun ledger(context: Context) = JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LEDGER, "[]") ?: "[]")

    fun alreadyImported(context: Context, invoice: NfeInvoice): Boolean {
        val entries = ledger(context)
        return (0 until entries.length()).any { i ->
            val entry = entries.getJSONObject(i)
            entry.optString("id") == invoice.importId || entry.optString("hash") == invoice.hash
        }
    }

    fun history(context: Context): List<String> {
        val array = ledger(context)
        return (0 until array.length()).mapNotNull { i ->
            val row = array.optJSONObject(i) ?: return@mapNotNull null
            val number = row.optString("number").ifBlank { "s/n" }
            "NF $number • ${row.optString("supplier")} • ${row.optInt("units")} unidades • ${row.optInt("lines")} linhas"
        }.reversed()
    }

    /** Compara EAN, depois SKU composto com emissor, depois nome; não sobrescreve preço de venda de item antigo. */
    fun findMatch(context: Context, invoice: NfeInvoice, line: NfeLine): AdminProduto? {
        val catalog = AdminLocalStore.products(context)
        return findIn(catalog, invoice, line)
    }

    private fun findIn(catalog: List<AdminProduto>, invoice: NfeInvoice, line: NfeLine): AdminProduto? {
        val ean = line.gtin.filter { it.isDigit() }.takeIf { it.length in 8..14 && it.any { c -> c != '0' } }
        val sku = suggestedSku(invoice, line)
        return catalog.firstOrNull { ean != null && it.codigoBarras == ean }
            ?: catalog.firstOrNull { it.sku.isNotBlank() && it.sku.equals(sku, true) }
            ?: catalog.firstOrNull { it.nome.equals(line.description, true) }
    }

    fun suggestedSku(invoice: NfeInvoice, line: NfeLine): String {
        val supplierPart = invoice.supplierCnpj.filter(Char::isDigit).takeLast(8).ifBlank { "FORNEC" }
        val productPart = line.vendorCode.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9-]"), "-").take(24)
            .ifBlank { "LINHA-${line.line}" }
        return "NF-$supplierPart-$productPart".take(48)
    }

    @Synchronized
    fun apply(context: Context, invoice: NfeInvoice, choices: List<NfeChoice>, actorEmail: String): NfeImportOutcome {
        val repo = UsuarioLocalRepository(context)
        require(repo.usuarioAtivo(actorEmail) && repo.isCeo(actorEmail)) {
            "A importação direta está restrita ao CEO. Use a conferência supervisionada."
        }
        require(!alreadyImported(context, invoice)) { "Esta nota já foi importada. Confira o histórico para evitar duplicações." }
        val chosen = choices.filter { it.selected }
        require(chosen.isNotEmpty()) { "Selecione pelo menos um produto da nota." }
        val updatedCatalog = AdminLocalStore.products(context)
        var created = 0
        var updated = 0
        var units = 0
        for (choice in chosen) {
            val qty = choice.quantity
            require(qty > 0 && qty <= 1000000) { "Quantidade inválida para ${choice.item.description}." }
            require(choice.productName.isNotBlank()) { "Informe o nome do produto na linha ${choice.item.line}." }
            require(choice.salePrice.isFinite() && choice.salePrice > 0 && choice.salePrice < 1e9) {
                "Preço de venda inválido na linha ${choice.item.line}."
            }
            require(choice.category.isNotBlank()) { "Informe a categoria da linha ${choice.item.line}." }
            val calculatedCost = choice.item.unitCost.setScale(2, RoundingMode.HALF_UP).toDouble()
            require(calculatedCost.isFinite() && calculatedCost >= 0 && calculatedCost < 1e9) { "Custo inválido." }
            val target = choice.existingOriginalName?.let { name -> updatedCatalog.indexOfFirst { it.nome.equals(name, true) } } ?: -1
            if (choice.existingOriginalName != null) require(target >= 0) { "Produto associado não foi encontrado: ${choice.existingOriginalName}." }
            if (target >= 0) {
                val original = updatedCatalog[target]
                require(original.estoque <= Int.MAX_VALUE - qty) { "O estoque de ${original.nome} excederia o limite." }
                // Último custo de compra; preserve o preço de venda e a propriedade do anúncio existentes.
                updatedCatalog[target] = original.copy(estoque = original.estoque + qty, precoCusto = calculatedCost)
                updated++
            } else {
                require(updatedCatalog.none { it.nome.equals(choice.productName, true) }) {
                    "Já existe '${choice.productName}'. Vincule ao produto existente em vez de duplicar."
                }
                val sku = suggestedSku(invoice, choice.item)
                require(updatedCatalog.none { it.sku.equals(sku, true) }) {
                    "SKU $sku já existe. Vincule a linha ao produto existente."
                }
                updatedCatalog.add(AdminProduto(
                    nome = choice.productName.trim(), categoria = choice.category.trim(), preco = choice.salePrice,
                    estoque = qty, sku = sku, codigoBarras = choice.item.gtin.takeIf { ean -> ean.all(Char::isDigit) && ean.length in 8..14 && ean.any { it != '0' } }.orEmpty(),
                    descricao = "Importado da NF-e ${invoice.number}. NCM ${choice.item.ncm} • CFOP ${choice.item.cfop}",
                    precoCusto = calculatedCost, fornecedor = invoice.supplier,
                    cadastradoPor = actorEmail.trim().lowercase(Locale.ROOT),
                    // Produtos novos não são publicados automaticamente; liberar após revisão do CEO.
                    anuncioAtivo = false
                ))
                created++
            }
            units += qty
        }
        val log = ledger(context)
        log.put(JSONObject().apply {
            put("id", invoice.importId); put("hash", invoice.hash)
            put("number", invoice.number); put("supplier", invoice.supplier)
            put("lines", chosen.size); put("units", units)
            put("actor", actorEmail.trim().lowercase(Locale.ROOT))
            put("timestamp", System.currentTimeMillis())
        })
        require(AdminLocalStore.saveInvoiceProductsAtomically(context, updatedCatalog, log.toString())) {
            "Não foi possível gravar o estoque. Nenhuma importação foi confirmada."
        }
        return NfeImportOutcome(created, updated, units)
    }
}
