package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/**
 * Recebimento em duas etapas: nenhum produto ou saldo é alterado antes da aprovação do CEO.
 * Armazenamento LOCAL para demonstração; múltiplos aparelhos exigem backend transacional.
 */
data class ConferenciaLinha(
    val numero: Int,
    val produto: String,
    val existente: String?,
    val categoria: String,
    val sku: String,
    val ean: String,
    val custo: Double,
    val precoVenda: Double,
    val previsto: Int,
    val contado: Int,
    val aprovado: Int = contado
)

data class ConferenciaEvento(val acao: String, val autor: String, val data: Long, val detalhe: String)

data class ConferenciaEstoque(
    val id: String,
    val origem: String,
    val documento: String,
    val notaId: String,
    val notaHash: String,
    val fornecedor: String,
    val funcionario: String,
    val situacao: String,
    val criadoEm: Long,
    val linhas: List<ConferenciaLinha>,
    val observacao: String,
    val eventos: List<ConferenciaEvento>
)

object EstoqueConferenciaStore {
    const val PENDENTE = "PENDENTE"
    const val DEVOLVIDA = "DEVOLVIDA"
    const val APROVADA = "APROVADA"
    private const val PREF = "rcar_admin"
    private const val KEY = "inventory_conferences_v1"
    private const val LEDGER = "nfe_import_ledger_v1"
    private const val MAX = 200

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun session(ctx: Context) = SessaoRcar.atual(ctx) ?: error("É necessário entrar na conta.")
    private fun isCeo(ctx: Context): Boolean {
        val email = session(ctx).email
        return UsuarioLocalRepository(ctx).isCeo(email)
    }
    private fun canCount(ctx: Context): String {
        val conta = session(ctx)
        val repo = UsuarioLocalRepository(ctx)
        require(repo.usuarioAtivo(conta.email) &&
            (repo.isCeo(conta.email) || repo.temPermissao(conta.email, "conferir_recebimento") ||
                repo.temPermissao(conta.email, "cadastrar_produtos") || repo.temPermissao(conta.email, "lancar_notas"))) {
            "O CEO precisa habilitar a permissão Conferir recebimentos para este funcionário."
        }
        return conta.email
    }
    private fun requireCeo(ctx: Context): String {
        val conta = session(ctx)
        require(isCeo(ctx) && UsuarioLocalRepository(ctx).usuarioAtivo(conta.email)) { "Apenas o CEO pode aprovar ou corrigir estoque." }
        return conta.email
    }
    private fun event(acao: String, actor: String, detalhe: String) =
        ConferenciaEvento(acao, actor, System.currentTimeMillis(), detalhe.trim().take(500))

