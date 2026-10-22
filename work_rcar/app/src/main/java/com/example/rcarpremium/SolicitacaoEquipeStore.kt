package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Fila local de solicitações da equipe. Em modo offline, os registros ficam neste dispositivo;
 * sincronização entre aparelhos requer uma API/backend.
 */
data class SolicitacaoEquipe(
    val id: String,
    val tipo: String,
    val produto: String,
    val solicitante: String,
    val quantidade: Int,
    val criadoEm: Long,
    val status: String,
    val permissao: String = ""
)

object SolicitacaoEquipeStore {
    const val PAUSAR_ANUNCIO = "PAUSAR_ANUNCIO"
    const val REPOSICAO_ESTOQUE = "REPOSICAO_ESTOQUE"
    const val STATUS_PENDENTE = "PENDENTE"
    const val STATUS_APROVADO = "APROVADO"
    const val STATUS_NEGADO = "NEGADO"
    const val STATUS_RECEBIDO = "RECEBIDO"

    private const val PREFS = "rcar_equipe_workflow"
    private const val KEY_REQUESTS = "solicitacoes"

    fun todas(context: Context): List<SolicitacaoEquipe> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_REQUESTS, "[]")
        val array = try { JSONArray(raw ?: "[]") } catch (_: Exception) { JSONArray() }
        return (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            SolicitacaoEquipe(
                id = o.optString("id", ""),
                tipo = o.optString("tipo", ""),
                produto = o.optString("produto", ""),
                solicitante = o.optString("solicitante", ""),
                quantidade = o.optInt("quantidade", 0).coerceAtLeast(0),
                criadoEm = o.optLong("criadoEm", 0L),
                status = o.optString("status", STATUS_PENDENTE),
                permissao = o.optString("permissao", "")
            )
        }.sortedByDescending { it.criadoEm }
    }

    fun criar(
        context: Context,
        tipo: String,
        produto: String,
        solicitante: String,
        quantidade: Int = 0,
        permissao: String = ""
    ): SolicitacaoEquipe {
        require(tipo.isNotBlank() && solicitante.isNotBlank()) { "Solicitação inválida." }
        val request = SolicitacaoEquipe(
            id = UUID.randomUUID().toString(), tipo = tipo, produto = produto,
            solicitante = solicitante.trim().lowercase(), quantidade = quantitySafe(quantidade),
            criadoEm = System.currentTimeMillis(), status = STATUS_PENDENTE, permissao = permissao
        )
        val all = todas(context).toMutableList()
        all.add(request)
        salvar(context, all)
        return request
    }

    fun existeAberta(context: Context, tipo: String, produto: String, solicitante: String, permissao: String = ""): Boolean =
        todas(context).any {
            it.tipo == tipo && it.produto.equals(produto, true) &&
                it.solicitante.equals(solicitante, true) && it.permissao == permissao &&
                (it.status == STATUS_PENDENTE || (it.tipo == REPOSICAO_ESTOQUE && it.status == STATUS_APROVADO))
        }

    fun atualizarStatus(context: Context, id: String, status: String): Boolean {
        val all = todas(context).toMutableList()
        val index = all.indexOfFirst { it.id == id }
        if (index < 0) return false
        all[index] = all[index].copy(status = status)
        salvar(context, all)
        return true
    }

    private fun salvar(context: Context, requests: List<SolicitacaoEquipe>) {
        val arr = JSONArray()
        // Mantém histórico recente e estados pendentes/aprovados.
        requests.sortedBy { it.criadoEm }.takeLast(1000).forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("tipo", r.tipo); put("produto", r.produto)
                put("solicitante", r.solicitante); put("quantidade", r.quantidade)
                put("criadoEm", r.criadoEm); put("status", r.status); put("permissao", r.permissao)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_REQUESTS, arr.toString()).apply()
    }

    private fun quantitySafe(value: Int) = value.coerceAtLeast(0)
}
