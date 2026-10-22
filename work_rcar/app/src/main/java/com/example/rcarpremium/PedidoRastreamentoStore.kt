package com.example.rcarpremium

import android.content.Context
import org.json.JSONObject

/** Última posição RECEBIDA (nunca gerada artificialmente). Armazenamento local até backend/transporte. */
data class RastreamentoInfo(
    val pedidoId: String, val transportadora: String, val codigo: String,
    val previsao: String, val latitude: Double?, val longitude: Double?,
    val atualizadoEm: Long, val atualizadoPor: String, val observacao: String
)
object PedidoRastreamentoStore {
    private const val PREF = "rcar_tracking_local_v1"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun get(ctx: Context, pedidoId: String): RastreamentoInfo? {
        val raw = prefs(ctx).getString("pedido_$pedidoId", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            RastreamentoInfo(pedidoId, o.optString("transportadora"), o.optString("codigo"),
                o.optString("previsao"), if (o.isNull("latitude")) null else o.getDouble("latitude"),
                if (o.isNull("longitude")) null else o.getDouble("longitude"),
                o.optLong("atualizadoEm"), o.optString("atualizadoPor"), o.optString("observacao"))
        }.getOrNull()
    }
    @Synchronized fun atualizar(ctx: Context, id: String, transportadora: String, codigo: String,
        previsao: String, latRaw: String, lonRaw: String, observacao: String, editor: String) {
        val repo = UsuarioLocalRepository(ctx)
        require(repo.usuarioAtivo(editor) && (repo.isCeo(editor) || repo.temPermissao(editor, "gerenciar_pedidos"))) {
            "Sem permissão para atualizar entrega."
        }
        require(PedidoLocalStore.listar(ctx).any { it.id == id }) { "Pedido inexistente" }
        require(transportadora.length <= 100 && codigo.length <= 100 && previsao.length <= 80 && observacao.length <= 500) {
            "Informação de entrega muito longa."
        }
        val hasLoc = latRaw.isNotBlank() || lonRaw.isNotBlank()
        require(!hasLoc || (latRaw.isNotBlank() && lonRaw.isNotBlank())) { "Informe latitude e longitude juntas." }
        val lat = if (hasLoc) latRaw.replace(',', '.').toDoubleOrNull() else null
        val lon = if (hasLoc) lonRaw.replace(',', '.').toDoubleOrNull() else null
        require(!hasLoc || (lat != null && lon != null && lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)) {
            "Coordenadas inválidas."
        }
        val old = get(ctx, id)
        val storedLat = if (hasLoc) lat else old?.latitude
        val storedLon = if (hasLoc) lon else old?.longitude
        val updated = JSONObject().apply {
            put("transportadora", transportadora.trim()); put("codigo", codigo.trim()); put("previsao", previsao.trim())
            put("latitude", storedLat ?: JSONObject.NULL); put("longitude", storedLon ?: JSONObject.NULL)
            put("atualizadoEm", System.currentTimeMillis()); put("atualizadoPor", editor.trim())
            put("observacao", observacao.trim())
        }
        check(prefs(ctx).edit().putString("pedido_$id", updated.toString()).commit()) { "Erro ao salvar rastreio." }
    }
}