    private fun parseLine(line: JSONObject) = ConferenciaLinha(
        numero = line.getInt("numero"), produto = line.getString("produto"),
        existente = line.optString("existente").takeIf { it.isNotBlank() },
        categoria = line.optString("categoria"), sku = line.optString("sku"), ean = line.optString("ean"),
        custo = line.optDouble("custo"), precoVenda = line.optDouble("precoVenda"),
        previsto = line.optInt("previsto"), contado = line.optInt("contado"),
        aprovado = line.optInt("aprovado", line.optInt("contado"))
    )
    private fun pack(line: ConferenciaLinha) = JSONObject().apply {
        put("numero", line.numero); put("produto", line.produto); put("existente", line.existente ?: "")
        put("categoria", line.categoria); put("sku", line.sku); put("ean", line.ean)
        put("custo", line.custo); put("precoVenda", line.precoVenda)
        put("previsto", line.previsto); put("contado", line.contado); put("aprovado", line.aprovado)
    }
    private fun parseConference(row: JSONObject): ConferenciaEstoque {
        val rows = row.getJSONArray("linhas")
        val history = row.optJSONArray("eventos") ?: JSONArray()
        return ConferenciaEstoque(
            id = row.getString("id"), origem = row.getString("origem"),
            documento = row.optString("documento"), notaId = row.optString("notaId"),
            notaHash = row.optString("notaHash"), fornecedor = row.optString("fornecedor"),
            funcionario = row.optString("funcionario"), situacao = row.optString("situacao", PENDENTE),
            criadoEm = row.optLong("criadoEm"),
            linhas = (0 until rows.length()).map { parseLine(rows.getJSONObject(it)) },
            observacao = row.optString("observacao"),
            eventos = (0 until history.length()).map {
                val e = history.getJSONObject(it)
                ConferenciaEvento(e.optString("acao"), e.optString("autor"), e.optLong("data"), e.optString("detalhe"))
            }
        )
    }
    private fun pack(item: ConferenciaEstoque) = JSONObject().apply {
        put("id", item.id); put("origem", item.origem); put("documento", item.documento)
        put("notaId", item.notaId); put("notaHash", item.notaHash); put("fornecedor", item.fornecedor)
        put("funcionario", item.funcionario); put("situacao", item.situacao); put("criadoEm", item.criadoEm)
        put("observacao", item.observacao)
        put("linhas", JSONArray().apply { item.linhas.forEach { put(pack(it)) } })
        put("eventos", JSONArray().apply { item.eventos.forEach { event -> put(JSONObject().apply {
            put("acao", event.acao); put("autor", event.autor); put("data", event.data); put("detalhe", event.detalhe)
        }) } })
    }
    private fun encode(list: List<ConferenciaEstoque>): String = JSONArray().apply { list.forEach { put(pack(it)) } }.toString()

    fun list(ctx: Context): List<ConferenciaEstoque> = try {
        val rows = JSONArray(pref(ctx).getString(KEY, "[]") ?: "[]")
        (0 until rows.length()).map { parseConference(rows.getJSONObject(it)) }.sortedByDescending { it.criadoEm }
    } catch (_: Exception) { emptyList() }

    private fun save(ctx: Context, items: List<ConferenciaEstoque>): Boolean =
        pref(ctx).edit().putString(KEY, encode(items.take(MAX))).commit()

    private fun validateLine(line: ConferenciaLinha) {
        require(line.produto.isNotBlank() && line.categoria.isNotBlank()) { "Nome e categoria são obrigatórios." }
        require(line.previsto in 0..1_000_000 && line.contado in 0..1_000_000 && line.aprovado in 0..1_000_000) {
            "Quantidade inválida para ${line.produto}."
        }
        require(line.custo.isFinite() && line.custo >= 0.0 && line.custo < 1e9 &&
            line.precoVenda.isFinite() && line.precoVenda > 0.0 && line.precoVenda < 1e9) {
            "Custo ou preço de venda inválido para ${line.produto}."
        }
    }
    private fun validateAll(lines: List<ConferenciaLinha>) {
        require(lines.isNotEmpty() && lines.size <= 400) { "Informe de 1 a 400 linhas." }
        lines.forEach(::validateLine)
        require(lines.map { it.numero }.distinct().size == lines.size) { "Linha duplicada na conferência." }
    }

    @Synchronized fun hasInvoice(ctx: Context, id: String, hash: String): Boolean =
        list(ctx).any { it.notaId.isNotBlank() && (it.notaId == id || it.notaHash == hash) }

