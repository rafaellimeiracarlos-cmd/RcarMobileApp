package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

 data class SessaoAcesso(val id: String, val email: String, val papel: String, val inicio: Long, val fim: Long)

/** Registro local de acesso para auditoria demonstrativa. Não substitui auditoria em servidor. */
object SessaoLocalStore {
    private const val PREFS = "rcar_sessoes"
    private const val KEY_HISTORICO = "historico"
    private const val KEY_ATIVA = "sessao_ativa_"

    fun iniciar(context: Context, email: String, papel: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val normalized = email.trim().lowercase()
        encerrar(context, normalized)
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        p.edit().putString(KEY_ATIVA + normalized, JSONObject().put("id", id).put("email", normalized).put("papel", papel).put("inicio", now).put("fim", 0L).toString()).apply()
        val all = historico(context).toMutableList()
        all.add(SessaoAcesso(id, normalized, papel, now, 0L))
        salvarHistorico(context, all.takeLast(500))
    }

    fun encerrar(context: Context, email: String? = RcarLocalStore.currentEmail(context)) {
        if (email.isNullOrBlank()) return
        val normalized = email.trim().lowercase()
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val active = try { p.getString(KEY_ATIVA + normalized, null)?.let { JSONObject(it) } } catch (_: Exception) { null } ?: return
        val id = active.optString("id")
        val now = System.currentTimeMillis()
        salvarHistorico(context, historico(context).map { if (it.id == id && it.fim == 0L) it.copy(fim = now) else it })
        p.edit().remove(KEY_ATIVA + normalized).apply()
    }

    fun historico(context: Context): List<SessaoAcesso> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORICO, "[]") ?: "[]"
        return try { val a=JSONArray(raw); (0 until a.length()).map { i -> val o=a.getJSONObject(i); SessaoAcesso(o.optString("id"),o.optString("email"),o.optString("papel"),o.optLong("inicio"),o.optLong("fim")) }.sortedByDescending { it.inicio } } catch (_: Exception) { emptyList() }
    }

    fun duracaoFormatada(inicio: Long, fim: Long = 0L): String {
        val segundos = (((if (fim > 0) fim else System.currentTimeMillis()) - inicio).coerceAtLeast(0L) / 1000L)
        val h = segundos / 3600; val m = (segundos % 3600) / 60; val s = segundos % 60
        return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h,m,s) else String.format(Locale.getDefault(), "%02d:%02d", m,s)
    }

    fun dataFormatada(millis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date(millis))

    private fun salvarHistorico(context: Context, items: List<SessaoAcesso>) {
        val a=JSONArray(); items.forEach { a.put(JSONObject().put("id",it.id).put("email",it.email).put("papel",it.papel).put("inicio",it.inicio).put("fim",it.fim)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HISTORICO,a.toString()).apply()
    }
}
