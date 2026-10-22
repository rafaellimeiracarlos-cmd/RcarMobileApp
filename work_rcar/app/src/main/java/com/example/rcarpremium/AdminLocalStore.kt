package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Produto persistido no armazenamento administrativo local. */
data class AdminProduto(
    val nome: String,
    val categoria: String,
    val preco: Double,
    var estoque: Int,
    val sku: String = "",
    val marca: String = "",
    val codigoBarras: String = "",
    val descricao: String = "",
    val precoCusto: Double = 0.0,
    val estoqueMinimo: Int = 5,
    val fornecedor: String = "",
    val localizacao: String = "",
    val ativo: Boolean = true,
    /** E-mail do funcionário/CEO que cadastrou o produto. */
    val cadastradoPor: String = "",
    /** Estado do anúncio no catálogo; independente da existência física do produto. */
    val anuncioAtivo: Boolean = true
)

data class AdminVenda(val total: Double, val timestamp: Long)
data class AdminVendaProduto(
    val nomeProduto: String,
    val quantidade: Int,
    val precoUnitario: Double,
    val custoUnitario: Double,
    val timestamp: Long,
    val responsavel: String
)
data class AdminResumoDiario(val label: String, val total: Double, val count: Int)

/** Persistência local para protótipo offline; os dados não são sincronizados entre dispositivos. */
object AdminLocalStore {
    private const val PREFS_NAME = "rcar_admin"
    private const val KEY_PRODUCTS = "products"
    private const val KEY_SALES = "sales"
    private const val KEY_SALE_LOG = "sale_log"
    private const val KEY_PRODUCT_SALES = "product_sale_log"

