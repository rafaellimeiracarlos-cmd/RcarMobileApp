package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import java.util.Locale

/** Nova vitrine multi-loja. Usa o estoque real cadastrado no aparelho (modo offline). */
class MainActivityMarketplace : RcarBaseActivity() {
    private lateinit var list: LinearLayout
    private lateinit var counter: TextView
    private lateinit var online: LinearLayout
    private lateinit var search: EditText
    private var filtroLoja = ""
    private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (frame, body) = MarketplaceUi.page(this, "MARKETPLACE RCARPREMIUM",
            "Autopeças de várias lojas, um só lugar. Catálogo local em demonstração.",
            "INÍCIO DA LOJA", { finish() }, {
                startActivity(Intent(this, MainActivitycomprasonline::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })
        body.addView(MarketplaceUi.text(this, "Explore por loja, categoria e proximidade", 15f, Color.WHITE, true), MarketplaceUi.lp(this, 18))
        search = EditText(this).apply {
            hint = "Buscar peças, marcas ou categorias"; setSingleLine(true); textSize = 15f
            setTextColor(Color.WHITE); setHintTextColor(MarketplaceUi.MUTED)
            setPadding(MarketplaceUi.dp(this@MainActivityMarketplace, 14), 0, MarketplaceUi.dp(this@MainActivityMarketplace, 14), 0)
            background = MarketplaceUi.shape(this@MainActivityMarketplace)
        }
        body.addView(search, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this, 54)).apply { topMargin = MarketplaceUi.dp(this@MainActivityMarketplace, 12) })
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderProducts() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val first = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val filter = MarketplaceUi.button(this, "▦ FILTRAR LOJA").apply { setOnClickListener { escolherLoja() } }
        val location = MarketplaceUi.button(this, "⌖ PERTO DE MIM", true).apply {
            setOnClickListener { startActivity(Intent(this@MainActivityMarketplace, MainActivityLocalizacaoMarketplace::class.java)) }
        }
        first.addView(filter, LinearLayout.LayoutParams(0, MarketplaceUi.dp(this, 46), 1f))
        first.addView(location, LinearLayout.LayoutParams(0, MarketplaceUi.dp(this, 46), 1f).apply { leftMargin=MarketplaceUi.dp(this@MainActivityMarketplace, 8) })
        shortcuts.addView(first)
        val second = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        second.addView(MarketplaceUi.button(this, "✦ ASSISTENTE DE COMPRA").apply {
            setOnClickListener { startActivity(Intent(this@MainActivityMarketplace, MainActivityAssistenteCompra::class.java)) }
        }, LinearLayout.LayoutParams(0, MarketplaceUi.dp(this, 46), 1f))
        val current = RcarLocalStore.currentEmail(this)
        if (!current.isNullOrBlank() && UsuarioLocalRepository(this).isCeo(current)) {
            second.addView(MarketplaceUi.button(this, "♙ GERENCIAR LOJAS").apply {
                setOnClickListener { startActivity(Intent(this@MainActivityMarketplace, MainActivityLojasParceiras::class.java)) }
            }, LinearLayout.LayoutParams(0, MarketplaceUi.dp(this, 46), 1f).apply { leftMargin=MarketplaceUi.dp(this@MainActivityMarketplace, 8) })
        }
        shortcuts.addView(second, MarketplaceUi.lp(this, 8))
        shortcuts.addView(MarketplaceUi.button(this, "⌕  BUSCAR PEÇAS PELA PLACA", true).apply {
            setOnClickListener { startActivity(Intent(this@MainActivityMarketplace, MainActivityBuscaPlaca::class.java)) }
        }, MarketplaceUi.lp(this, 8))
        body.addView(shortcuts, MarketplaceUi.lp(this, 14))
        counter = MarketplaceUi.text(this, "", 14f, MarketplaceUi.MUTED, true)
        body.addView(counter, MarketplaceUi.lp(this, 22))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(list, MarketplaceUi.lp(this, 10))
        body.addView(MarketplaceUi.text(this, "CATÁLOGO REMOTO", 17f, MarketplaceUi.ORANGE, true), MarketplaceUi.lp(this, 22))
        body.addView(MarketplaceUi.button(this, "↓ ATUALIZAR CATÁLOGO DO SERVIDOR").apply {
            setOnClickListener { carregarOnline() }
        }, MarketplaceUi.lp(this, 8))
        online = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(online, MarketplaceUi.lp(this, 8))
        body.addView(MarketplaceUi.text(this, "Pagamentos e pedidos seguem no fluxo atual da RCarPremium. Vendas entre aparelhos exigem backend publicado.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 18))
        setContentView(frame)
        renderProducts()
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) renderProducts() }

