package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Visão financeira do CEO. Lucro calculado com linhas de venda que possuem custo registrado. */
class MainActivityMargemLucro : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!UsuarioLocalRepository(this).isCeo(RcarLocalStore.currentEmail(this).orEmpty())) {
            Toast.makeText(this, "Apenas o CEO pode consultar margens e resultados financeiros.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        render()
    }
    override fun onResume() { super.onResume(); if (RcarLocalStore.currentEmail(this)?.let { UsuarioLocalRepository(this).isCeo(it) } == true) render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)); setBackgroundColor(0xFF090909.toInt()) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(button("←  VOLTAR AO PAINEL", false).apply { setOnClickListener { finish() } }, lp())
        content.addView(text("MARGEM DE LUCRO & VENDAS", 22f, orange, true), lp(14))
        content.addView(text("Acompanhe faturamento, custo dos produtos vendidos e margem bruta estimada.", 13f, Color.LTGRAY), lp(5))

        val detailed = AdminLocalStore.vendasDetalhadas(this, 5000)
        val revenueDetailed = detailed.sumOf { it.precoUnitario * it.quantidade }
        val withKnownCost = detailed.filter { it.custoUnitario > 0.0 }
        val knownCostRevenue = withKnownCost.sumOf { it.precoUnitario * it.quantidade }
        val costDetailed = withKnownCost.sumOf { it.custoUnitario * it.quantidade }
        val hasCost = withKnownCost.isNotEmpty()
        val grossProfit = withKnownCost.sumOf { (it.precoUnitario - it.custoUnitario) * it.quantidade }
        val margin = if (knownCostRevenue > 0.0) grossProfit / knownCostRevenue * 100.0 else 0.0
        val today = AdminLocalStore.salesToday(this)
        val todayCount = AdminLocalStore.salesTodayCount(this)
        content.addView(text("RESUMO DO NEGÓCIO", 11f, Color.LTGRAY, true), lp(18))
        val stats = card()
        stats.addView(metric("FATURAMENTO REGISTRADO", money(AdminLocalStore.totalSales(this)), "Acumulado de todas as vendas registradas"))
        stats.addView(separator(), lp(8))
        stats.addView(metric("VENDAS HOJE", money(today), "$todayCount venda(s) registrada(s) hoje"))
        stats.addView(separator(), lp(8))
        stats.addView(metric("FATURAMENTO COM ITENS DETALHADOS", money(revenueDetailed), "Linhas de venda com produto e quantidade"))
        stats.addView(separator(), lp(8))
        stats.addView(metric("CUSTO IDENTIFICADO NAS VENDAS", if (hasCost) money(costDetailed) else "Custo não informado", "Somente vendas com custo unitário informado"))
        stats.addView(separator(), lp(8))
        stats.addView(metric("LUCRO BRUTO ESTIMADO", if (hasCost) money(grossProfit) else "—", if (hasCost) "Margem sobre vendas com custo conhecido: ${String.format(Locale("pt", "BR"), "%.1f", margin)}%" else "Preencha o custo de compra dos produtos para calcular a margem."))
        content.addView(stats, lp(8))

        content.addView(text("MARGEM POR PRODUTO", 20f, orange, true), lp(24))
        content.addView(text("Estimativa unitária com base no custo e no preço atualmente cadastrados.", 12f, Color.LTGRAY), lp(4))
        AdminLocalStore.products(this).sortedByDescending { if (it.preco > 0.0) (it.preco - it.precoCusto) / it.preco else 0.0 }.forEach { product ->
            val profit = product.preco - product.precoCusto
            val percent = if (product.preco > 0 && product.precoCusto > 0) profit / product.preco * 100.0 else null
            val pCard = card()
            pCard.addView(text(product.nome, 15f, Color.WHITE, true))
            pCard.addView(text("Venda: ${money(product.preco)}   •   Custo: ${if (product.precoCusto > 0.0) money(product.precoCusto) else "não informado"}", 12f, Color.LTGRAY), lp(5))
            val marginText = if (percent == null) "Informe o custo para calcular a margem" else "Lucro bruto unitário estimado: ${money(profit)}  •  Margem: ${String.format(Locale("pt", "BR"), "%.1f", percent)}%"
            pCard.addView(text(marginText, 12f, if (percent != null && percent >= 0.0) orange else 0xFFFFB45C.toInt(), true), lp(5))
            pCard.addView(text("Estoque: ${product.estoque}  •  Anúncio: ${if (product.anuncioAtivo) "ativo" else "pausado"}", 11f, Color.GRAY), lp(4))
            content.addView(pCard, lp(9))
        }

        content.addView(text("VENDAS RECENTES DETALHADAS", 20f, orange, true), lp(24))
        if (detailed.isEmpty()) content.addView(infoCard("Nenhuma venda detalhada ainda.", "As vendas registradas pela equipe e as compras concluídas pelo checkout passarão a aparecer aqui."), lp(8))
        detailed.take(30).forEach { sale ->
            val value = sale.precoUnitario * sale.quantidade
            val cost = sale.custoUnitario * sale.quantidade
            val profitLine = value - cost
            val timestamp = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(sale.timestamp))
            val row = card()
            row.addView(text(sale.nomeProduto, 14f, Color.WHITE, true))
            row.addView(text("${sale.quantidade} un. × ${money(sale.precoUnitario)} = ${money(value)}", 12f, Color.LTGRAY), lp(4))
            row.addView(text("Custo: ${if (sale.custoUnitario > 0.0) money(cost) else "não informado"}  •  Lucro bruto estimado: ${if (sale.custoUnitario > 0.0) money(profitLine) else "não calculado"}", 12f, orange), lp(3))
            row.addView(text("${sale.responsavel.ifBlank { "Responsável não informado" }}  •  $timestamp", 10f, Color.GRAY), lp(4))
            content.addView(row, lp(8))
        }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun metric(label: String, value: String, detail: String): LinearLayout {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(text(label, 10f, Color.LTGRAY, true))
        layout.addView(text(value, 22f, Color.WHITE, true), lp(3))
        layout.addView(text(detail, 11f, Color.GRAY), lp(2))
        return layout
    }
    private fun separator() = android.view.View(this).apply { setBackgroundColor(0xFF333333.toInt()); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)) }
    private fun infoCard(title: String, desc: String) = card().apply { addView(text(title, 14f, Color.WHITE, true)); addView(text(desc, 12f, Color.LTGRAY), lp(5)) }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(13), dp(14), dp(13)); background = GradientDrawable().apply { setColor(0xFF191919.toInt()); cornerRadius = dp(15).toFloat(); setStroke(dp(1), 0xFF4A2A10.toInt()) } }
    private fun text(v: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply { text = v; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun button(v: String, primary: Boolean) = android.widget.Button(this).apply { text = v; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE); background = GradientDrawable().apply { setColor(if (primary) orange else 0xFF292929.toInt()); cornerRadius = dp(13).toFloat(); setStroke(dp(1), if (primary) orange else 0xFF555555.toInt()) } }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (top > 0) topMargin = dp(top) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Double) = String.format(Locale("pt", "BR"), "R$ %.2f", v)
}
