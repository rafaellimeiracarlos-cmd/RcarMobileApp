package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivityInsightsCeo : RcarBaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val moeda: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    private val metricValues = linkedMapOf<String, TextView>()
    private lateinit var revenueChart: RcarAnalyticsChartView
    private lateinit var tvUpdated: TextView
    private lateinit var criticalContainer: LinearLayout
    private lateinit var recentSalesContainer: LinearLayout
    private lateinit var recentSessionsContainer: LinearLayout
    private lateinit var funnelSummaryContainer: LinearLayout
    private var criticalSignature = "__initial__"
    private var salesSignature = "__initial__"
    private var sessionsSignature = "__initial__"

    private val refreshTask = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                refreshDashboard()
                handler.postDelayed(this, 10_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = RcarLocalStore.currentEmail(this) ?: ""
        if (!UsuarioLocalRepository(this).isCeo(email)) {
            finish()
            return
        }
        buildDashboard()
        refreshDashboard()
        handler.postDelayed(refreshTask, 10_000L)
    }

    override fun onResume() {
        super.onResume()
        if (::revenueChart.isInitialized) refreshDashboard()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshTask)
        super.onDestroy()
    }

    private fun buildDashboard() {
        val scroll = ScrollView(this).apply { setBackgroundColor(BG); isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(30))
        }
        scroll.addView(content)
        setContentView(scroll)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = makeText("‹", 35f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar ao painel do CEO"
            setOnClickListener { finish() }
        }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(42), dp(46)))
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(makeText("INSIGHTS EXECUTIVOS", 19f, ORANGE, true))
        heading.addView(makeText("Visão geral da operação", 12f, MUTED))
        toolbar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(makeText("CEO", 10f, ORANGE, true).apply {
            background = rounded(Color.rgb(52, 31, 18), 40)
            setPadding(dp(11), dp(7), dp(11), dp(7))
        })
        content.addView(toolbar)

        tvUpdated = makeText("Dados locais • atualização automática", 11f, MUTED).apply {
            setPadding(dp(44), dp(3), 0, dp(12))
        }
        content.addView(tvUpdated)

        content.addView(sectionTitle("DESEMPENHO DA OPERAÇÃO", "Indicadores essenciais em um só lugar"))
        val metricRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val metrics = listOf(
            Triple("FATURAMENTO DE HOJE", "R$ 0,00", ORANGE),
            Triple("VENDAS CONFIRMADAS HOJE", "0", GREEN),
            Triple("TICKET MÉDIO DE HOJE", "R$ 0,00", ORANGE),
            Triple("FATURAMENTO ACUMULADO", "R$ 0,00", Color.WHITE),
            Triple("VALOR ESTIMADO DO ESTOQUE", "R$ 0,00", BLUE),
            Triple("PRODUTOS CADASTRADOS", "0", Color.WHITE),
            Triple("UNIDADES EM ESTOQUE", "0", Color.WHITE),
            Triple("ALERTAS DE ESTOQUE", "0", RED),
            Triple("USUÁRIOS CADASTRADOS", "0", Color.WHITE),
            Triple("FUNCIONÁRIOS ATIVOS", "0", GREEN),
            Triple("ACESSOS AINDA ABERTOS", "0", BLUE),
            Triple("CARRINHOS ATIVOS", "0", BLUE),
            Triple("CARRINHOS INICIADOS HOJE", "0", ORANGE),
            Triple("COMPRAS MONITORADAS HOJE", "0", GREEN),
            Triple("CARRINHOS ABANDONADOS", "0", RED),
            Triple("CONVERSÃO DO CARRINHO", "0%", GREEN)
        )
        metrics.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { index, item ->
                val card = metricCard(item.first, item.second, item.third)
                row.addView(card, LinearLayout.LayoutParams(0, dp(102), 1f).apply {
                    if (index == 0) rightMargin = dp(6) else leftMargin = dp(6)
                    bottomMargin = dp(10)
                })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(102), 1f))
            metricRows.addView(row)
        }
        content.addView(metricRows)

        content.addView(sectionTitle("FATURAMENTO • ÚLTIMOS 7 DIAS", "Comparativo diário das vendas confirmadas"))
        val chartCard = card(Color.rgb(22, 22, 25), 18).apply { setPadding(dp(13), dp(14), dp(13), dp(10)) }
        chartCard.addView(makeText("EVOLUÇÃO DE RECEITA", 12f, Color.WHITE, true))
        chartCard.addView(makeText("Valores registrados no histórico de vendas local", 11f, MUTED).apply {
            setPadding(0, dp(3), 0, 0)
        })
        revenueChart = RcarAnalyticsChartView(this)
        revenueChart.setRevenueData(emptyList(), emptyList())
        chartCard.addView(revenueChart, LinearLayout.LayoutParams(-1, dp(230)))
        content.addView(chartCard)

        content.addView(sectionTitle("FUNIL DE COMPRA", "Acompanhe o comportamento dos clientes"))
        funnelSummaryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(22, 22, 25), 18)
            setPadding(dp(15), dp(5), dp(15), dp(9))
        }
        content.addView(funnelSummaryContainer)

        content.addView(sectionTitle("ESTOQUE CRÍTICO", "Produtos que atingiram o estoque mínimo"))
        criticalContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(criticalContainer)

        content.addView(sectionTitle("VENDAS RECENTES", "Últimas compras registradas com sucesso"))
        recentSalesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(recentSalesContainer)

        content.addView(sectionTitle("ACESSOS RECENTES", "Auditoria de sessões • visível somente ao CEO"))
        recentSessionsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(recentSessionsContainer)


    }

    private fun metricCard(label: String, initialValue: String, accent: Int): LinearLayout {
        val card = card(Color.rgb(23, 23, 26), 16).apply { setPadding(dp(13), dp(11), dp(11), dp(10)) }
        card.addView(makeText(label, 9f, MUTED, true))
        val value = makeText(initialValue, 19f, accent, true).apply {
            tag = label
            setPadding(0, dp(10), 0, 0)
            maxLines = 1
            textSize = if (label.length > 22) 16f else 19f
        }
        card.addView(value)
        metricValues[label] = value
        return card
    }

    private fun refreshDashboard() {
        val products = AdminLocalStore.products(this)
        val users = UsuarioLocalRepository(this).listarUsuarios()
        val sessions = SessaoLocalStore.historico(this).filter { it.fim == 0L }
        val funnel = AnalyticsLocalStore.snapshot(this)
        val metrics = mapOf(
            "FATURAMENTO DE HOJE" to moeda.format(AdminLocalStore.salesToday(this)),
            "VENDAS CONFIRMADAS HOJE" to AdminLocalStore.salesTodayCount(this).toString(),
            "TICKET MÉDIO DE HOJE" to moeda.format(AdminLocalStore.averageSaleToday(this)),
            "FATURAMENTO ACUMULADO" to moeda.format(AdminLocalStore.totalSales(this)),
            "VALOR ESTIMADO DO ESTOQUE" to moeda.format(AdminLocalStore.inventoryValue(this)),
            "PRODUTOS CADASTRADOS" to products.size.toString(),
            "UNIDADES EM ESTOQUE" to products.sumOf { it.estoque }.toString(),
            "ALERTAS DE ESTOQUE" to products.count { it.estoque <= it.estoqueMinimo }.toString(),
            "USUÁRIOS CADASTRADOS" to users.size.toString(),
            "FUNCIONÁRIOS ATIVOS" to users.count { it.papel in UsuarioLocalRepository.PAPEIS_EQUIPE && it.ativo }.toString(),
            "ACESSOS AINDA ABERTOS" to sessions.size.toString(),
            "CARRINHOS ATIVOS" to funnel.activeNow.toString(),
            "CARRINHOS INICIADOS HOJE" to funnel.startedToday.toString(),
            "COMPRAS MONITORADAS HOJE" to funnel.purchasedToday.toString(),
            "CARRINHOS ABANDONADOS" to funnel.abandonedToday.toString(),
            "CONVERSÃO DO CARRINHO" to "${funnel.conversionPercent}%"
        )
        metrics.forEach { (label, value) -> metricValues[label]?.text = value }
        tvUpdated.text = "Atualizado às ${SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())} • dados locais"

        val daily = AdminLocalStore.dailySales(this, 7)
        revenueChart.setRevenueData(daily.map { it.label }, daily.map { it.total })
        renderFunnelSummary(funnel)

        val critical = products.filter { it.estoque <= it.estoqueMinimo }.sortedBy { it.estoque }
        val criticalKey = critical.joinToString("|") { "${it.nome}:${it.estoque}:${it.estoqueMinimo}:${it.preco}" }
        if (criticalKey != criticalSignature) {
            criticalSignature = criticalKey
            renderCriticalStock(critical)
        }

        val recentSales = AdminLocalStore.recentSales(this, 8)
        val salesKey = recentSales.joinToString("|") { "${it.timestamp}:${it.total}" }
        if (salesKey != salesSignature) {
            salesSignature = salesKey
            renderRecentSales(recentSales)
        }

        val recentSessions = SessaoLocalStore.historico(this).take(6)
        val tick = System.currentTimeMillis() / 10_000L
        val sessionKey = recentSessions.joinToString("|") { "${it.email}:${it.inicio}:${it.fim}:${if (it.fim == 0L) tick else 0L}" }
        if (sessionKey != sessionsSignature) {
            sessionsSignature = sessionKey
            renderRecentSessions(recentSessions)
        }
    }

    private fun renderFunnelSummary(snapshot: CartAnalyticsSnapshot) {
        funnelSummaryContainer.removeAllViews()
        addFunnelSummaryRow("Carrinhos iniciados hoje", snapshot.startedToday, ORANGE)
        addFunnelSummaryRow("Avançaram para o checkout", snapshot.checkoutFromTodayStarts, BLUE)
        addFunnelSummaryRow("Compras concluídas", snapshot.purchasedFromTodayStarts, GREEN)
        addFunnelSummaryRow("Abandonados após 30 min", snapshot.abandonedFromTodayStarts, RED)
        funnelSummaryContainer.addView(makeText(
            "O funil considera os carrinhos iniciados hoje e acompanha seus desfechos. Abandono estimado após 30 minutos sem atividade.",
            11f, MUTED
        ).apply { setPadding(0, dp(8), 0, dp(5)) })
    }

    private fun addFunnelSummaryRow(label: String, count: Int, color: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }
        val dot = View(this).apply { background = rounded(color, 50) }
        row.addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)))
        row.addView(makeText(label, 12f, Color.WHITE).apply { setPadding(dp(9), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(makeText(count.toString(), 14f, color, true))
        funnelSummaryContainer.addView(row)
    }

    private fun renderCriticalStock(products: List<AdminProduto>) {
        criticalContainer.removeAllViews()
        if (products.isEmpty()) {
            criticalContainer.addView(infoCard("Estoque saudável", "Nenhum produto atingiu o limite mínimo configurado.", GREEN))
            return
        }
        products.take(8).forEach { product ->
            criticalContainer.addView(infoCard(
                product.nome,
                "${product.estoque} unidade(s) disponíveis • mínimo ${product.estoqueMinimo} • ${moeda.format(product.preco * product.estoque)} em estoque",
                RED
            ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun renderRecentSales(sales: List<AdminVenda>) {
        recentSalesContainer.removeAllViews()
        if (sales.isEmpty()) {
            recentSalesContainer.addView(infoCard("Nenhuma venda registrada", "As vendas confirmadas aparecerão aqui automaticamente.", MUTED))
            return
        }
        sales.forEach { sale ->
            recentSalesContainer.addView(infoCard(
                moeda.format(sale.total),
                "Venda local confirmada • ${SessaoLocalStore.dataFormatada(sale.timestamp)}",
                GREEN
            ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun renderRecentSessions(sessions: List<SessaoAcesso>) {
        recentSessionsContainer.removeAllViews()
        if (sessions.isEmpty()) {
            recentSessionsContainer.addView(infoCard("Nenhum acesso registrado", "Os próximos acessos serão listados aqui.", MUTED))
            return
        }
        sessions.forEach { session ->
            recentSessionsContainer.addView(infoCard(
                "${session.papel} • ${session.email}",
                "Início: ${SessaoLocalStore.dataFormatada(session.inicio)} • Duração: ${SessaoLocalStore.duracaoFormatada(session.inicio, session.fim)}",
                if (session.fim == 0L) GREEN else MUTED
            ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun infoCard(title: String, subtitle: String, accent: Int): LinearLayout = card(Color.rgb(22, 22, 25), 14).apply {
        setPadding(dp(14), dp(12), dp(14), dp(12))
        addView(makeText(title, 13f, accent, true))
        addView(makeText(subtitle, 11f, MUTED).apply { setPadding(0, dp(5), 0, 0) })
    }

    private fun sectionTitle(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(18), dp(2), dp(10))
        addView(makeText(title, 13f, ORANGE, true))
        addView(makeText(subtitle, 11f, MUTED).apply { setPadding(0, dp(3), 0, 0) })
    }

    private fun card(color: Int, radius: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, radius)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun makeText(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = Color.rgb(10, 10, 11)
        private val ORANGE = Color.rgb(255, 102, 0)
        private val GREEN = Color.rgb(44, 201, 141)
        private val RED = Color.rgb(255, 83, 83)
        private val BLUE = Color.rgb(79, 155, 255)
        private val MUTED = Color.rgb(164, 164, 172)
    }
}