    private fun escolherLoja() {
        val lojas = MarketplaceStore.lojas(this).filter { it.ativa }
        val labels = listOf("Todas as lojas") + lojas.map { it.nome + " • " + it.cidade }
        AlertDialog.Builder(this).setTitle("Selecione uma loja").setItems(labels.toTypedArray()) { _, n ->
            filtroLoja = if (n == 0) "" else lojas[n - 1].id
            renderProducts()
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun renderProducts() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val term = search.text.toString().trim()
        val products = MarketplaceStore.produtosDisponiveis(this).filter { (p, loja) ->
            (filtroLoja.isBlank() || loja.id == filtroLoja) &&
            (term.isBlank() || p.nome.contains(term, true) || p.categoria.contains(term, true) ||
                p.marca.contains(term, true) || loja.nome.contains(term, true))
        }
        counter.text = "${products.size} anúncios disponíveis • ${MarketplaceStore.lojas(this).count { it.ativa }} lojas ativas"
        if (products.isEmpty()) {
            list.addView(MarketplaceUi.card(this).apply {
                addView(MarketplaceUi.text(this@MainActivityMarketplace, "Nenhum anúncio encontrado.", 17f, Color.WHITE, true))
                addView(MarketplaceUi.text(this@MainActivityMarketplace, "Cadastre produtos, ative os anúncios ou altere os filtros.", 13f, MarketplaceUi.MUTED), MarketplaceUi.lp(this@MainActivityMarketplace, 7))
            }, MarketplaceUi.lp(this, 6))
            return
        }
        products.forEach { (p, loja) ->
            val card = MarketplaceUi.card(this)
            card.addView(MarketplaceUi.text(this, "VENDEDOR  •  ${loja.nome}", 11f, MarketplaceUi.ORANGE, true))
            card.addView(MarketplaceUi.text(this, p.nome, 18f, Color.WHITE, true), MarketplaceUi.lp(this, 8))
            card.addView(MarketplaceUi.text(this, "${p.categoria}   •   ${p.marca.ifBlank { "Peça automotiva" }}", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 4))
            card.addView(MarketplaceUi.text(this, "${loja.cidade}, ${loja.uf}   •   Estoque: ${p.estoque}", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 5))
            val off = AdminLocalStore.offerPercent(this, p.nome)
            val price = AdminLocalStore.promotionalPrice(this, p)
            card.addView(MarketplaceUi.text(this, "${moeda.format(price)}${if (off > 0) "    •    $off% OFF" else ""}", 20f, MarketplaceUi.ORANGE, true), MarketplaceUi.lp(this, 9))
            card.addView(MarketplaceUi.button(this, "VER PRODUTO  →", true).apply {
                setOnClickListener {
                    startActivity(Intent(this@MainActivityMarketplace, MainActivityVerProdutos::class.java).apply {
                        putExtra("PRODUTO_NOME", p.nome)
                        putExtra("PRODUTO_PRECO", price)
                    })
                }
            }, MarketplaceUi.lp(this, 8))
            list.addView(card, MarketplaceUi.lp(this, 9))
        }
    }
    private fun carregarOnline() {
        online.removeAllViews()
        online.addView(MarketplaceUi.text(this, "Consultando servidor HTTPS...", 13f, MarketplaceUi.MUTED))
        MarketplaceRemoteApi.catalog(this) { result ->
            online.removeAllViews()
            result.onFailure {
                online.addView(MarketplaceUi.text(this, "Catálogo online indisponível: ${it.message}", 13f, MarketplaceUi.MUTED))
            }.onSuccess { arr ->
                if (arr.length() == 0) online.addView(MarketplaceUi.text(this, "Nenhum produto foi publicado no servidor.", 13f, MarketplaceUi.MUTED))
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val card = MarketplaceUi.card(this)
                    card.addView(MarketplaceUi.text(this, "ONLINE • ${item.optString("vendor_name")}", 11f, MarketplaceUi.ORANGE, true))
                    card.addView(MarketplaceUi.text(this, item.optString("name"), 17f, Color.WHITE, true), MarketplaceUi.lp(this, 6))
                    card.addView(MarketplaceUi.text(this, item.optString("category") + "  •  " + item.optString("city") + "/" + item.optString("state"), 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 4))
                    card.addView(MarketplaceUi.text(this, moeda.format(item.optLong("price_cents") / 100.0), 20f, MarketplaceUi.ORANGE, true), MarketplaceUi.lp(this, 5))
                    card.addView(MarketplaceUi.text(this, "Visualização online: compra deste anúncio exige integração do checkout ao servidor.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 5))
                    online.addView(card, MarketplaceUi.lp(this, 8))
                }
            }
        }
    }

}
