package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Ambiente de compra isolado para ensaios do CEO.
 * Não utiliza carrinho, estoque, pedidos nem estatísticas da operação comercial.
 * Não envia cobranças Pix/cartão/boleto e nunca aciona gateways de pagamento.
 */
object CompraTesteCeoStore {
    private const val PREFS = "rcar_compra_teste_ceo"
    private const val EMAIL_CEO = "ceo_em_teste"
    private const val CART = "carrinho_teste"
    private const val ADDRESS = "endereco_teste"
    private const val HISTORY = "historico_teste"

    data class PedidoTeste(
        val id: String,
        val criadoEm: Long,
        val emailCeo: String,
        val itens: String,
        val total: Double,
        val pagamento: String,
        val endereco: String
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autorizado(context: Context): Boolean {
        val email = RcarLocalStore.currentEmail(context) ?: return false
        val usuarios = UsuarioLocalRepository(context)
        return usuarios.usuarioAtivo(email) && usuarios.isCeo(email)
    }

    fun ativo(context: Context): Boolean {
        val email = RcarLocalStore.currentEmail(context)?.trim()?.lowercase(Locale.ROOT)
        return !email.isNullOrBlank() && autorizado(context) &&
            prefs(context).getString(EMAIL_CEO, null) == email
    }

    fun iniciar(context: Context): Boolean {
        if (!autorizado(context)) return false
        val email = RcarLocalStore.currentEmail(context)!!.trim().lowercase(Locale.ROOT)
        prefs(context).edit().putString(EMAIL_CEO, email).remove(CART).remove(ADDRESS).apply()
        return true
    }

    /** Mantém histórico de testes, mas descarta a sessão e o carrinho de demonstração. */
    fun encerrar(context: Context) {
        prefs(context).edit().remove(EMAIL_CEO).remove(CART).remove(ADDRESS).apply()
    }

    fun carrinho(context: Context): MutableList<ProdutoItem> {
        if (!ativo(context)) return mutableListOf()
        val data = prefs(context).getString(CART, "[]") ?: "[]"
        return try {
            val arr = JSONArray(data)
            (0 until arr.length()).map { n ->
                val item = arr.getJSONObject(n)
                ProdutoItem(item.getString("nome"), item.getDouble("preco"), item.getInt("qtd"), R.drawable.pecasautomotiva)
            }.toMutableList()
        } catch (_: Exception) { mutableListOf() }
    }

    fun salvarCarrinho(context: Context, itens: List<ProdutoItem>) {
        if (!ativo(context)) return
        val arr = JSONArray()
        itens.filter { it.quantidade > 0 && it.preco >= 0.0 }.forEach { produto ->
            arr.put(JSONObject().apply {
                put("nome", produto.nome)
                put("preco", produto.preco)
                put("qtd", produto.quantidade)
            })
        }
        prefs(context).edit().putString(CART, arr.toString()).apply()
    }

    fun adicionar(context: Context, produto: ProdutoItem) {
        if (!ativo(context)) return
        val itens = carrinho(context)
        val anterior = itens.firstOrNull { it.nome.equals(produto.nome, ignoreCase = true) }
        if (anterior == null) itens.add(produto.copy())
        else anterior.quantidade += produto.quantidade
        salvarCarrinho(context, itens)
    }

    fun limparCarrinho(context: Context) = prefs(context).edit().remove(CART).apply()

    fun endereco(context: Context): String = if (ativo(context)) prefs(context).getString(ADDRESS, "").orEmpty() else ""

    fun salvarEndereco(context: Context, endereco: String) {
        if (ativo(context)) prefs(context).edit().putString(ADDRESS, endereco.trim()).apply()
    }

    /** Os pedidos de teste jamais entram no PedidoLocalStore ou nos relatórios reais. */
    fun concluir(context: Context, pagamento: String, freteSimulado: Double): PedidoTeste? {
        if (!ativo(context)) return null
        val itens = carrinho(context)
        val endereco = endereco(context)
        if (itens.isEmpty() || endereco.isBlank()) return null
        val pedidos = historico(context).toMutableList()
        val momento = System.currentTimeMillis()
        val pedido = PedidoTeste(
            id = "TESTE-${momento}",
            criadoEm = momento,
            emailCeo = RcarLocalStore.currentEmail(context).orEmpty(),
            itens = itens.joinToString(" • ") { "${it.nome} × ${it.quantidade}" },
            total = itens.sumOf { it.preco * it.quantidade } + freteSimulado.coerceAtLeast(0.0),
            pagamento = pagamento,
            endereco = endereco
        )
        pedidos.add(0, pedido)
        val arr = JSONArray()
        pedidos.take(80).forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("criadoEm", item.criadoEm)
                put("emailCeo", item.emailCeo); put("itens", item.itens)
                put("total", item.total); put("pagamento", item.pagamento)
                put("endereco", item.endereco)
            })
        }
        prefs(context).edit().putString(HISTORY, arr.toString()).remove(CART).apply()
        return pedido
    }

    fun historico(context: Context): List<PedidoTeste> {
        if (!autorizado(context)) return emptyList()
        val emailAtual = RcarLocalStore.currentEmail(context).orEmpty()
        val arr = try { JSONArray(prefs(context).getString(HISTORY, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (!o.optString("emailCeo").equals(emailAtual, true)) return@mapNotNull null
            PedidoTeste(o.optString("id"), o.optLong("criadoEm"), o.optString("emailCeo"),
                o.optString("itens"), o.optDouble("total"), o.optString("pagamento"), o.optString("endereco"))
        }.sortedByDescending { it.criadoEm }
    }

    fun limparHistorico(context: Context) {
        if (!autorizado(context)) return
        val emailAtual = RcarLocalStore.currentEmail(context).orEmpty()
        val outros = try { JSONArray(prefs(context).getString(HISTORY, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        val arr = JSONArray()
        (0 until outros.length()).forEach { i ->
            val o = outros.optJSONObject(i) ?: return@forEach
            if (!o.optString("emailCeo").equals(emailAtual, true)) arr.put(o)
        }
        prefs(context).edit().putString(HISTORY, arr.toString()).apply()
    }
}
