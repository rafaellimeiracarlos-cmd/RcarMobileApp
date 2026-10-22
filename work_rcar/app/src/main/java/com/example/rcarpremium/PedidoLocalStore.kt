package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

/** Itens e preços preservados no momento da compra, sem depender do preço atual do catálogo. */
data class PedidoProduto(
    val nome: String,
    val quantidade: Int,
    val precoUnitario: Double? = null
)

/** Pedido salvo localmente para o modo de demonstração do RCarPremium. */
data class PedidoLocal(
    val id: String,
    val clienteEmail: String,
    val clienteNome: String,
    val resumoItens: String,
    val total: Double,
    val quantidadeItens: Int,
    val status: String,
    val criadoEm: Long,
    val formaPagamento: String,
    val notaInterna: String = "",
    val atualizadoEm: Long = 0L,
    val atualizadoPor: String = "",
    val demonstracao: Boolean = false,
    val itens: List<PedidoProduto> = emptyList()
)

/**
 * Persistência de pedidos e notas internas no aparelho.
 * A autorização para editar notas e status é validada aqui e também na interface.
 * Para produção, a mesma regra deve ser aplicada no backend central.
 */
object PedidoLocalStore {
    private const val PREFS = "rcar_pedidos_local"
    private const val KEY_PEDIDOS = "pedidos_v1"

    val statusDisponiveis = listOf("Em andamento", "Separando produtos", "Enviado", "Entregue", "Cancelado")

