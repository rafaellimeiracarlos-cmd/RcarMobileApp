package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivityRelatorios : RcarBaseActivity() {

    private enum class Aba { VENDAS, PRODUTOS }

    private lateinit var repository: UsuarioLocalRepository
    private lateinit var tabVendas: TextView
    private lateinit var tabProdutos: TextView
    private lateinit var actionPrimary: Button
    private lateinit var actionSecondary: Button
    private lateinit var reportsContainer: LinearLayout
    private lateinit var metricFaturamento: TextView
    private lateinit var metricVendasHoje: TextView
    private lateinit var metricTicketMedio: TextView
    private lateinit var metricCatalogo: TextView
    private lateinit var metricEstoqueBaixo: TextView

    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    private val white = Color.WHITE
    private val muted = Color.rgb(160, 168, 180)
    private val panel = Color.rgb(20, 22, 24)
    private val panelSoft = Color.rgb(18, 20, 23)
    private val line = Color.rgb(44, 49, 56)
    private val orange = Color.rgb(255, 122, 0)
    private val orangeSoft = Color.rgb(82, 49, 18)
    private val green = Color.rgb(74, 201, 121)
    private val red = Color.rgb(255, 102, 102)
    private val yellow = Color.rgb(255, 193, 94)

    private var abaAtual = Aba.VENDAS
    private var currentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val conta = SessaoRcar.atual(this)
        repository = UsuarioLocalRepository(this)
        if (conta == null || conta.papel != UsuarioLocalRepository.PAPEL_CEO) {
            Toast.makeText(this, "Somente o CEO pode acessar e editar os relatórios.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentEmail = conta.email

        setContentView(R.layout.activity_relatorios)
        bindViews()
        setupClicks()
        refreshOverview()
        updateTab(Aba.VENDAS)
    }

    override fun onResume() {
        super.onResume()
        if (::metricFaturamento.isInitialized) {
            refreshOverview()
            updateTab(abaAtual)
        }
    }

    private fun bindViews() {
        tabVendas = findViewById(R.id.btnTabVendas)
        tabProdutos = findViewById(R.id.btnTabProdutos)
        actionPrimary = findViewById(R.id.btnAcaoPrimaria)
        actionSecondary = findViewById(R.id.btnAcaoSecundaria)
        reportsContainer = findViewById(R.id.containerRelatorios)
        metricFaturamento = findViewById(R.id.txtMetricFaturamento)
        metricVendasHoje = findViewById(R.id.txtMetricVendasHoje)
        metricTicketMedio = findViewById(R.id.txtMetricTicketMedio)
        metricCatalogo = findViewById(R.id.txtMetricCatalogo)
        metricEstoqueBaixo = findViewById(R.id.txtMetricEstoqueBaixo)
    }

    private fun setupClicks() {
        findViewById<TextView>(R.id.btnVoltar).setOnClickListener { finish() }
        tabVendas.setOnClickListener { updateTab(Aba.VENDAS) }
        tabProdutos.setOnClickListener { updateTab(Aba.PRODUTOS) }
        findViewById<Button>(R.id.btnGerar).setOnClickListener {
            refreshOverview()
            updateTab(abaAtual)
            Toast.makeText(this, "Relatório atualizado com os dados locais do app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshOverview() {
        val produtos = AdminLocalStore.products(this)
        val ativos = produtos.count { it.ativo }
        val estoqueBaixo = produtos.count { it.ativo && it.estoque <= it.estoqueMinimo }
        metricFaturamento.text = money.format(AdminLocalStore.totalSales(this))
        metricVendasHoje.text = AdminLocalStore.salesTodayCount(this).toString()
        metricTicketMedio.text = money.format(AdminLocalStore.averageSaleToday(this))
        metricCatalogo.text = "$ativos ativos"
        metricEstoqueBaixo.text = "$estoqueBaixo com estoque baixo"
    }

    private fun updateTab(aba: Aba) {
        abaAtual = aba
        val activeBg = getDrawable(R.drawable.bg_reports_chip_active)
        val inactiveBg = getDrawable(R.drawable.bg_reports_chip_inactive)
        when (aba) {
            Aba.VENDAS -> {
                tabVendas.background = activeBg
                tabVendas.setTextColor(white)
                tabProdutos.background = inactiveBg
                tabProdutos.setTextColor(Color.rgb(216, 216, 216))
                actionPrimary.text = "Abrir painel de vendas"
                actionSecondary.text = "Ver margem e lucro"
                actionPrimary.setOnClickListener { startActivity(Intent(this, MainActivityVendas::class.java)) }
                actionSecondary.setOnClickListener { startActivity(Intent(this, MainActivityMargemLucro::class.java)) }
                renderSalesReport()
            }
            Aba.PRODUTOS -> {
                tabProdutos.background = activeBg
                tabProdutos.setTextColor(white)
                tabVendas.background = inactiveBg
                tabVendas.setTextColor(Color.rgb(216, 216, 216))
                actionPrimary.text = "Cadastrar novo produto"
                actionSecondary.text = "Ver produtos cadastrados"
                actionPrimary.setOnClickListener { startActivity(Intent(this, MainActivityCadastroProduto::class.java)) }
                actionSecondary.setOnClickListener { startActivity(Intent(this, MainActivityProdutosCadastrados::class.java)) }
                renderProductsReport()
            }
        }
    }

    private fun renderSalesReport() {
        reportsContainer.removeAllViews()
        reportsContainer.addView(sectionHeader("Resumo de vendas", "Veja o desempenho recente do faturamento e as últimas movimentações."))

        val diarios = AdminLocalStore.dailySales(this, 7)
        val serieCard = cardContainer()
        serieCard.addView(title("Últimos 7 dias", 16f, white, true))
        serieCard.addView(text("Faturamento por dia e quantidade de vendas registradas.", 12f, muted), lp(top = 4))
        if (diarios.all { it.total <= 0.0 && it.count == 0 }) {
            serieCard.addView(emptyNote("Ainda não há vendas suficientes para compor a série diária."), lp(top = 12))
        } else {
            diarios.forEachIndexed { index, item ->
                val row = rowContainer()
                row.addView(text(item.label, 13f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
                val center = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
                center.addView(text(money.format(item.total), 13f, orange, true))
                center.addView(text("${item.count} venda(s)", 11f, muted))
                row.addView(center)
                serieCard.addView(row, lp(top = if (index == 0) 12 else 8))
            }
        }
        reportsContainer.addView(serieCard)

        val sales = AdminLocalStore.recentSales(this, 10)
        val recentCard = cardContainer()
        recentCard.addView(title("Vendas recentes", 16f, white, true))
        recentCard.addView(text("Registro financeiro mais recente do aplicativo.", 12f, muted), lp(top = 4))
        if (sales.isEmpty()) {
            recentCard.addView(emptyNote("Nenhuma venda foi registrada até o momento."), lp(top = 12))
        } else {
            sales.forEachIndexed { index, venda ->
                val row = rowContainer()
                val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                left.addView(text("Venda #${sales.size - index}", 13f, white, true))
                left.addView(text(dateFormat.format(Date(venda.timestamp)), 11f, muted), lp(top = 2))
                row.addView(left, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(text(money.format(venda.total), 15f, orange, true))
                recentCard.addView(row, lp(top = if (index == 0) 12 else 8))
            }
        }
        reportsContainer.addView(recentCard, lp(top = 12))

        val detailed = AdminLocalStore.vendasDetalhadas(this, 8)
        val linesCard = cardContainer()
        linesCard.addView(title("Produtos vendidos recentemente", 16f, white, true))
        linesCard.addView(text("Linhas de venda com item, quantidade e responsável pelo registro.", 12f, muted), lp(top = 4))
        if (detailed.isEmpty()) {
            linesCard.addView(emptyNote("As linhas detalhadas de venda aparecerão aqui depois das próximas compras ou vendas internas."), lp(top = 12))
        } else {
            detailed.forEachIndexed { index, item ->
                val wrapper = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = rounded(panelSoft, line, 14f)
                }
                wrapper.addView(text(item.nomeProduto, 14f, white, true))
                wrapper.addView(text("${item.quantidade} unidade(s) • ${money.format(item.precoUnitario)} por unidade", 12f, muted), lp(top = 4))
                val lucroUnit = item.precoUnitario - item.custoUnitario
                val lucroCor = if (lucroUnit >= 0) green else red
                wrapper.addView(text("Lucro bruto unitário estimado: ${money.format(lucroUnit)}", 12f, lucroCor, true), lp(top = 4))
                wrapper.addView(text("Responsável: ${if (item.responsavel.isBlank()) "sistema" else item.responsavel}", 11f, muted), lp(top = 4))
                wrapper.addView(text(dateFormat.format(Date(item.timestamp)), 11f, muted), lp(top = 2))
                linesCard.addView(wrapper, lp(top = if (index == 0) 12 else 8))
            }
        }
        reportsContainer.addView(linesCard, lp(top = 12))
    }

    private fun renderProductsReport() {
        reportsContainer.removeAllViews()
        reportsContainer.addView(sectionHeader("Resumo de produtos", "Monitore catálogo, estoque, preços e margem estimada dos itens."))

        val produtos = AdminLocalStore.products(this).sortedWith(compareBy<AdminProduto> { !it.ativo }.thenBy { it.nome.lowercase(Locale.getDefault()) })
        val resumoCard = cardContainer()
        val ativos = produtos.count { it.ativo }
        val pausados = produtos.count { !it.ativo || !it.anuncioAtivo }
        resumoCard.addView(title("Visão do catálogo", 16f, white, true))
        resumoCard.addView(text("$ativos produtos ativos • $pausados com anúncio pausado ou item inativo", 12f, muted), lp(top = 4))
        resumoCard.addView(text("Valor em estoque: ${money.format(AdminLocalStore.inventoryValue(this))}", 13f, orange, true), lp(top = 12))
        reportsContainer.addView(resumoCard)

        if (produtos.isEmpty()) {
            reportsContainer.addView(cardContainer().apply {
                addView(emptyNote("Nenhum produto cadastrado ainda."))
            }, lp(top = 12))
            return
        }

        produtos.take(20).forEachIndexed { index, produto ->
            val card = cardContainer()
            val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(title(produto.nome, 16f, white, true))
            info.addView(text("${produto.categoria} • SKU ${produto.sku.ifBlank { "não informado" }}", 12f, muted), lp(top = 2))
            header.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(statusPill(if (produto.ativo && produto.anuncioAtivo) "ATIVO" else "PAUSADO", if (produto.ativo && produto.anuncioAtivo) green else yellow))
            card.addView(header)

            val marginText = if (produto.preco > 0.0 && produto.precoCusto > 0.0) {
                val lucro = produto.preco - produto.precoCusto
                val margem = (lucro / produto.preco) * 100.0
                "Margem estimada: ${String.format(Locale("pt", "BR"), "%.1f", margem)}%"
            } else "Margem estimada indisponível"

            val grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(metricMini("Venda", money.format(produto.preco)), LinearLayout.LayoutParams(0, -2, 1f))
            grid.addView(metricMini("Custo", if (produto.precoCusto > 0.0) money.format(produto.precoCusto) else "—"), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            grid.addView(metricMini("Estoque", produto.estoque.toString()), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            card.addView(grid, lp(top = 12))

            card.addView(text(marginText, 12f, if (produto.precoCusto > 0.0) orange else muted, true), lp(top = 10))
            card.addView(text("Estoque mínimo: ${produto.estoqueMinimo} • Fornecedor: ${produto.fornecedor.ifBlank { "não informado" }}", 12f, muted), lp(top = 4))
            card.addView(text("Responsável: ${produto.cadastradoPor.ifBlank { "não informado" }}", 12f, muted), lp(top = 4))

            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val edit = actionButton("Editar produto", filled = true).apply {
                setOnClickListener {
                    startActivity(Intent(this@MainActivityRelatorios, MainActivityCadastroProduto::class.java)
                        .putExtra("EDITAR_PRODUTO_NOME", produto.nome))
                }
            }
            val stock = actionButton("Abrir estoque", filled = false).apply {
                setOnClickListener { startActivity(Intent(this@MainActivityRelatorios, MainActivityEstoque::class.java)) }
            }
            buttons.addView(edit, LinearLayout.LayoutParams(0, dp(42), 1f))
            buttons.addView(stock, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(10) })
            card.addView(buttons, lp(top = 14))

            reportsContainer.addView(card, lp(top = if (index == 0) 12 else 12))
        }
    }

    private fun sectionHeader(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(title.uppercase(Locale.getDefault()), 12f, orange, true))
        addView(text(subtitle, 12f, muted), lp(top = 4))
    }

    private fun cardContainer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = rounded(panel, orangeSoft, 18f)
    }

    private fun rowContainer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(panelSoft, line, 14f)
    }

    private fun metricMini(title: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = rounded(panelSoft, line, 14f)
        addView(text(title.uppercase(Locale.getDefault()), 10f, muted, true))
        addView(text(value, 13f, white, true), lp(top = 4))
    }

    private fun emptyNote(message: String): TextView = text(message, 13f, muted).apply {
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(panelSoft, line, 14f)
    }

    private fun statusPill(label: String, color: Int): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (color == yellow) Color.BLACK else white)
        textSize = 11f
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = rounded(color, null, 50f)
    }

    private fun actionButton(label: String, filled: Boolean): Button = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(white)
        background = getDrawable(if (filled) R.drawable.bg_reports_button_primary else R.drawable.bg_reports_button_secondary)
    }

    private fun title(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(fill: Int, stroke: Int?, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radiusDp.toInt()).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun lp(top: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        if (top > 0) topMargin = dp(top)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
