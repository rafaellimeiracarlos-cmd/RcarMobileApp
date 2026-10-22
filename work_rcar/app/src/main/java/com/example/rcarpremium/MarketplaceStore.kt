package com.example.rcarpremium

import android.content.Context
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Vínculo local de lojas aos produtos existentes. Não substitui a API em produção. */
data class LojaParceira(
    val id: String,
    val nome: String,
    val cidade: String,
    val uf: String,
    val descricao: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ativa: Boolean = true
)

object MarketplaceStore {
    private const val PREFS = "rcar_marketplace_v3"
    private const val KEY_LOJAS = "lojas"
    private const val KEY_LINKS = "produtos_loja"
    private const val KEY_API = "backend_https"
    const val LOJA_PRINCIPAL_ID = "rcar_oficial"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Loja inicial sem localização fictícia; o CEO pode informar os dados reais. */
    fun lojas(context: Context): List<LojaParceira> {
        val raw = prefs(context).getString(KEY_LOJAS, null)
        if (raw == null) return listOf(LojaParceira(LOJA_PRINCIPAL_ID, "RCarPremium Oficial", "Não informada", "", "Loja principal"))
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                LojaParceira(
                    o.getString("id"), o.getString("nome"), o.optString("cidade"),
                    o.optString("uf"), o.optString("descricao"),
                    if (o.has("lat") && !o.isNull("lat")) o.getDouble("lat") else null,
                    if (o.has("lng") && !o.isNull("lng")) o.getDouble("lng") else null,
                    o.optBoolean("ativa", true)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun salvarLojas(context: Context, lojas: List<LojaParceira>) {
        val arr = JSONArray()
        lojas.forEach { l -> arr.put(JSONObject().apply {
            put("id", l.id); put("nome", l.nome); put("cidade", l.cidade)
            put("uf", l.uf); put("descricao", l.descricao)
            put("lat", l.latitude ?: JSONObject.NULL); put("lng", l.longitude ?: JSONObject.NULL)
            put("ativa", l.ativa)
        }) }
        check(prefs(context).edit().putString(KEY_LOJAS, arr.toString()).commit()) { "Falha ao salvar lojas." }
    }

    fun criar(context: Context, nome: String, cidade: String, uf: String, descricao: String,
              latitude: Double?, longitude: Double?) {
        require(nome.trim().length >= 3) { "Informe o nome da loja (mínimo 3 letras)." }
        require(cidade.isNotBlank()) { "Informe a cidade da loja." }
        require(uf.trim().length == 2) { "Informe a UF com duas letras." }
        require(lojas(context).none { it.nome.equals(nome.trim(), true) }) { "Essa loja já foi cadastrada." }
        require((latitude == null && longitude == null) ||
            (latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0)) { "Coordenadas inválidas." }
        salvarLojas(context, lojas(context) + LojaParceira(UUID.randomUUID().toString(),
            nome.trim(), cidade.trim(), uf.trim().uppercase(), descricao.trim(), latitude, longitude))
    }

    fun atualizar(context: Context, loja: LojaParceira) {
        require(loja.nome.trim().length >= 3 && loja.cidade.isNotBlank() && loja.uf.length == 2) { "Nome, cidade e UF são obrigatórios." }
        require((loja.latitude == null && loja.longitude == null) ||
            (loja.latitude != null && loja.longitude != null && loja.latitude in -90.0..90.0 && loja.longitude in -180.0..180.0)) { "Coordenadas inválidas." }
        val data = lojas(context)
        require(data.any { it.id == loja.id }) { "Loja não encontrada." }
        salvarLojas(context, data.map { if (it.id == loja.id) loja.copy(nome = loja.nome.trim(), cidade = loja.cidade.trim(), uf = loja.uf.trim().uppercase()) else it })
    }

    fun definirAtiva(context: Context, lojaId: String, ativa: Boolean) {
        require(lojaId != LOJA_PRINCIPAL_ID || ativa) { "A loja principal deve permanecer ativa." }
        salvarLojas(context, lojas(context).map { if (it.id == lojaId) it.copy(ativa = ativa) else it })
    }

    fun chaveProduto(p: AdminProduto) = p.sku.trim().takeIf { it.isNotEmpty() } ?: "nome:${p.nome.lowercase()}"
    private fun links(context: Context): JSONObject = try {
        JSONObject(prefs(context).getString(KEY_LINKS, "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }
    fun lojaDeProduto(context: Context, p: AdminProduto): String =
        links(context).optString(chaveProduto(p), LOJA_PRINCIPAL_ID)

    fun vincular(context: Context, p: AdminProduto, lojaId: String) {
        require(lojas(context).any { it.id == lojaId }) { "Loja não encontrada." }
        val obj = links(context).apply { put(chaveProduto(p), lojaId) }
        check(prefs(context).edit().putString(KEY_LINKS, obj.toString()).commit()) { "Falha ao salvar vínculo." }
    }

    fun produtosDisponiveis(context: Context): List<Pair<AdminProduto, LojaParceira>> {
        val byId = lojas(context).filter { it.ativa }.associateBy { it.id }
        return AdminLocalStore.products(context)
            .filter { it.ativo && it.anuncioAtivo && it.estoque > 0 }
            .mapNotNull { p -> byId[lojaDeProduto(context, p)]?.let { p to it } }
    }

    fun distanciaKm(latitude: Double, longitude: Double, loja: LojaParceira): Double? {
        if (loja.latitude == null || loja.longitude == null) return null
        val result = FloatArray(1)
        Location.distanceBetween(latitude, longitude, loja.latitude, loja.longitude, result)
        return result[0] / 1000.0
    }

    /** Somente URLs HTTPS para não enviar credenciais via conexão insegura. */
    fun urlBackend(context: Context) = prefs(context).getString(KEY_API, "").orEmpty()
    fun salvarUrlBackend(context: Context, url: String) {
        val cleaned = url.trim().trimEnd('/')
        require(cleaned.isEmpty() || cleaned.startsWith("https://", true)) { "Use uma URL HTTPS." }
        require(cleaned.isEmpty() || android.net.Uri.parse(cleaned).host?.isNotBlank() == true) { "URL inválida." }
        prefs(context).edit().putString(KEY_API, cleaned).apply()
    }
}
