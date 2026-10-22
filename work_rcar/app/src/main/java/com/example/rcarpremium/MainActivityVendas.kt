package com.example.rcarpremium

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivityVendas : RcarBaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val moeda: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    private var ceoMode = false

    private lateinit var tvFaturamentoHoje: TextView
    private lateinit var tvPedidosHoje: TextView
    private lateinit var tvCarrinhosAtivos: TextView
    private lateinit var tvAbandonados: TextView
    private lateinit var tvTicketMedio: TextView
    private lateinit var tvConversao: TextView
    private lateinit var tvFaturamentoAcumulado: TextView
    private lateinit var tvCheckoutsHoje: TextView
    private lateinit var tvAtualizado: TextView
    private lateinit var chart: RcarAnalyticsChartView
    private lateinit var funnelContainer: LinearLayout
    private lateinit var journeysContainer: LinearLayout
    private lateinit var salesContainer: LinearLayout
    private var lastJourneySignature = "__initial__"
    private var lastSalesSignature = "__initial__"
    private val funnelRows = mutableMapOf<String, FunnelRow>()

    private data class FunnelRow(val value: TextView, val progress: ProgressBar)

    private val liveTicker = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                refreshLiveData()
                handler.postDelayed(this, 5_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = RcarLocalStore.currentEmail(this)
        val repository = UsuarioLocalRepository(this)
        if (!repository.temPermissao(email, "ver_vendas")) {
            android.widget.Toast.makeText(this, "Seu perfil não tem permissão para consultar vendas.", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
        ceoMode = repository.isCeo(email ?: "")
        buildScreen()
        refreshLiveData()
        handler.postDelayed(liveTicker, 5_000L)
    }

    override fun onResume() {
        super.onResume()
        if (::chart.isInitialized) refreshLiveData()
    }

    override fun onDestroy() {
        handler.removeCallbacks(liveTicker)
        super.onDestroy()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(30))
        }
        scroll.addView(content, android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(scroll)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = text("‹", 36f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
            contentDescription = "Voltar"
        }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(44), dp(48)))
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(text("Vendas e conversão", 22f, Color.WHITE, true))
        titleColumn.addView(text("Monitoramento da operação", 12f, MUTED))
        toolbar.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        val liveBadge = text("● AO VIVO", 10f, GREEN, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(19, 48, 38), 50)
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }
        toolbar.addView(liveBadge)
        content.addView(toolbar)
        tvAtualizado = text("Atualização automática a cada 5 segundos", 11f, MUTED).apply {
            setPadding(dp(44), dp(1), 0, dp(16))
        }
        content.addView(tvAtualizado)

        content.addView(sectionTitle("RESUMO DO DIA", "Indicadores atualizados automaticamente"))
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val revenueMetric = createMetric("FATURAMENTO HOJE", "R$ 0,00", ORANGE)
        val ordersMetric = createMetric("PEDIDOS CONFIRMADOS", "0", GREEN)
        val activeMetric = createMetric("CARRINHOS ATIVOS", "0", BLUE)
        val abandonedMetric = createMetric("CARRINHOS ABANDONADOS", "0", RED)
        val averageMetric = createMetric("TICKET MÉDIO", "R$ 0,00", ORANGE)
        val conversionMetric = createMetric("CONVERSÃO DO FUNIL", "0%", GREEN)
        val accumulatedMetric = createMetric("FATURAMENTO ACUMULADO", "R$ 0,00", Color.WHITE)
        val checkoutMetric = createMetric("CHECKOUTS INICIADOS", "0", BLUE)
        metrics.addView(metricRow(revenueMetric, ordersMetric))
        metrics.addView(metricRow(activeMetric, abandonedMetric))
        metrics.addView(metricRow(averageMetric, conversionMetric))
        metrics.addView(metricRow(accumulatedMetric, checkoutMetric))
        tvFaturamentoHoje = revenueMetric.findViewWithTag<TextView>("metric-value")
        tvPedidosHoje = ordersMetric.findViewWithTag<TextView>("metric-value")
        tvCarrinhosAtivos = activeMetric.findViewWithTag<TextView>("metric-value")
        tvAbandonados = abandonedMetric.findViewWithTag<TextView>("metric-value")
        tvTicketMedio = averageMetric.findViewWithTag<TextView>("metric-value")
        tvConversao = conversionMetric.findViewWithTag<TextView>("metric-value")
        tvFaturamentoAcumulado = accumulatedMetric.findViewWithTag<TextView>("metric-value")
        tvCheckoutsHoje = checkoutMetric.findViewWithTag<TextView>("metric-value")
        content.addView(metrics)

        content.addView(sectionTitle("FUNIL DE COMPRA • ÚLTIMAS 24 HORAS", "Carrinhos, compras concluídas e desistências por hora"))
        val chartCard = card(Color.rgb(22, 22, 25), 18).apply { setPadding(dp(12), dp(14), dp(12), dp(12)) }
        chartCard.addView(text("MOVIMENTAÇÃO POR HORA", 12f, Color.WHITE, true))
        chartCard.addView(text("Cada ponto representa a quantidade de jornadas naquele horário.", 11f, MUTED).apply {
            setPadding(0, dp(3), 0, dp(2))
        })
        chart = RcarAnalyticsChartView(this).apply {
            setFunnelData(emptyList(), emptyList(), emptyList(), emptyList())
            contentDescription = "Gráfico em tempo real de carrinhos iniciados, compras e abandonos"
        }
        chartCard.addView(chart, LinearLayout.LayoutParams(-1, dp(230)))
        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, 0)
        }
        legend.addView(legendItem(ORANGE, "Carrinhos"), LinearLayout.LayoutParams(0, -2, 1f))
        legend.addView(legendItem(GREEN, "Compras"), LinearLayout.LayoutParams(0, -2, 1f))
        legend.addView(legendItem(RED, "Abandonos"), LinearLayout.LayoutParams(0, -2, 1f))
        chartCard.addView(legend)
        content.addView(chartCard)

        content.addView(sectionTitle("ETAPAS DO FUNIL", "Desfecho dos carrinhos iniciados hoje"))
        val funnelCard = card(Color.rgb(22, 22, 25), 18).apply { setPadding(dp(16), dp(8), dp(16), dp(10)) }
        funnelContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        addFunnelRow("Carrinhos iniciados", ORANGE)
        addFunnelRow("Checkout iniciado", BLUE)
        addFunnelRow("Compras concluídas", GREEN)
        addFunnelRow("Abandonados após 30 min", RED)
        funnelCard.addView(funnelContainer)
        content.addView(funnelCard)
        content.addView(text(
            "Um carrinho é classificado como abandonado após 30 minutos sem atividade. O monitoramento desta versão usa dados locais deste dispositivo.",
            11f, MUTED
        ).apply { setPadding(dp(2), dp(8), dp(2), dp(14)) })

        content.addView(sectionTitle("ATIVIDADE DE CARRINHOS", "Últimas jornadas de compra detectadas"))
        journeysContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(journeysContainer)

        content.addView(sectionTitle("VENDAS RECENTES", "Pedidos registrados com sucesso"))
        salesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(salesContainer)
    }

    private fun metricRow(left: View, right: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, LinearLayout.LayoutParams(0, dp(105), 1f).apply { rightMargin = dp(6); bottomMargin = dp(10) })
        addView(right, LinearLayout.LayoutParams(0, dp(105), 1f).apply { leftMargin = dp(6); bottomMargin = dp(10) })
    }

    private fun createMetric(label: String, initial: String, accent: Int): LinearLayout {
        val box = card(Color.rgb(23, 23, 26), 16).apply { setPadding(dp(13), dp(12), dp(12), dp(10)) }
        box.addView(text(label, 10f, MUTED, true))
        val value = text(initial, 21f, accent, true).apply {
            tag = "metric-value"
            setPadding(0, dp(9), 0, 0)
            maxLines = 1
            textSize = if (label.length > 18) 18f else 21f
        }
        box.addView(value)
        return box
    }

    private fun sectionTitle(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(18), dp(2), dp(10))
        addView(text(title, 14f, ORANGE, true))
        addView(text(subtitle, 11f, MUTED).apply { setPadding(0, dp(3), 0, 0) })
    }

    private fun addFunnelRow(label: String, color: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(8))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text(label, 12f, Color.WHITE), LinearLayout.LayoutParams(0, -2, 1f))
        val value = text("0", 13f, color, true)
        header.addView(value)
        row.addView(header)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(color)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(48, 48, 52))
        }
        row.addView(progress, LinearLayout.LayoutParams(-1, dp(5)).apply { topMargin = dp(7) })
        funnelContainer.addView(row)
        funnelRows[label] = FunnelRow(value, progress)
    }

    private fun legendItem(color: Int, label: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val dot = View(this@MainActivityVendas).apply { background = rounded(color, 50) }
        addView(dot, LinearLayout.LayoutParams(dp(7), dp(7)))
        addView(text(label, 10f, MUTED).apply { setPadding(dp(5), 0, 0, 0) })
    }

    private fun refreshLiveData() {
        val now = System.currentTimeMillis()
        val funnel = AnalyticsLocalStore.snapshot(this)
        val salesToday = AdminLocalStore.salesToday(this)
        val ordersToday = AdminLocalStore.salesTodayCount(this)
        val avg = AdminLocalStore.averageSaleToday(this)
        tvFaturamentoHoje.text = moeda.format(salesToday)
        tvPedidosHoje.text = ordersToday.toString()
        tvCarrinhosAtivos.text = funnel.activeNow.toString()
        tvAbandonados.text = funnel.abandonedToday.toString()
        tvTicketMedio.text = moeda.format(avg)
        tvConversao.text = "${funnel.conversionPercent}%"
        tvFaturamentoAcumulado.text = moeda.format(AdminLocalStore.totalSales(this))
        tvCheckoutsHoje.text = funnel.checkoutToday.toString()
        tvAtualizado.text = "Atualizado às ${SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date(now))} • atualização a cada 5 s"

        val timeline = AnalyticsLocalStore.hourlyTimeline(this)
        chart.setFunnelData(timeline.labels, timeline.carts, timeline.purchases, timeline.abandoned)

        val maxStage = maxOf(1, funnel.startedToday, funnel.checkoutToday, funnel.purchasedToday, funnel.abandonedToday)
        updateFunnelRow("Carrinhos iniciados", funnel.startedToday, maxStage)
        updateFunnelRow("Checkout iniciado", funnel.checkoutFromTodayStarts, maxStage)
        updateFunnelRow("Compras concluídas", funnel.purchasedFromTodayStarts, maxStage)
        updateFunnelRow("Abandonados após 30 min", funnel.abandonedFromTodayStarts, maxStage)

        val journeys = AnalyticsLocalStore.recentJourneys(this, 8)
        val journeySignature = journeys.joinToString("|") { "${it.id}:${it.status()}:${it.amount}:${it.itemCount}:${it.lastActivityAt}" }
        if (journeySignature != lastJourneySignature) {
            lastJourneySignature = journeySignature
            renderJourneys(journeys)
        }
        val sales = AdminLocalStore.recentSales(this, 6)
        val salesSignature = sales.joinToString("|") { "${it.timestamp}:${it.total}" }
        if (salesSignature != lastSalesSignature) {
            lastSalesSignature = salesSignature
            renderSales(sales)
        }
    }

    private fun updateFunnelRow(label: String, value: Int, maxValue: Int) {
        val row = funnelRows[label] ?: return
        row.value.text = value.toString()
        row.progress.progress = ((value.toDouble() / maxValue.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
    }

    private fun renderJourneys(journeys: List<CartJourney>) {
        journeysContainer.removeAllViews()
        if (journeys.isEmpty()) {
            journeysContainer.addView(emptyState("Nenhum carrinho monitorado ainda", "Os eventos aparecerão quando alguém adicionar um produto ao carrinho."))
            return
        }
        journeys.forEach { journey ->
            val status = journey.status()
            val statusColor = when (status) {
                "Compra concluída" -> GREEN
                "Abandonado" -> RED
                "Checkout iniciado" -> BLUE
                else -> ORANGE
            }
            val row = card(Color.rgb(22, 22, 25), 14).apply { setPadding(dp(14), dp(12), dp(14), dp(12)) }
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val amount = text(moeda.format(journey.amount), 15f, Color.WHITE, true)
            top.addView(amount, LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(text(status.uppercase(Locale("pt", "BR")), 9f, statusColor, true).apply {
                background = rounded(withAlpha(statusColor, 36), 40)
                setPadding(dp(8), dp(5), dp(8), dp(5))
            })
            row.addView(top)
            val identity = when {
                !ceoMode -> "Cliente"
                journey.email == "cliente_sem_login" -> "Cliente sem login"
                else -> journey.email
            }
            row.addView(text("$identity • ${journey.itemCount} unidade(s)", 11f, MUTED).apply { setPadding(0, dp(6), 0, 0) })
            val moment = when {
                journey.purchasedAt > 0L -> journey.purchasedAt
                journey.abandonedAt > 0L -> journey.abandonedAt
                journey.checkoutAt > 0L -> journey.checkoutAt
                else -> journey.startedAt
            }
            row.addView(text("Início ${formatTime(journey.startedAt)}  •  Atualização ${formatTime(moment)}", 10f, MUTED).apply {
                setPadding(0, dp(4), 0, 0)
            })
            journeysContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun renderSales(sales: List<AdminVenda>) {
        salesContainer.removeAllViews()
        if (sales.isEmpty()) {
            salesContainer.addView(emptyState("Ainda não há vendas", "As compras confirmadas aparecerão aqui."))
            return
        }
        sales.forEach { sale ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.rgb(22, 22, 25), 13)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(text("Venda confirmada", 13f, Color.WHITE, true))
            info.addView(text(formatTime(sale.timestamp), 10f, MUTED).apply { setPadding(0, dp(3), 0, 0) })
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text(moeda.format(sale.total), 15f, GREEN, true))
            salesContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun emptyState(title: String, subtitle: String): View = card(Color.rgb(22, 22, 25), 14).apply {
        setPadding(dp(15), dp(15), dp(15), dp(15))
        addView(text(title, 13f, Color.WHITE, true))
        addView(text(subtitle, 11f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
    }

    private fun card(color: Int, radius: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, radius)
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        includeFontPadding = true
    }

    private fun formatTime(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(timestamp))
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = Color.rgb(10, 10, 11)
        private val ORANGE = Color.rgb(255, 112, 0)
        private val GREEN = Color.rgb(44, 201, 141)
        private val RED = Color.rgb(255, 83, 83)
        private val BLUE = Color.rgb(79, 155, 255)
        private val MUTED = Color.rgb(164, 164, 172)
    }
}
