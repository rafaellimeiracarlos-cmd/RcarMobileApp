package com.example.rcarpremium

import java.text.Normalizer
import java.util.Locale

/** Apenas valida formato; uma placa NÃO identifica automaticamente o veículo. */
object PlateRules {
    private val mercosul = Regex("^[A-Z]{3}[0-9][A-Z][0-9]{2}$")
    private val antigo = Regex("^[A-Z]{3}[0-9]{4}$")

    fun normalize(input: String): String = input.trim().uppercase(Locale.ROOT)
        .replace("-", "").replace(" ", "")

    fun isValid(input: String): Boolean {
        val plate = normalize(input)
        return mercosul.matches(plate) || antigo.matches(plate)
    }

    fun patternName(input: String): String = if (mercosul.matches(normalize(input))) "Mercosul" else "Padrão antigo"
}

data class VehicleSpec(val brand: String, val model: String, val year: Int, val engine: String) {
    fun isValid(): Boolean = brand.isNotBlank() && model.isNotBlank() && year in 1900..2100 && engine.isNotBlank()
    fun description(): String = "$brand $model • $year • $engine"
}

data class VehicleFitment(
    val productKey: String,
    val brand: String,
    val model: String,
    val fromYear: Int,
    val toYear: Int,
    val engine: String
) {
    fun matches(vehicle: VehicleSpec): Boolean =
        tidy(brand) == tidy(vehicle.brand) && tidy(model) == tidy(vehicle.model) &&
            vehicle.year in fromYear..toYear && tidy(engine) == tidy(vehicle.engine)
}

private fun tidy(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
