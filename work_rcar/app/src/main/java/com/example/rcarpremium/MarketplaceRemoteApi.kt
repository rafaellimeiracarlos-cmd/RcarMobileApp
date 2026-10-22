package com.example.rcarpremium

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Transporte HTTP. A chave do provedor de IA NUNCA vai para o APK. */
object MarketplaceRemoteApi {
    // Token de sessão do backend só em memória; ao fechar processo, nova autenticação é exigida.
    private var token: String? = null
    private val main = Handler(Looper.getMainLooper())

    /** Catálogo multi-loja do servidor, apenas leitura na v3.0. */
    fun catalog(context: Context, done: (Result<JSONArray>) -> Unit) {
        val base = MarketplaceStore.urlBackend(context)
        if (!base.startsWith("https://")) {
            done(Result.failure(IllegalArgumentException("Configure o servidor HTTPS no painel CEO.")))
            return
        }
        Thread {
            val response = runCatching {
                val connection = URL(base.trimEnd('/') + "/v1/catalog").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"; connection.connectTimeout = 8000; connection.readTimeout = 12000
                    if (connection.responseCode != 200) throw IllegalStateException("HTTP ${connection.responseCode}")
                    JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                } finally { connection.disconnect() }
            }
            main.post { done(response) }
        }.start()
    }

    /** Dados básicos do veículo vindos exclusivamente do backend com provedor autorizado.
     * A API exige conta autenticada; não consulta dono, chassi nem dados pessoais.
     */
    fun vehicleByPlate(context: Context, plate: String, done: (Result<VehicleSpec>) -> Unit) {
        val safePlate = PlateRules.normalize(plate)
        if (!PlateRules.isValid(safePlate)) {
            done(Result.failure(IllegalArgumentException("Formato de placa inválido.")))
            return
        }
        val access = token
        if (access.isNullOrBlank()) {
            done(Result.failure(IllegalStateException("Conecte sua conta no servidor e configure um provedor de placas autorizado; ou informe o carro manualmente.")))
            return
        }
        val endpoint = MarketplaceStore.urlBackend(context)
        request(endpoint, "/v1/vehicles/lookup", JSONObject().put("plate", safePlate), access) { result ->
            done(result.map { o ->
                val vehicle = VehicleSpec(o.optString("brand"), o.optString("model"), o.optInt("year"), o.optString("engine"))
                if (!vehicle.isValid()) throw IllegalStateException("O serviço não retornou marca, modelo, ano e motor completos; confirme manualmente.")
                vehicle
            })
        }
    }

    fun login(context: Context, email: String, password: String, done: (Result<String>) -> Unit) {
        val endpoint = MarketplaceStore.urlBackend(context)
        request(endpoint, "/v1/auth/login", JSONObject().put("email", email).put("password", password), null) { result ->
            result.onSuccess { token = it.optString("access_token").takeIf(String::isNotBlank) }
            done(result.map { if (token == null) "Falha: token ausente" else "Autenticado no servidor" })
        }
    }
    fun chat(context: Context, question: String, done: (Result<String>) -> Unit) {
        val endpoint = MarketplaceStore.urlBackend(context)
        val access = token
        if (access.isNullOrBlank()) { done(Result.failure(IllegalStateException("Entre na conta do servidor de IA primeiro."))); return }
        request(endpoint, "/v1/assistant", JSONObject().put("question", question), access) { result ->
            done(result.map { it.optString("answer", "Sem resposta.") })
        }
    }
    private fun request(base: String, path: String, body: JSONObject, access: String?, done: (Result<JSONObject>) -> Unit) {
        if (!base.startsWith("https://")) { done(Result.failure(IllegalArgumentException("Configure uma URL HTTPS no painel CEO."))); return }
        Thread {
            val outcome = runCatching {
                val connection = URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"; connection.connectTimeout = 8000; connection.readTimeout = 20000
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.setRequestProperty("Accept", "application/json")
                    if (!access.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $access")
                    connection.doOutput = true
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) throw IllegalStateException("Servidor HTTP $code: ${text.take(150)}")
                    JSONObject(text)
                } finally { connection.disconnect() }
            }
            main.post { done(outcome) }
        }.start()
    }
}