    fun products(context: Context): MutableList<AdminProduto> {
        val raw = preferences(context).getString(KEY_PRODUCTS, null)
        if (raw.isNullOrBlank()) return catalogoInicial()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                AdminProduto(
                    nome = item.getString("nome"),
                    categoria = item.optString("categoria", "Automotivo"),
                    preco = item.getDouble("preco"),
                    estoque = item.getInt("estoque").coerceAtLeast(0),
                    sku = item.optString("sku", ""),
                    marca = item.optString("marca", ""),
                    codigoBarras = item.optString("codigoBarras", ""),
                    descricao = item.optString("descricao", ""),
                    precoCusto = item.optDouble("precoCusto", 0.0).coerceAtLeast(0.0),
                    estoqueMinimo = item.optInt("estoqueMinimo", 5).coerceAtLeast(0),
                    fornecedor = item.optString("fornecedor", ""),
                    localizacao = item.optString("localizacao", ""),
                    ativo = item.optBoolean("ativo", true),
                    cadastradoPor = item.optString("cadastradoPor", ""),
                    anuncioAtivo = item.optBoolean("anuncioAtivo", true)
                )
            }
        } catch (_: Exception) {
            catalogoInicial()
        }
    }

    fun saveProducts(context: Context, products: List<AdminProduto>) {
        preferences(context).edit().putString(KEY_PRODUCTS, encodeProducts(products)).apply()
    }

    /** Persistência conjunta: produtos e registro de importação na mesma operação SharedPreferences. */
    fun saveInvoiceProductsAtomically(context: Context, products: List<AdminProduto>, ledgerJson: String): Boolean {
        return preferences(context).edit()
            .putString(KEY_PRODUCTS, encodeProducts(products))
            .putString("nfe_import_ledger_v1", ledgerJson)
            .commit()
    }

    /** Grava catálogo, conferência e ledger da NF-e na mesma transação local. */
    fun saveConferenceApprovalAtomically(context: Context, products: List<AdminProduto>, conferencesJson: String, ledgerJson: String): Boolean {
        return preferences(context).edit()
            .putString(KEY_PRODUCTS, encodeProducts(products))
            .putString("inventory_conferences_v1", conferencesJson)
            .putString("nfe_import_ledger_v1", ledgerJson)
            .commit()
    }

    private fun encodeProducts(products: List<AdminProduto>): String {
        val array = JSONArray()
        products.forEach { product ->
            array.put(JSONObject().apply {
                put("nome", product.nome.trim())
                put("categoria", product.categoria.trim())
                put("preco", product.preco)
                put("estoque", product.estoque.coerceAtLeast(0))
                put("sku", product.sku.trim())
                put("marca", product.marca.trim())
                put("codigoBarras", product.codigoBarras.trim())
                put("descricao", product.descricao.trim())
                put("precoCusto", product.precoCusto.coerceAtLeast(0.0))
                put("estoqueMinimo", product.estoqueMinimo.coerceAtLeast(0))
                put("fornecedor", product.fornecedor.trim())
                put("localizacao", product.localizacao.trim())
                put("ativo", product.ativo)
                put("cadastradoPor", product.cadastradoPor.trim().lowercase())
                put("anuncioAtivo", product.anuncioAtivo)
            })
        }
        return array.toString()
    }

    fun addProduct(context: Context, product: AdminProduto) {
        require(product.nome.isNotBlank()) { "O nome do produto é obrigatório." }
        require(product.categoria.isNotBlank()) { "A categoria é obrigatória." }
        require(product.preco.isFinite() && product.preco > 0.0) { "O preço de venda deve ser maior que zero." }
        require(product.precoCusto.isFinite() && product.precoCusto >= 0.0) { "O preço de custo não é válido." }
        require(product.estoque >= 0) { "O estoque não pode ser negativo." }
        require(product.estoqueMinimo >= 0) { "O estoque mínimo não pode ser negativo." }
        val current = products(context)
        require(current.none { it.nome.equals(product.nome.trim(), ignoreCase = true) }) {
            "Já existe um produto com esse nome."
        }
        if (product.sku.isNotBlank()) {
            require(current.none { it.sku.isNotBlank() && it.sku.equals(product.sku.trim(), ignoreCase = true) }) {
                "Já existe um produto com esse SKU. Gere outro código."
            }
        }
        if (product.codigoBarras.isNotBlank()) {
            require(current.none { it.codigoBarras.isNotBlank() && it.codigoBarras == product.codigoBarras.trim() }) {
                "Já existe um produto com esse código de barras."
            }
        }
        current.add(product.copy(
            nome = product.nome.trim(),
            categoria = product.categoria.trim(),
            sku = product.sku.trim(),
            marca = product.marca.trim(),
            codigoBarras = product.codigoBarras.trim(),
            descricao = product.descricao.trim(),
            fornecedor = product.fornecedor.trim(),
            localizacao = product.localizacao.trim()
        ))
        saveProducts(context, current)
    }


    fun updateProduct(context: Context, originalName: String, product: AdminProduto) {
        require(product.nome.isNotBlank()) { "O nome do produto é obrigatório." }
        require(product.categoria.isNotBlank()) { "A categoria é obrigatória." }
        require(product.preco.isFinite() && product.preco > 0.0) { "O preço de venda deve ser maior que zero." }
        require(product.estoque >= 0) { "O estoque não pode ser negativo." }
        val current = products(context)
        require(current.none { it.nome.equals(product.nome.trim(), true) && !it.nome.equals(originalName, true) }) {
            "Já existe outro produto com esse nome."
        }
        val index = current.indexOfFirst { it.nome.equals(originalName, true) }
        require(index >= 0) { "Produto não encontrado para edição." }
        current[index] = product.copy(nome = product.nome.trim(), categoria = product.categoria.trim())
        saveProducts(context, current)
    }

    /** Ativa/desativa somente a divulgação do item, sem retirar o produto do estoque. */
    fun setAnuncioAtivo(context: Context, productName: String, ativo: Boolean): Boolean {
        val list = products(context)
        val index = list.indexOfFirst { it.nome.equals(productName, true) }
        if (index < 0) return false
        list[index] = list[index].copy(anuncioAtivo = ativo)
        saveProducts(context, list)
        return true
    }

    /** Atribui ou transfere a responsabilidade de um produto; chamada apenas pela tela restrita ao CEO. */
    fun definirResponsavel(context: Context, productName: String, emailResponsavel: String): Boolean {
        val list = products(context)
        val index = list.indexOfFirst { it.nome.equals(productName, true) }
        if (index < 0) return false
        list[index] = list[index].copy(cadastradoPor = emailResponsavel.trim().lowercase())
        saveProducts(context, list)
        return true
    }

    /** Registra uma venda balcão feita por funcionário, baixa estoque e registra faturamento e custo. */
    fun registrarVendaProduto(context: Context, productName: String, quantity: Int, responsavel: String): String? {
        if (CompraTesteCeoStore.ativo(context)) return "Modo de teste: venda real bloqueada."
        if (quantity <= 0) return "Informe uma quantidade maior que zero."
        val list = products(context)
        val item = list.firstOrNull { it.nome.equals(productName, true) }
            ?: return "Produto não encontrado no estoque."
        if (!item.ativo) return "Este produto está desativado."
        if (item.estoque < quantity) return "Estoque insuficiente. Disponível: ${item.estoque}."
        item.estoque -= quantity
        saveProducts(context, list)
        val total = item.preco * quantity
        registerSale(context, total)
        appendProductSales(context, listOf(AdminVendaProduto(item.nome, quantity, item.preco, item.precoCusto, System.currentTimeMillis(), responsavel)))
        return null
    }

    /** Linhas de venda permitem calcular margem bruta quando preço e custo são conhecidos. */
    fun registrarItensVenda(context: Context, items: List<ProdutoItem>, responsavel: String = RcarLocalStore.currentEmail(context).orEmpty()) {
        val products = products(context)
        val now = System.currentTimeMillis()
        val lines = items.mapNotNull { sold ->
            val product = products.firstOrNull { it.nome.equals(sold.nome, true) } ?: return@mapNotNull null
            if (sold.quantidade <= 0) return@mapNotNull null
            AdminVendaProduto(product.nome, sold.quantidade, sold.preco, product.precoCusto, now, responsavel)
        }
        appendProductSales(context, lines)
    }

    fun vendasDetalhadas(context: Context, limit: Int = 200): List<AdminVendaProduto> {
        val arr = try { JSONArray(preferences(context).getString(KEY_PRODUCT_SALES, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            AdminVendaProduto(
                nomeProduto = o.optString("nomeProduto", "Produto"),
                quantidade = o.optInt("quantidade", 0).coerceAtLeast(0),
                precoUnitario = o.optDouble("precoUnitario", 0.0).coerceAtLeast(0.0),
                custoUnitario = o.optDouble("custoUnitario", 0.0).coerceAtLeast(0.0),
                timestamp = o.optLong("timestamp", 0L),
                responsavel = o.optString("responsavel", "")
            )
        }.sortedByDescending { it.timestamp }.take(limit.coerceAtLeast(0))
    }

    private fun appendProductSales(context: Context, lines: List<AdminVendaProduto>) {
        if (lines.isEmpty()) return
        val prefs = preferences(context)
        val arr = try { JSONArray(prefs.getString(KEY_PRODUCT_SALES, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        lines.forEach { line -> arr.put(JSONObject().apply {
            put("nomeProduto", line.nomeProduto)
            put("quantidade", line.quantidade)
            put("precoUnitario", line.precoUnitario)
            put("custoUnitario", line.custoUnitario)
            put("timestamp", line.timestamp)
            put("responsavel", line.responsavel)
        }) }
        // Limita o histórico para evitar crescimento indefinido do armazenamento local.
        val trimmed = JSONArray()
        val start = (arr.length() - 5000).coerceAtLeast(0)
        for (i in start until arr.length()) trimmed.put(arr.getJSONObject(i))
        prefs.edit().putString(KEY_PRODUCT_SALES, trimmed.toString()).apply()
    }

    private const val KEY_OFFERS = "product_offers_percent"
    fun offerPercent(context: Context, productName: String): Int =
        try { org.json.JSONObject(preferences(context).getString(KEY_OFFERS, "{}") ?: "{}").optInt(productName, 0).coerceIn(0, 90) } catch (_: Exception) { 0 }

    fun setOfferPercent(context: Context, productName: String, percent: Int) {
        val safe = percent.coerceIn(0, 90)
        val json = try { org.json.JSONObject(preferences(context).getString(KEY_OFFERS, "{}") ?: "{}") } catch (_: Exception) { org.json.JSONObject() }
        if (safe == 0) json.remove(productName) else json.put(productName, safe)
        preferences(context).edit().putString(KEY_OFFERS, json.toString()).apply()
    }

    fun promotionalPrice(context: Context, product: AdminProduto): Double =
        product.preco * (1.0 - offerPercent(context, product.nome) / 100.0)

    fun registerSale(context: Context, total: Double) {
        if (CompraTesteCeoStore.ativo(context)) return
        require(total.isFinite() && total >= 0.0) { "Total de venda inválido." }
        val prefs = preferences(context)
        val currentTotal = prefs.getString(KEY_SALES, null)?.toDoubleOrNull()
            ?: prefs.getFloat(KEY_SALES, 0f).toDouble()
        val log = try { JSONArray(prefs.getString(KEY_SALE_LOG, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        log.put(JSONObject().put("total", total).put("timestamp", System.currentTimeMillis()))
        prefs.edit().putString(KEY_SALES, (currentTotal + total).toString()).putString(KEY_SALE_LOG, log.toString()).apply()
    }

    /** Confirma uma venda local: valida disponibilidade, baixa estoque e registra o faturamento. */
    fun finalizarVenda(context: Context, itens: List<ProdutoItem>): String? {
        if (CompraTesteCeoStore.ativo(context)) return "Modo de teste: não é permitido registrar uma venda real."

        if (itens.isEmpty()) return "O carrinho está vazio."
        if (itens.any { it.quantidade <= 0 || !it.preco.isFinite() || it.preco < 0.0 }) {
            return "O carrinho contém um item inválido."
        }
        val produtos = products(context)
        val quantidades = itens.groupBy { it.nome.trim().lowercase() }
            .mapValues { (_, grupo) -> grupo.sumOf { it.quantidade } }
        val porNome = produtos.associateBy { it.nome.trim().lowercase() }
        quantidades.forEach { (nome, qtd) ->
            val produto = porNome[nome] ?: return "O produto '${itens.first { it.nome.trim().lowercase() == nome }.nome}' não está cadastrado no estoque."
            if (!produto.ativo || !produto.anuncioAtivo) return "${produto.nome} está temporariamente indisponível para compra."
            if (produto.estoque < qtd) return "Estoque insuficiente para ${produto.nome}. Disponível: ${produto.estoque}."
        }
        quantidades.forEach { (nome, qtd) -> porNome.getValue(nome).estoque -= qtd }
        saveProducts(context, produtos)
        val totalVenda = itens.sumOf { it.preco * it.quantidade }
        registerSale(context, totalVenda)
        registrarItensVenda(context, itens)
        AnalyticsLocalStore.markPurchase(context, itens, totalVenda)
        return null
    }

    fun salesToday(context: Context): Double = salesTodayRecords(context).sumOf { it.total }

    fun salesTodayCount(context: Context): Int = salesTodayRecords(context).size

    fun averageSaleToday(context: Context): Double {
        val sales = salesTodayRecords(context)
        return if (sales.isEmpty()) 0.0 else sales.sumOf { it.total } / sales.size
    }

    fun recentSales(context: Context, limit: Int = 8): List<AdminVenda> =
        allSales(context).sortedByDescending { it.timestamp }.take(limit.coerceAtLeast(0))

    /** Série diária usada pelo gráfico executivo dos últimos dias. */
    fun dailySales(context: Context, days: Int = 7): List<AdminResumoDiario> {
        val safeDays = days.coerceIn(1, 31)
        val dayFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT)
        val labelFormat = java.text.SimpleDateFormat("dd/MM", java.util.Locale("pt", "BR"))
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.DAY_OF_MONTH, -(safeDays - 1))
        }
        val daysList = (0 until safeDays).map {
            val date = java.util.Date(calendar.timeInMillis)
            val entry = Pair(dayFormat.format(date), labelFormat.format(date))
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            entry
        }
        val grouped = allSales(context).groupBy { dayFormat.format(java.util.Date(it.timestamp)) }
        return daysList.map { (key, label) ->
            val sales = grouped[key].orEmpty()
            AdminResumoDiario(label, sales.sumOf { it.total }, sales.size)
        }
    }

    private fun salesTodayRecords(context: Context): List<AdminVenda> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60L * 60L * 1000L
        return allSales(context).filter { it.timestamp in start until end }
    }

    private fun allSales(context: Context): List<AdminVenda> {
        val arr = try { JSONArray(preferences(context).getString(KEY_SALE_LOG, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            AdminVenda(o.optDouble("total", 0.0), o.optLong("timestamp", 0L))
        }
    }

    fun inventoryValue(context: Context): Double = products(context).sumOf { it.preco * it.estoque }
    fun lowStockCount(context: Context, threshold: Int = 5): Int = products(context).count { it.estoque <= threshold }

    fun adjustStock(context: Context, productName: String, delta: Int): Boolean {
        val list = products(context)
        val item = list.firstOrNull { it.nome.equals(productName, true) } ?: return false
        if (item.estoque + delta < 0) return false
        item.estoque += delta
        saveProducts(context, list)
        return true
    }

    fun totalSales(context: Context): Double {
        val prefs = preferences(context)
        return (prefs.getString(KEY_SALES, null)?.toDoubleOrNull()
            ?: prefs.getFloat(KEY_SALES, 0f).toDouble()).coerceAtLeast(0.0)
    }

    private fun catalogoInicial() = RcarLocalStore.catalog.map { item ->
        AdminProduto(item.nome, "Automotivo", item.preco, 25)
    }.toMutableList()

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
