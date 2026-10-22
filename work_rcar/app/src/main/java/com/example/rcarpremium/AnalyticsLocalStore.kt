package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Analítica de funil de compra para a versão local/offline do RCarPremium.
 * Um carrinho é considerado abandonado após 30 minutos sem atividade.
 * Para análise simultânea de clientes em dispositivos diferentes, os dados devem ir para um backend.
 */
data class CartJourney(
    val id: String,
    val email: String,
    val startedAt: Long,
    val lastActivityAt: Long,
    val amount: Double,
    val itemCount: Int,
    val checkoutAt: Long = 0L,
    val purchasedAt: Long = 0L,
    val abandonedAt: Long = 0L
) {
    fun status(): String = when {
        purchasedAt > 0L -> "Compra concluída"
        abandonedAt > 0L -> "Abandonado"
        checkoutAt > 0L -> "Checkout iniciado"
        else -> "Carrinho ativo"
    }
}

data class CartAnalyticsSnapshot(
    val startedToday: Int,
    val checkoutToday: Int,
    val purchasedToday: Int,
    val abandonedToday: Int,
    val activeNow: Int,
    val conversionPercent: Int,
    val checkoutFromTodayStarts: Int,
    val purchasedFromTodayStarts: Int,
    val abandonedFromTodayStarts: Int
)

data class FunnelTimeline(
    val labels: List<String>,
    val carts: List<Int>,
    val purchases: List<Int>,
    val abandoned: List<Int>
)

object AnalyticsLocalStore {
    const val ABANDON_TIMEOUT_MS = 30L * 60L * 1000L
    private const val PREFS_NAME = "rcar_checkout_analytics"
    private const val KEY_JOURNEYS = "cart_journeys"
    private const val KEY_ACTIVE_ID = "active_cart_journey"
    private const val MAX_JOURNEYS = 1000

    /** Chamado quando o conteúdo do carrinho é salvo ou atualizado. */
    fun recordCartState(context: Context, items: List<ProdutoItem>) {
        if (items.isEmpty()) return
        synchronized(this) {
            val prefs = preferences(context)
            val journeys = readJourneys(context).toMutableList()
            val activeId = prefs.getString(KEY_ACTIVE_ID, null)
            val current = journeys.firstOrNull { it.id == activeId }
            val now = System.currentTimeMillis()
            val total = items.sumOf { it.preco * it.quantidade }.coerceAtLeast(0.0)
            val quantity = items.sumOf { it.quantidade }.coerceAtLeast(0)

            val updated = if (current == null || current.purchasedAt > 0L || current.abandonedAt > 0L) {
                CartJourney(
                    id = UUID.randomUUID().toString(),
                    email = RcarLocalStore.currentEmail(context)?.trim()?.lowercase(Locale.ROOT)
                        ?.takeIf { it.isNotBlank() } ?: "cliente_sem_login",
                    startedAt = now,
                    lastActivityAt = now,
                    amount = total,
                    itemCount = quantity
                )
            } else {
                current.copy(
                    lastActivityAt = now,
                    amount = total,
                    itemCount = quantity
                )
            }

            if (current == null || current.id != updated.id) journeys.add(updated)
            else replaceJourney(journeys, updated)
            writeJourneys(context, journeys)
            prefs.edit().putString(KEY_ACTIVE_ID, updated.id).apply()
        }
    }

    fun markCheckoutStarted(context: Context, items: List<ProdutoItem>) {
        if (items.isEmpty()) return
        recordCartState(context, items)
        synchronized(this) {
            val prefs = preferences(context)
            val id = prefs.getString(KEY_ACTIVE_ID, null) ?: return
            val journeys = readJourneys(context).toMutableList()
            val current = journeys.firstOrNull { it.id == id } ?: return
            if (current.purchasedAt > 0L || current.abandonedAt > 0L) return
            replaceJourney(journeys, current.copy(checkoutAt = current.checkoutAt.takeIf { it > 0L } ?: System.currentTimeMillis()))
            writeJourneys(context, journeys)
        }
    }

    /** Marca o funil como convertido somente depois de a venda local ser confirmada. */
    fun markPurchase(context: Context, items: List<ProdutoItem>, total: Double) {
        if (items.isEmpty()) return
        runCatching {
            recordCartState(context, items)
            synchronized(this) {
                val prefs = preferences(context)
                val id = prefs.getString(KEY_ACTIVE_ID, null) ?: return@synchronized
                val journeys = readJourneys(context).toMutableList()
                val current = journeys.firstOrNull { it.id == id } ?: return@synchronized
                val now = System.currentTimeMillis()
                replaceJourney(
                    journeys,
                    current.copy(
                        lastActivityAt = now,
                        amount = total.coerceAtLeast(0.0),
                        itemCount = items.sumOf { it.quantidade }.coerceAtLeast(0),
                        purchasedAt = now,
                        abandonedAt = 0L
                    )
                )
                writeJourneys(context, journeys)
                prefs.edit().remove(KEY_ACTIVE_ID).apply()
            }
        }
    }

