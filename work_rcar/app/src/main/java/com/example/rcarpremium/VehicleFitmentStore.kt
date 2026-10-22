package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** Associações cadastradas e confirmadas pelo CEO, não inferidas por IA nem por nomes das peças. */
object VehicleFitmentStore {
    private const val PREFS = "rcar_vehicle_fitment_v1"
    private const val KEY_FITMENTS = "fitments"
    private const val KEY_PLATES = "plate_to_spec_local"
    private const val KEY_SALT = "plate_salt"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun productKey(product: AdminProduto): String = MarketplaceStore.chaveProduto(product)

    fun all(context: Context): List<VehicleFitment> {
        val arr = try { JSONArray(prefs(context).getString(KEY_FITMENTS, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val value = VehicleFitment(obj.optString("key"), obj.optString("brand"),
                obj.optString("model"), obj.optInt("from"), obj.optInt("to"), obj.optString("engine"))
            value.takeIf { it.productKey.isNotBlank() && it.fromYear in 1900..2100 && it.toYear in it.fromYear..2100 }
        }
    }

    fun forProduct(context: Context, product: AdminProduto): List<VehicleFitment> =
        all(context).filter { it.productKey == productKey(product) }

    fun add(context: Context, fitment: VehicleFitment) {
        require(fitment.productKey.isNotBlank()) { "Escolha um produto." }
        require(fitment.brand.isNotBlank() && fitment.model.isNotBlank() && fitment.engine.isNotBlank()) { "Preencha marca, modelo e motor." }
        require(fitment.fromYear in 1900..2100 && fitment.toYear in fitment.fromYear..2100) { "Informe uma faixa de anos válida." }
        val list = all(context).toMutableList()
        require(fitment !in list) { "Esta compatibilidade já existe." }
        list.add(fitment)
        save(context, list)
    }

    fun remove(context: Context, fitment: VehicleFitment) = save(context, all(context).filterNot { it == fitment })

    private fun save(context: Context, fits: List<VehicleFitment>) {
        val array = JSONArray()
        fits.forEach { f -> array.put(JSONObject().apply {
            put("key", f.productKey); put("brand", f.brand.trim()); put("model", f.model.trim())
            put("from", f.fromYear); put("to", f.toYear); put("engine", f.engine.trim())
        }) }
        check(prefs(context).edit().putString(KEY_FITMENTS, array.toString()).commit()) { "Não foi possível salvar compatibilidades." }
    }

    fun compatible(context: Context, vehicle: VehicleSpec): List<AdminProduto> {
        if (!vehicle.isValid()) return emptyList()
        val fits = all(context)
        return AdminLocalStore.products(context).filter { p ->
            p.ativo && p.anuncioAtivo && p.estoque > 0 && fits.any { it.productKey == productKey(p) && it.matches(vehicle) }
        }
    }

    // Placas NÃO são gravadas em texto puro. Apenas um hash com sal local para histórico opt-in.
    private fun plateHash(context: Context, plate: String): String {
        val p = prefs(context)
        var salt = p.getString(KEY_SALT, null)
        if (salt == null) {
            salt = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            check(p.edit().putString(KEY_SALT, salt).commit()) { "Não foi possível inicializar o armazenamento." }
        }
        val data = (salt + ":" + PlateRules.normalize(plate)).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    }

    fun remembered(context: Context, plate: String): VehicleSpec? {
        if (!PlateRules.isValid(plate)) return null
        val all = try { JSONObject(prefs(context).getString(KEY_PLATES, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        val item = all.optJSONObject(plateHash(context, plate)) ?: return null
        return VehicleSpec(item.optString("brand"), item.optString("model"), item.optInt("year"), item.optString("engine"))
            .takeIf { it.isValid() }
    }

    fun remember(context: Context, plate: String, vehicle: VehicleSpec) {
        require(PlateRules.isValid(plate) && vehicle.isValid()) { "Placa ou dados de veículo inválidos." }
        val all = try { JSONObject(prefs(context).getString(KEY_PLATES, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        all.put(plateHash(context, plate), JSONObject().apply {
            put("brand", vehicle.brand.trim()); put("model", vehicle.model.trim())
            put("year", vehicle.year); put("engine", vehicle.engine.trim())
        })
        check(prefs(context).edit().putString(KEY_PLATES, all.toString()).commit()) { "Não foi possível salvar o veículo." }
    }

    fun forget(context: Context, plate: String) {
        if (!PlateRules.isValid(plate)) return
        val all = try { JSONObject(prefs(context).getString(KEY_PLATES, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        all.remove(plateHash(context, plate))
        prefs(context).edit().putString(KEY_PLATES, all.toString()).apply()
    }
}