    fun listar(context: Context): List<PedidoLocal> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_PEDIDOS)) criarDadosDeDemonstracao(context)
        val raw = prefs.getString(KEY_PEDIDOS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                PedidoLocal(
                    id = item.optString("id"),
                    clienteEmail = item.optString("clienteEmail"),
                    clienteNome = item.optString("clienteNome", "Cliente"),
                    resumoItens = item.optString("resumoItens", "Produtos do pedido"),
                    total = item.optDouble("total", 0.0).coerceAtLeast(0.0),
                    quantidadeItens = item.optInt("quantidadeItens", 0).coerceAtLeast(0),
                    status = item.optString("status", "Em andamento"),
                    criadoEm = item.optLong("criadoEm", 0L),
                    formaPagamento = item.optString("formaPagamento", "Não informado"),
                    notaInterna = item.optString("notaInterna", ""),
                    atualizadoEm = item.optLong("atualizadoEm", 0L),
                    atualizadoPor = item.optString("atualizadoPor", ""),
                    demonstracao = item.optBoolean("demonstracao", false),
                    itens = lerItens(item.optJSONArray("itens"))
                )
            }.sortedByDescending { it.criadoEm }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun listarDoCliente(context: Context, email: String): List<PedidoLocal> =
        listar(context).filter { it.clienteEmail.equals(email.trim(), ignoreCase = true) }

    /** Mantém pedidos antigos legíveis mesmo antes de existirem linhas de produto persistidas. */
    fun produtosDoPedido(pedido: PedidoLocal): List<PedidoProduto> {
        if (pedido.itens.isNotEmpty()) return pedido.itens
        return pedido.resumoItens.split(" • ").mapNotNull { trecho ->
            val s = trecho.trim()
            if (s.isEmpty()) return@mapNotNull null
            val partes = Regex("^(.+?)\\s*[×x]\\s*(\\d+)$").matchEntire(s)
            val nome = partes?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { s }
            val qtd = partes?.groupValues?.getOrNull(2)?.toIntOrNull()
                ?: if (pedido.resumoItens.split(" • ").size == 1) pedido.quantidadeItens.coerceAtLeast(1) else 1
            PedidoProduto(nome, qtd.coerceIn(1, 10000), null)
        }
    }

    private fun lerItens(array: JSONArray?): List<PedidoProduto> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { indice ->
            val item = array.optJSONObject(indice) ?: return@mapNotNull null
            val nome = item.optString("nome").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val quantidade = item.optInt("quantidade", 0).takeIf { it > 0 } ?: return@mapNotNull null
            val preco = if (item.has("precoUnitario") && !item.isNull("precoUnitario")) {
                item.optDouble("precoUnitario", -1.0).takeIf { it.isFinite() && it >= 0.0 }
            } else null
            PedidoProduto(nome, quantidade, preco)
        }
    }

    fun registrarCompra(
        context: Context,
        email: String?,
        itens: List<ProdutoItem>,
        total: Double,
        formaPagamento: String
    ): PedidoLocal? {
        if (CompraTesteCeoStore.ativo(context)) return null
        if (email.isNullOrBlank() || itens.isEmpty() || total < 0.0) return null
        val repository = UsuarioLocalRepository(context)
        val now = System.currentTimeMillis()
        val id = gerarIdUnico(context)
        val resumo = itens.joinToString(" • ") { item -> "${item.nome} × ${item.quantidade}" }
        val pedido = PedidoLocal(
            id = id,
            clienteEmail = email.trim().lowercase(Locale.ROOT),
            clienteNome = repository.nomeDoUsuario(email) ?: "Cliente RCar Premium",
            resumoItens = resumo,
            total = total,
            quantidadeItens = itens.sumOf { it.quantidade.coerceAtLeast(0) },
            status = "Em andamento",
            criadoEm = now,
            formaPagamento = formaPagamento,
            atualizadoEm = now,
            itens = itens.filter { it.quantidade > 0 }.map {
                PedidoProduto(it.nome.trim(), it.quantidade, it.preco.takeIf { preco -> preco.isFinite() && preco >= 0.0 })
            }
        )
        salvarTodos(context, listar(context) + pedido)
        return pedido
    }

    fun salvarNota(context: Context, pedidoId: String, nota: String, editorEmail: String): Boolean {
        if (!podeGerenciarPedidos(context, editorEmail)) return false
        val existente = listar(context)
        if (existente.none { it.id == pedidoId }) return false
        val agora = System.currentTimeMillis()
        val atualizados = existente.map { pedido ->
            if (pedido.id == pedidoId) pedido.copy(
                notaInterna = nota.trim(),
                atualizadoEm = agora,
                atualizadoPor = editorEmail.trim().lowercase(Locale.ROOT)
            ) else pedido
        }
        salvarTodos(context, atualizados)
        return true
    }

    fun atualizarStatus(context: Context, pedidoId: String, status: String, editorEmail: String): Boolean {
        if (!podeGerenciarPedidos(context, editorEmail) || status !in statusDisponiveis) return false
        val existente = listar(context)
        if (existente.none { it.id == pedidoId }) return false
        val agora = System.currentTimeMillis()
        salvarTodos(context, existente.map { pedido ->
            if (pedido.id == pedidoId) pedido.copy(
                status = status,
                atualizadoEm = agora,
                atualizadoPor = editorEmail.trim().lowercase(Locale.ROOT)
            ) else pedido
        })
        return true
    }

    fun podeGerenciarPedidos(context: Context, email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val repo = UsuarioLocalRepository(context)
        if (!repo.usuarioAtivo(email)) return false
        return repo.isCeo(email) || repo.temPermissao(email, "gerenciar_pedidos")
    }

    fun dataFormatada(timestamp: Long): String {
        if (timestamp <= 0L) return "Data não disponível"
        return SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(timestamp))
    }

    private fun gerarIdUnico(context: Context): String {
        val existentes = listar(context).map { it.id }.toSet()
        repeat(20) {
            val candidate = ThreadLocalRandom.current().nextInt(100000, 999999).toString()
            if (candidate !in existentes) return candidate
        }
        return System.currentTimeMillis().toString().takeLast(6)
    }

    private fun criarDadosDeDemonstracao(context: Context) {
        val agora = System.currentTimeMillis()
        val exemplos = listOf(
            PedidoLocal(
                id = "123456",
                clienteEmail = "rafael@rcarpremium.com",
                clienteNome = "Rafael (cliente de demonstração)",
                resumoItens = "Kit Pastilha de Freio RCar Premium",
                total = 159.70,
                quantidadeItens = 1,
                status = "Em andamento",
                criadoEm = agora - 2L * 60L * 60L * 1000L,
                formaPagamento = "Demonstração",
                demonstracao = true
            ),
            PedidoLocal(
                id = "987654",
                clienteEmail = "rafael@rcarpremium.com",
                clienteNome = "Rafael (cliente de demonstração)",
                resumoItens = "Shampoo + Cera Automotiva",
                total = 89.90,
                quantidadeItens = 2,
                status = "Entregue",
                criadoEm = agora - 2L * 24L * 60L * 60L * 1000L,
                formaPagamento = "Demonstração",
                demonstracao = true
            )
        )
        salvarTodos(context, exemplos)
    }

    private fun salvarTodos(context: Context, pedidos: List<PedidoLocal>) {
        val array = JSONArray()
        pedidos.sortedByDescending { it.criadoEm }.take(500).forEach { pedido ->
            array.put(JSONObject().apply {
                put("id", pedido.id)
                put("clienteEmail", pedido.clienteEmail)
                put("clienteNome", pedido.clienteNome)
                put("resumoItens", pedido.resumoItens)
                put("total", pedido.total)
                put("quantidadeItens", pedido.quantidadeItens)
                put("status", pedido.status)
                put("criadoEm", pedido.criadoEm)
                put("formaPagamento", pedido.formaPagamento)
                put("notaInterna", pedido.notaInterna)
                put("atualizadoEm", pedido.atualizadoEm)
                put("atualizadoPor", pedido.atualizadoPor)
                put("demonstracao", pedido.demonstracao)
                put("itens", JSONArray().apply {
                    pedido.itens.forEach { produto ->
                        put(JSONObject().apply {
                            put("nome", produto.nome)
                            put("quantidade", produto.quantidade)
                            produto.precoUnitario?.let { put("precoUnitario", it) }
                        })
                    }
                })
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PEDIDOS, array.toString()).apply()
    }
}