    fun snapshot(context: Context): CartAnalyticsSnapshot {
        val journeys = syncAbandoned(context)
        val startToday = startOfToday()
        val endToday = startToday + 24L * 60L * 60L * 1000L
        val started = journeys.filter { it.startedAt in startToday until endToday }
        val purchasedFromToday = started.count { it.purchasedAt > 0L }
        return CartAnalyticsSnapshot(
            startedToday = started.size,
            checkoutToday = journeys.count { it.checkoutAt in startToday until endToday },
            purchasedToday = journeys.count { it.purchasedAt in startToday until endToday },
            abandonedToday = journeys.count { it.abandonedAt in startToday until endToday },
            activeNow = journeys.count { it.purchasedAt == 0L && it.abandonedAt == 0L },
            conversionPercent = if (started.isEmpty()) 0 else ((purchasedFromToday.toDouble() / started.size) * 100.0).toInt().coerceIn(0, 100),
            checkoutFromTodayStarts = started.count { it.checkoutAt > 0L },
            purchasedFromTodayStarts = purchasedFromToday,
            abandonedFromTodayStarts = started.count { it.abandonedAt > 0L }
        )
    }

    fun hourlyTimeline(context: Context): FunnelTimeline {
        val journeys = syncAbandoned(context)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.HOUR_OF_DAY, -23)
        val firstHour = calendar.timeInMillis
        val labels = (0 until 24).map { offset ->
            val c = Calendar.getInstance().apply { timeInMillis = firstHour; add(Calendar.HOUR_OF_DAY, offset) }
            SimpleDateFormat("HH", Locale("pt", "BR")).format(Date(c.timeInMillis))
        }
        val carts = MutableList(24) { 0 }
        val purchases = MutableList(24) { 0 }
        val abandons = MutableList(24) { 0 }
        fun indexOf(timestamp: Long): Int {
            if (timestamp < firstHour) return -1
            val index = ((timestamp - firstHour) / (60L * 60L * 1000L)).toInt()
            return index.takeIf { it in 0..23 } ?: -1
        }
        journeys.forEach { journey ->
            indexOf(journey.startedAt).takeIf { it >= 0 }?.let { carts[it]++ }
            indexOf(journey.purchasedAt).takeIf { it >= 0 && journey.purchasedAt > 0L }?.let { purchases[it]++ }
            indexOf(journey.abandonedAt).takeIf { it >= 0 && journey.abandonedAt > 0L }?.let { abandons[it]++ }
        }
        return FunnelTimeline(labels, carts, purchases, abandons)
    }

    fun recentJourneys(context: Context, limit: Int = 8): List<CartJourney> =
        syncAbandoned(context).sortedByDescending { it.lastActivityAt }.take(limit.coerceAtLeast(0))

    fun journeyStatus(journey: CartJourney): String = journey.status()

    private fun syncAbandoned(context: Context): List<CartJourney> = synchronized(this) {
        val now = System.currentTimeMillis()
        val journeys = readJourneys(context).toMutableList()
        var changed = false
        for (i in journeys.indices) {
            val item = journeys[i]
            if (item.purchasedAt == 0L && item.abandonedAt == 0L && now - item.lastActivityAt >= ABANDON_TIMEOUT_MS) {
                journeys[i] = item.copy(abandonedAt = item.lastActivityAt + ABANDON_TIMEOUT_MS)
                if (preferences(context).getString(KEY_ACTIVE_ID, null) == item.id) {
                    preferences(context).edit().remove(KEY_ACTIVE_ID).apply()
                }
                changed = true
            }
        }
        if (changed) writeJourneys(context, journeys)
        journeys
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun replaceJourney(items: MutableList<CartJourney>, updated: CartJourney) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index >= 0) items[index] = updated else items.add(updated)
    }

    private fun readJourneys(context: Context): List<CartJourney> {
        val raw = preferences(context).getString(KEY_JOURNEYS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                CartJourney(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    email = obj.optString("email", "cliente_sem_login"),
                    startedAt = obj.optLong("startedAt", 0L),
                    lastActivityAt = obj.optLong("lastActivityAt", 0L),
                    amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    itemCount = obj.optInt("itemCount", 0).coerceAtLeast(0),
                    checkoutAt = obj.optLong("checkoutAt", 0L),
                    purchasedAt = obj.optLong("purchasedAt", 0L),
                    abandonedAt = obj.optLong("abandonedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeJourneys(context: Context, source: List<CartJourney>) {
        val journeys = source.sortedBy { it.startedAt }.takeLast(MAX_JOURNEYS)
        val array = JSONArray()
        journeys.forEach { journey ->
            array.put(JSONObject().apply {
                put("id", journey.id)
                put("email", journey.email)
                put("startedAt", journey.startedAt)
                put("lastActivityAt", journey.lastActivityAt)
                put("amount", journey.amount)
                put("itemCount", journey.itemCount)
                put("checkoutAt", journey.checkoutAt)
                put("purchasedAt", journey.purchasedAt)
                put("abandonedAt", journey.abandonedAt)
            })
        }
        preferences(context).edit().putString(KEY_JOURNEYS, array.toString()).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