    /** Registra a contagem da nota, sem criar produtos nem movimentar quantidades. */
    @Synchronized fun submitInvoice(ctx: Context, invoice: NfeInvoice, choices: List<NfeChoice>, observation: String): ConferenciaEstoque {
        val actor = canCount(ctx)
        require(!NfeImportStore.alreadyImported(ctx, invoice) && !hasInvoice(ctx, invoice.importId, invoice.hash)) {
            "Esta NF-e já está em conferência ou foi lançada no estoque."
        }
        val rows = choices.filter { it.selected }.map { choice ->
            val expected = runCatching { choice.item.quantity.intValueExact() }.getOrDefault(0).coerceAtLeast(0)
            ConferenciaLinha(choice.item.line, choice.productName, choice.existingOriginalName, choice.category,
                NfeImportStore.suggestedSku(invoice, choice.item), choice.item.gtin,
                choice.item.unitCost.toDouble(), choice.salePrice, expected, choice.quantity)
        }
        validateAll(rows)
        return append(ctx, ConferenciaEstoque(UUID.randomUUID().toString(), "NFE", invoice.number, invoice.importId,
            invoice.hash, invoice.supplier, actor, PENDENTE, System.currentTimeMillis(), rows, observation,
            listOf(event("ENVIADA", actor, "NF-e ${invoice.number} • ${rows.size} itens para aprovação"))))
    }

    /** Recebimento avulso de produto existente, igualmente sujeito à aprovação. */
    @Synchronized fun submitManual(ctx: Context, document: String, observations: String, lines: List<ConferenciaLinha>): ConferenciaEstoque {
        val actor = canCount(ctx)
        require(document.isNotBlank()) { "Informe a referência da entrega, compra ou remessa." }
        require(lines.all { !it.existente.isNullOrBlank() }) { "Recebimento manual só pode usar produtos cadastrados." }
        validateAll(lines)
        val reference = document.trim().take(90)
        if (reference.startsWith("REPO:")) {
            val request = SolicitacaoEquipeStore.todas(ctx).firstOrNull { it.id == reference.removePrefix("REPO:") }
                ?: error("A solicitação de compra não foi encontrada.")
            require(request.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE &&
                request.status == SolicitacaoEquipeStore.STATUS_APROVADO &&
                lines.all { it.existente.equals(request.produto, true) }) {
                "A conferência deve corresponder à compra aprovada e ao mesmo produto."
            }
        }
        if (reference.startsWith("REPO:") || reference.startsWith("CADASTRO-")) {
            require(list(ctx).none { it.documento.equals(reference, true) }) {
                "Esta entrega já possui uma conferência. Abra o registro existente."
            }
        }
        return append(ctx, ConferenciaEstoque(UUID.randomUUID().toString(), "MANUAL", reference,
            "", "", "Recebimento manual", actor, PENDENTE, System.currentTimeMillis(), lines,
            observations, listOf(event("ENVIADA", actor, "Entrada manual aguardando aprovação do CEO"))))
    }

    private fun append(ctx: Context, item: ConferenciaEstoque): ConferenciaEstoque {
        require(save(ctx, listOf(item) + list(ctx))) { "Não foi possível salvar a conferência." }
        return item
    }

    @Synchronized fun returnToEmployee(ctx: Context, id: String, reason: String) {
        val ceo = requireCeo(ctx)
        require(reason.trim().length >= 5) { "Descreva a divergência para o funcionário (mínimo 5 caracteres)." }
        val rows = list(ctx).toMutableList()
        val at = rows.indexOfFirst { it.id == id }
        require(at >= 0 && rows[at].situacao == PENDENTE) { "A conferência não está pendente." }
        val original = rows[at]
        rows[at] = original.copy(situacao = DEVOLVIDA,
            eventos = original.eventos + event("DEVOLVIDA", ceo, reason))
        require(save(ctx, rows)) { "Falha ao devolver a conferência." }
    }

