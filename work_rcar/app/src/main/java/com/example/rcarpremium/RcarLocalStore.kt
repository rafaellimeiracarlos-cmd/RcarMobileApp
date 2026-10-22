package com.example.rcarpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RcarLocalStore {
    private const val PREFS = "rcarpremium_app"
    private const val CART = "cart"  // chave legada usada somente na migração
    private const val CART_PREFIX = "cart_user_"

    private fun cartKey(context: Context): String? = currentEmail(context)
        ?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { CART_PREFIX + it }

    private fun cartForCurrentUser(context: Context): String? {
        val key = cartKey(context) ?: return null
        val data = prefs(context)
        // Migra o carrinho das versões anteriores uma vez, evitando compartilhá-lo entre contas.
        if (!data.contains(key) && data.contains(CART)) {
            data.edit().putString(key, data.getString(CART, "[]")).remove(CART).apply()
        }
        return key
    }
    private const val EMAIL = "session_email"

    fun login(context: Context, email: String) {
        // A simulação nunca deve sobreviver a um novo login, ainda que seja o mesmo CEO.
        CompraTesteCeoStore.encerrar(context)
        prefs(context).edit().putString(EMAIL, email.trim().lowercase()).apply()
    }
    fun logout(context: Context) {
        CompraTesteCeoStore.encerrar(context)
        prefs(context).edit().remove(EMAIL).apply()
    }
    fun currentEmail(context: Context): String? = prefs(context).getString(EMAIL, null)

    fun cart(context: Context): MutableList<ProdutoItem> {
        if (CompraTesteCeoStore.ativo(context)) return CompraTesteCeoStore.carrinho(context)
        val key = cartForCurrentUser(context) ?: return mutableListOf()
        val raw = prefs(context).getString(key, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                ProdutoItem(o.getString("nome"), o.getDouble("preco"), o.getInt("qtd"), R.drawable.pecasautomotiva)
            }
        } catch (_: Exception) { mutableListOf() }
    }

    fun saveCart(context: Context, items: List<ProdutoItem>) {
        if (CompraTesteCeoStore.ativo(context)) {
            CompraTesteCeoStore.salvarCarrinho(context, items)
            return
        }
        val arr = JSONArray()
        items.forEach { p -> arr.put(JSONObject().apply { put("nome", p.nome); put("preco", p.preco); put("qtd", p.quantidade) }) }
        cartForCurrentUser(context)?.let { key -> prefs(context).edit().putString(key, arr.toString()).apply() }
        if (items.isNotEmpty()) AnalyticsLocalStore.recordCartState(context, items)
    }

    fun addToCart(context: Context, product: ProdutoItem) {
        if (CompraTesteCeoStore.ativo(context)) {
            CompraTesteCeoStore.adicionar(context, product)
            return
        }
        val items = cart(context)
        val existing = items.firstOrNull { it.nome == product.nome }
        if (existing != null) existing.quantidade += product.quantidade else items.add(product.copy())
        saveCart(context, items)
    }

    fun clearCart(context: Context) {
        if (CompraTesteCeoStore.ativo(context)) CompraTesteCeoStore.limparCarrinho(context)
        else cartForCurrentUser(context)?.let { key -> prefs(context).edit().remove(key).apply() }
    }
    fun total(context: Context) = cart(context).sumOf { it.preco * it.quantidade }
    fun count(context: Context) = cart(context).sumOf { it.quantidade }

    val catalog = listOf(
        ProdutoItem("Pastilha de Freio RCar", 150.00, 1, R.drawable.pecasautomotiva),
        ProdutoItem("Óleo Sintético 5W30", 45.90, 1, R.drawable.pecasautomotiva),
        ProdutoItem("Amortecedor Dianteiro Premium", 159.90, 1, R.drawable.pecasautomotiva),
        ProdutoItem("Kit Suspensão RCar", 349.90, 1, R.drawable.pecasautomotiva),
        ProdutoItem("Filtro de Óleo Premium", 39.90, 1, R.drawable.pecasautomotiva)
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