    /** Recontagem de quantidades e revisão do vínculo de cada peça devolvida. */
    @Synchronized fun resubmitLines(ctx: Context, id: String, amended: List<ConferenciaLinha>, note: String) {
        val actor = canCount(ctx)
        val rows = list(ctx).toMutableList()
        val at = rows.indexOfFirst { it.id == id }
        require(at >= 0 && rows[at].situacao == DEVOLVIDA) { "Conferência não está devolvida." }
        val original = rows[at]
        require(original.funcionario.equals(actor, true) || isCeo(ctx)) { "Somente o conferente ou o CEO pode reenviar." }
        require(amended.size == original.linhas.size && amended.indices.all {
            amended[it].numero == original.linhas[it].numero
        }) { "As linhas originais da nota devem ser preservadas." }
        require(original.origem != "MANUAL" || amended.all { !it.existente.isNullOrBlank() }) {
            "Recebimento manual precisa estar vinculado a um produto já cadastrado."
        }
        validateAll(amended)
        val currentProducts = AdminLocalStore.products(ctx)
        amended.forEach { line ->
            if (line.existente != null) require(currentProducts.any { it.nome.equals(line.existente, true) }) {
                "Produto ${line.existente} não foi encontrado no catálogo."
            }
        }
        rows[at] = original.copy(situacao = PENDENTE, linhas = amended.map { it.copy(aprovado = it.contado) },
            observacao = note.trim().take(500),
            eventos = original.eventos + event("REENVIADA", actor, "Recontagem e vínculos revisados. ${note.trim()}"))
        require(save(ctx, rows)) { "Falha ao reenviar a conferência." }
    }

    /** Aprovação e lançamento em operação local única: catálogo, nota lançada e fila de conferências. */
    @Synchronized fun approve(ctx: Context, id: String, approvedQty: List<Int>, reason: String): String {
        val actor = requireCeo(ctx)
        val rows = list(ctx).toMutableList()
        val index = rows.indexOfFirst { it.id == id }
        require(index >= 0 && rows[index].situacao == PENDENTE) { "A conferência não está aguardando aprovação." }
        val original = rows[index]
        require(approvedQty.size == original.linhas.size) { "Quantidade de linhas divergente." }
        val edited = original.linhas.mapIndexed { i, line -> line.copy(aprovado = approvedQty[i]) }
        validateAll(edited)
        val changes = edited.filter { it.contado != it.aprovado }
        require(changes.isEmpty() || reason.trim().length >= 5) {
            "Explique o motivo dos ajustes de quantidade do CEO."
        }
        if (original.documento.startsWith("REPO:")) {
            val request = SolicitacaoEquipeStore.todas(ctx).firstOrNull { it.id == original.documento.removePrefix("REPO:") }
                ?: error("A compra vinculada não existe mais.")
            require(request.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE &&
                request.status == SolicitacaoEquipeStore.STATUS_APROVADO &&
                edited.all { it.existente.equals(request.produto, true) }) {
                "A solicitação vinculada já mudou de status ou contém outro produto."
            }
        }
        if (original.notaId.isNotBlank()) {
            val log = JSONArray(pref(ctx).getString(LEDGER, "[]") ?: "[]")
            require((0 until log.length()).none { i ->
                val entry = log.getJSONObject(i)
                entry.optString("id") == original.notaId || entry.optString("hash") == original.notaHash
            }) { "A NF-e já foi lançada ao estoque." }
        }
        val products = AdminLocalStore.products(ctx)
        var total = 0L
        var created = 0
        edited.forEach { line ->
            val qty = line.aprovado
            total += qty.toLong()
            if (qty == 0) return@forEach
            val existingIndex = line.existente?.let { name -> products.indexOfFirst { it.nome.equals(name, true) } } ?: -1
            if (line.existente != null) require(existingIndex >= 0) { "Produto ${line.existente} não encontrado; devolva a conferência." }
            if (existingIndex >= 0) {
                val originalProduct = products[existingIndex]
                require(originalProduct.estoque <= Int.MAX_VALUE - qty) { "Estoque excederia o limite: ${originalProduct.nome}." }
                products[existingIndex] = originalProduct.copy(estoque = originalProduct.estoque + qty,
                    precoCusto = if (line.custo > 0.0) line.custo else originalProduct.precoCusto)
            } else {
                require(products.none { it.nome.equals(line.produto, true) || (line.sku.isNotBlank() && it.sku.equals(line.sku, true)) }) {
                    "Produto ou SKU já existe: ${line.produto}. Vincule corretamente antes de aprovar."
                }
                products.add(AdminProduto(nome = line.produto.trim(), categoria = line.categoria.trim(),
                    preco = line.precoVenda, estoque = qty, sku = line.sku, codigoBarras = line.ean,
                    precoCusto = line.custo, fornecedor = original.fornecedor,
                    cadastradoPor = original.funcionario, anuncioAtivo = false,
                    descricao = "Recebido na conferência ${original.documento} • aprovação CEO"))
                created++
            }
        }
        require(total > 0) { "Nenhuma unidade aprovada. Devolva a conferência ou informe os itens recebidos." }
        rows[index] = original.copy(situacao = APROVADA, linhas = edited, eventos = original.eventos +
            event("APROVADA", actor, "${total} unidades lançadas, $created produto(s) novo(s). ${reason.trim()}"))
        val ledger = JSONArray(pref(ctx).getString(LEDGER, "[]") ?: "[]")
        if (original.notaId.isNotBlank()) ledger.put(JSONObject().apply {
            put("id", original.notaId); put("hash", original.notaHash); put("number", original.documento)
            put("supplier", original.fornecedor); put("lines", edited.size); put("units", total)
            put("actor", actor); put("timestamp", System.currentTimeMillis()); put("conferenceId", id)
        })
        require(AdminLocalStore.saveConferenceApprovalAtomically(ctx, products, encode(rows), ledger.toString())) {
            "Falha ao salvar a aprovação e o estoque juntos."
        }
        if (original.documento.startsWith("REPO:")) {
            // A fila de compras é local e separada do estoque; somente após aprovação física.
            val purchaseId = original.documento.removePrefix("REPO:")
            SolicitacaoEquipeStore.atualizarStatus(ctx, purchaseId, SolicitacaoEquipeStore.STATUS_RECEBIDO)
        }
        return "$total unidades lançadas no estoque; $created produto(s) novo(s)."
    }

    /** Correções posteriores à aprovação exigem justificativa e deixam trilha de auditoria. */
    @Synchronized fun correctStock(ctx: Context, id: String, itemIndex: Int, delta: Int, reason: String) {
        val actor = requireCeo(ctx)
        require(delta != 0 && delta in -1_000_000..1_000_000) { "Informe uma correção válida." }
        require(reason.trim().length >= 5) { "Informe o motivo da correção (mínimo 5 caracteres)." }
        val rows = list(ctx).toMutableList()
        val pos = rows.indexOfFirst { it.id == id }
        require(pos >= 0 && rows[pos].situacao == APROVADA) { "Somente conferências aprovadas podem ter ajuste posterior." }
        val conf = rows[pos]
        require(itemIndex in conf.linhas.indices) { "Item não encontrado." }
        val line = conf.linhas[itemIndex]
        val name = line.existente ?: line.produto
        val products = AdminLocalStore.products(ctx)
        val productIndex = products.indexOfFirst { it.nome.equals(name, true) }
        require(productIndex >= 0) { "Produto não encontrado. Ajuste pelo cadastro e registre a auditoria." }
        val product = products[productIndex]
        val newQty = product.estoque.toLong() + delta.toLong()
        require(newQty in 0..Int.MAX_VALUE.toLong()) { "A correção deixaria estoque negativo ou fora do limite." }
        products[productIndex] = product.copy(estoque = newQty.toInt())
        rows[pos] = conf.copy(eventos = conf.eventos + event("AJUSTE_ESTOQUE", actor,
            "$name: ${if (delta > 0) "+" else ""}$delta unidades • $reason"))
        require(AdminLocalStore.saveConferenceApprovalAtomically(ctx, products, encode(rows),
            pref(ctx).getString(LEDGER, "[]") ?: "[]")) { "Falha ao salvar ajuste e auditoria." }
    }
}
