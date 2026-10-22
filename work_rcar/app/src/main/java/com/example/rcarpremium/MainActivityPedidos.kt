package com.example.rcarpremium

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

/**
 * Tela de pedidos com dois modos:
 * - CLIENTE: vê apenas os próprios pedidos, sem notas internas.
 * - CEO/FUNCIONÁRIO autorizado: vê todos os pedidos e pode editar notas/status.
 */
class MainActivityPedidos : RcarBaseActivity() {

    private val bg = Color.rgb(10, 10, 11)
    private val panel = Color.rgb(27, 27, 29)
    private val panelRaised = Color.rgb(35, 35, 38)
    private val orange = Color.rgb(255, 112, 0)
    private val orangeSoft = Color.rgb(68, 37, 19)
    private val green = Color.rgb(52, 211, 153)
    private val blue = Color.rgb(96, 165, 250)
    private val red = Color.rgb(248, 113, 113)
    private val muted = Color.rgb(164, 164, 170)
    private val white = Color.WHITE
    private val currency: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    private var currentEmail: String = ""
    private var managerMode: Boolean = false
    private var selectedFilter: String = "Todos"
    private lateinit var searchField: EditText
    private lateinit var filterContainer: LinearLayout
    private lateinit var statsContainer: LinearLayout
    private lateinit var ordersContainer: LinearLayout
    private var loadedOrders: List<PedidoLocal> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = SessaoRcar.atual(this)?.email
        if (email.isNullOrBlank()) {
            Toast.makeText(this, "Entre na sua conta para consultar pedidos.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentEmail = email.trim().lowercase(Locale.ROOT)
        val repository = UsuarioLocalRepository(this)
        if (!repository.usuarioAtivo(currentEmail)) {
            Toast.makeText(this, "Esta conta está bloqueada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val papel = repository.papel(currentEmail)
        managerMode = when (papel) {
            UsuarioLocalRepository.PAPEL_CEO -> true
            in UsuarioLocalRepository.PAPEIS_EQUIPE -> {
                val permitido = repository.temPermissao(currentEmail, "gerenciar_pedidos")
                if (!permitido) {
                    Toast.makeText(this, "O CEO ainda não liberou o gerenciamento de pedidos para esta conta.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                true
            }
            UsuarioLocalRepository.PAPEL_CLIENTE -> false
            else -> {
                finish()
                return
            }
        }

        window.statusBarColor = bg
        window.navigationBarColor = Color.BLACK
        buildScreen()
        refreshOrders()
    }

    override fun onResume() {
        super.onResume()
        if (::ordersContainer.isInitialized) {
            val email = RcarLocalStore.currentEmail(this)
            if (managerMode && !PedidoLocalStore.podeGerenciarPedidos(this, email)) {
                Toast.makeText(this, "A permissão de gerenciar pedidos foi removida pelo CEO.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            refreshOrders()
        }
    }

    private fun buildScreen() {
        setContentView(R.layout.activity_pedidos)

        findViewById<TextView>(R.id.btnVoltarPedidos).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloPedidos).text = if (managerMode) "Gestão de pedidos" else "Meus pedidos"
        findViewById<TextView>(R.id.txtSubtituloPedidos).text = if (managerMode) {
            "Pedidos, notas internas e acompanhamento"
        } else {
            "Acompanhe cada etapa da sua compra"
        }
        findViewById<TextView>(R.id.txtModoPedidos).apply {
            text = if (managerMode) "● ACESSO INTERNO" else "● MINHA CONTA"
            setTextColor(orange)
        }
        findViewById<TextView>(R.id.txtHeroEyebrow).text = if (managerMode) {
            "📦  CENTRAL DE PEDIDOS"
        } else {
            "🚘  EXPERIÊNCIA RCAR PREMIUM"
        }
        findViewById<TextView>(R.id.txtHeroTitle).text = if (managerMode) {
            "Controle cada pedido do início à entrega."
        } else {
            "Tudo organizado para você acompanhar sua compra."
        }
        findViewById<TextView>(R.id.txtHeroDescription).text = if (managerMode) {
            "Acompanhe clientes, valores, status e registre observações internas para a equipe."
        } else {
            "Consulte o status, os itens e o valor dos seus pedidos em um só lugar."
        }
        findViewById<TextView>(R.id.txtTituloResumoPedidos).text = if (managerMode) "VISÃO GERAL" else "RESUMO DAS COMPRAS"
        findViewById<TextView>(R.id.txtSubtituloResumoPedidos).text = if (managerMode) {
            "Indicadores da carteira de pedidos"
        } else {
            "Resumo do seu histórico"
        }
        findViewById<TextView>(R.id.txtSectionPedidos).text = if (managerMode) "PEDIDOS REGISTRADOS" else "SEU HISTÓRICO"
        findViewById<TextView>(R.id.txtSectionPedidosSub).text = if (managerMode) {
            "Pesquise clientes, acompanhe status e mantenha as notas internas"
        } else {
            "Confira cada pedido e seu andamento"
        }
        findViewById<View>(R.id.layoutPermissaoNotas).visibility = if (managerMode) View.VISIBLE else View.GONE
        findViewById<View>(R.id.bottomNavPedidos).visibility = if (managerMode) View.GONE else View.VISIBLE

        statsContainer = findViewById(R.id.statsContainer)
        searchField = findViewById(R.id.fieldBuscaPedidos)
        searchField.hint = if (managerMode) "Buscar por pedido, cliente ou e-mail..." else "Buscar pedido ou produto..."
        filterContainer = findViewById(R.id.filterContainer)
        ordersContainer = findViewById(R.id.ordersContainer)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderOrders() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        if (!managerMode) BottomNav.setup(this, "pedidos")
    }

    private fun refreshOrders() {
        loadedOrders = if (managerMode) {
            PedidoLocalStore.listar(this)
        } else {
            PedidoLocalStore.listarDoCliente(this, currentEmail)
        }
        renderStats()
        renderFilters()
        renderOrders()
    }

    private fun renderStats() {
        if (!::statsContainer.isInitialized) return
        statsContainer.removeAllViews()
        val total = loadedOrders.size
        val active = loadedOrders.count { it.status !in listOf("Entregue", "Cancelado") }
        val delivered = loadedOrders.count { it.status == "Entregue" }
        val metrics = listOf(
            Triple(if (managerMode) "PEDIDOS" else "TOTAL", total.toString(), orange),
            Triple("EM ANDAMENTO", active.toString(), blue),
            Triple("ENTREGUES", delivered.toString(), green)
        )
        metrics.forEachIndexed { index, metric ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(panel, 15f, Color.rgb(42, 42, 45))
                setPadding(dp(11), dp(12), dp(8), dp(12))
            }
            box.addView(text(metric.first, 8.5f, muted, true).apply { maxLines = 1 })
            box.addView(text(metric.second, 23f, metric.third, true).apply { setPadding(0, dp(5), 0, 0) })
            val lp = LinearLayout.LayoutParams(0, -1, 1f)
            if (index > 0) lp.leftMargin = dp(8)
            statsContainer.addView(box, lp)
        }
    }

    private fun renderFilters() {
        if (!::filterContainer.isInitialized) return
        filterContainer.removeAllViews()
        val filters = listOf("Todos", "Em andamento", "Separando produtos", "Enviado", "Entregue", "Cancelado")
        filters.forEach { filter ->
            val active = selectedFilter == filter
            val chip = text(filter, 11f, if (active) Color.BLACK else Color.rgb(224, 224, 227), active).apply {
                background = rounded(if (active) orange else panel, 30f, if (active) orange else Color.rgb(52, 52, 56))
                setPadding(dp(13), dp(9), dp(13), dp(9))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedFilter = filter
                    renderFilters()
                    renderOrders()
                }
            }
            val lp = LinearLayout.LayoutParams(-2, -2)
            if (filterContainer.childCount > 0) lp.rightMargin = dp(7)
            filterContainer.addView(chip, lp)
        }
    }

    private fun renderOrders() {
        if (!::ordersContainer.isInitialized) return
        ordersContainer.removeAllViews()
        val query = if (::searchField.isInitialized) searchField.text.toString().trim() else ""
        val filtered = loadedOrders.filter { pedido ->
            val matchesQuery = query.isBlank() || listOf(
                pedido.id, pedido.clienteNome, pedido.clienteEmail, pedido.resumoItens, pedido.status
            ).any { it.contains(query, ignoreCase = true) }
            matchesQuery && (selectedFilter == "Todos" || pedido.status.equals(selectedFilter, ignoreCase = true))
        }
        if (filtered.isEmpty()) {
            val empty = card(panel, 18f, Color.rgb(45, 45, 48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(20), dp(24), dp(20), dp(24))
            }
            empty.addView(text(if (managerMode) "📦" else "🛍️", 34f, orange, true).apply { gravity = Gravity.CENTER })
            empty.addView(text(
                if (query.isNotBlank() || selectedFilter != "Todos") "Nenhum pedido encontrado" else if (managerMode) "Ainda não há pedidos" else "Sua próxima compra começa aqui",
                17f, white, true
            ).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(6)) })
            empty.addView(text(
                if (query.isNotBlank() || selectedFilter != "Todos") "Tente mudar a busca ou o filtro selecionado." else if (managerMode) "Os pedidos aparecerão aqui quando uma compra for confirmada no aplicativo." else "Quando você finalizar uma compra, poderá acompanhar o andamento por esta tela.",
                12f, muted
            ).apply { gravity = Gravity.CENTER })
            if (!managerMode && query.isBlank() && selectedFilter == "Todos") {
                empty.addView(actionButton("EXPLORAR PRODUTOS", true) {
                    startActivity(android.content.Intent(this, MainActivityListaProdutos::class.java))
                }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(16) })
            }
            ordersContainer.addView(empty)
            return
        }
        filtered.forEach { pedido ->
            ordersContainer.addView(if (managerMode) adminOrderCard(pedido) else customerOrderCard(pedido),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
    }

    private fun customerOrderCard(pedido: PedidoLocal): View {
        val card = card(panel, 18f, Color.rgb(45, 45, 48)).apply { setPadding(dp(16), dp(15), dp(16), dp(15)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("PEDIDO #${pedido.id}", 12f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(statusBadge(pedido.status))
        card.addView(header)
        card.addView(text(PedidoLocalStore.dataFormatada(pedido.criadoEm), 10.5f, muted).apply {
            setPadding(0, dp(7), 0, dp(12))
        })
        if (pedido.demonstracao) {
            card.addView(text("PEDIDO DE DEMONSTRAÇÃO", 9f, orange, true).apply {
                background = rounded(orangeSoft, 7f)
                setPadding(dp(8), dp(5), dp(8), dp(5))
            })
        }
        card.addView(text(pedido.resumoItens, 14f, white, true).apply {
            setPadding(0, dp(10), 0, dp(8))
            maxLines = 3
        })
        card.addView(text("${pedido.quantidadeItens} ${if (pedido.quantidadeItens == 1) "unidade" else "unidades"} • ${pedido.formaPagamento}", 11f, muted))
        val progressRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(15), 0, dp(11))
        }
        progressRow.addView(text("ACOMPANHAMENTO DO PEDIDO", 9.5f, muted, true))
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 4
            progress = progressFor(pedido.status)
            progressTintList = ColorStateList.valueOf(if (pedido.status == "Cancelado") red else orange)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(55, 55, 59))
        }
        progressRow.addView(progress, LinearLayout.LayoutParams(-1, dp(5)).apply { topMargin = dp(9) })
        progressRow.addView(text(customerStatusDescription(pedido.status), 11f, muted).apply { setPadding(0, dp(7), 0, 0) })
        card.addView(progressRow)
        val divider = View(this).apply { setBackgroundColor(Color.rgb(48, 48, 52)) }
        card.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply { bottomMargin = dp(12) })
        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val valueColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        valueColumn.addView(text("TOTAL DO PEDIDO", 9.5f, muted, true))
        valueColumn.addView(text(currency.format(pedido.total), 20f, orange, true).apply { setPadding(0, dp(3), 0, 0) })
        footer.addView(valueColumn, LinearLayout.LayoutParams(0, -2, 1f))
        footer.addView(text(if (pedido.status == "Entregue") "✓ Concluído" else if (pedido.status == "Cancelado") "Pedido cancelado" else "Em acompanhamento", 10f,
            if (pedido.status == "Entregue") green else if (pedido.status == "Cancelado") red else orange, true))
        card.addView(footer)
        card.addView(actionButton("⌖  ACOMPANHAR ENTREGA • MAPA E NOTA FISCAL", true) {
            startActivity(Intent(this, MainActivityAcompanharPedido::class.java).putExtra("PEDIDO_ID", pedido.id))
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(13) })
        return card
    }

    private fun adminOrderCard(pedido: PedidoLocal): View {
        val card = card(panel, 18f, Color.rgb(47, 47, 51)).apply { setPadding(dp(15), dp(15), dp(15), dp(15)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val orderTitle = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        orderTitle.addView(text("PEDIDO #${pedido.id}", 14f, white, true))
        orderTitle.addView(text(PedidoLocalStore.dataFormatada(pedido.criadoEm), 10f, muted).apply { setPadding(0, dp(4), 0, 0) })
        header.addView(orderTitle, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(statusBadge(pedido.status))
        card.addView(header)
        if (pedido.demonstracao) {
            card.addView(text("DADOS DE DEMONSTRAÇÃO", 9f, orange, true).apply {
                background = rounded(orangeSoft, 6f)
                setPadding(dp(8), dp(4), dp(8), dp(4))
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })
        }

        card.addView(sectionLabel("CLIENTE", "${pedido.clienteNome}\n${pedido.clienteEmail}"))
        card.addView(sectionLabel("ITENS DO PEDIDO", pedido.resumoItens))
        val amountRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(22, 22, 24), 12f)
            setPadding(dp(12), dp(11), dp(12), dp(11))
        }
        val amountColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        amountColumn.addView(text("VALOR TOTAL", 9f, muted, true))
        amountColumn.addView(text(currency.format(pedido.total), 20f, orange, true).apply { setPadding(0, dp(4), 0, 0) })
        amountRow.addView(amountColumn, LinearLayout.LayoutParams(0, -2, 1f))
        amountRow.addView(text("${pedido.quantidadeItens} itens\n${pedido.formaPagamento}", 10f, muted).apply { gravity = Gravity.END })
        card.addView(amountRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val notePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(34, 27, 22), 12f, Color.rgb(94, 58, 32))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val noteHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        noteHeader.addView(text("🔒  NOTA INTERNA", 10f, orange, true), LinearLayout.LayoutParams(0, -2, 1f))
        noteHeader.addView(text(if (pedido.notaInterna.isBlank()) "SEM NOTA" else "REGISTRADA", 8.5f,
            if (pedido.notaInterna.isBlank()) muted else green, true).apply {
            background = rounded(Color.rgb(45, 39, 33), 20f)
            setPadding(dp(7), dp(4), dp(7), dp(4))
        })
        notePanel.addView(noteHeader)
        notePanel.addView(text(
            pedido.notaInterna.ifBlank { "Nenhuma observação interna registrada para este pedido." },
            12f, if (pedido.notaInterna.isBlank()) muted else white
        ).apply { setPadding(0, dp(9), 0, dp(5)) })
        if (pedido.atualizadoPor.isNotBlank()) {
            notePanel.addView(text("Última atualização: ${pedido.atualizadoPor} • ${PedidoLocalStore.dataFormatada(pedido.atualizadoEm)}", 9f, muted))
        } else {
            notePanel.addView(text("Visível somente à administração e equipe autorizada.", 9f, muted))
        }
        card.addView(notePanel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(actionButton("✎  ${if (pedido.notaInterna.isBlank()) "ADICIONAR NOTA" else "EDITAR NOTA"}", true) {
            editarNota(pedido)
        }, LinearLayout.LayoutParams(0, dp(44), 1.1f).apply { topMargin = dp(11); rightMargin = dp(7) })
        actions.addView(actionButton("ATUALIZAR STATUS", false) {
            selecionarStatus(pedido)
        }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { topMargin = dp(11) })
        card.addView(actions)
        card.addView(actionButton("🚚  EDITAR RASTREIO / PRAZO / LOCALIZAÇÃO", false) {
            editarRastreamento(pedido)
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(11) })
        card.addView(actionButton("⌖  VER ACOMPANHAMENTO DO CLIENTE", false) {
            startActivity(Intent(this, MainActivityAcompanharPedido::class.java).putExtra("PEDIDO_ID", pedido.id))
        }, LinearLayout.LayoutParams(-1, dp(43)).apply { topMargin = dp(7) })
        return card
    }

    private fun editarRastreamento(pedido: PedidoLocal) {
        if (!PedidoLocalStore.podeGerenciarPedidos(this, currentEmail)) {
            Toast.makeText(this, "Sem permissão para atualizar a entrega.", Toast.LENGTH_LONG).show(); return
        }
        val old = PedidoRastreamentoStore.get(this, pedido.id)
        val fields = listOf(
            "Transportadora" to (old?.transportadora.orEmpty()),
            "Código de rastreio" to (old?.codigo.orEmpty()),
            "Previsão de entrega (ex.: 28/10/2026)" to (old?.previsao.orEmpty()),
            "Latitude da ÚLTIMA posição fornecida" to "",
            "Longitude da ÚLTIMA posição fornecida" to "",
            "Informações para o cliente" to (old?.observacao.orEmpty())
        )
        val panelWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.rgb(15, 17, 23), 18f, orange)
        }
        form.addView(text("Informe apenas dados recebidos da transportadora. A localização não será inventada e os dados continuam locais neste aparelho.", 12f, muted).apply {
            setPadding(0, 0, 0, dp(12))
        })
        val inputs = fields.mapIndexed { index, (hint, initial) ->
            EditText(this).apply {
                this.hint = hint
                setText(initial)
                setTextColor(white)
                setHintTextColor(Color.rgb(125, 132, 143))
                textSize = 13f
                background = rounded(Color.rgb(21, 24, 31), 12f, Color.rgb(58, 64, 74))
                setPadding(dp(12), dp(12), dp(12), dp(12))
                if (index == 5) {
                    minLines = 3
                    maxLines = 5
                    gravity = Gravity.TOP or Gravity.START
                } else {
                    setSingleLine(true)
                }
                form.addView(this, LinearLayout.LayoutParams(-1, if (index == 5) -2 else dp(52)).apply {
                    topMargin = if (index == 0) 0 else dp(9)
                })
            }
        }
        panelWrap.addView(form, LinearLayout.LayoutParams(-1, -2))
        val view = ScrollView(this).apply { addView(panelWrap) }
        val dialog = AlertDialog.Builder(this).setTitle("Entrega • Pedido #${pedido.id}")
            .setView(view).setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar informações", null).create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    PedidoRastreamentoStore.atualizar(this, pedido.id, inputs[0].text.toString(), inputs[1].text.toString(),
                        inputs[2].text.toString(), inputs[3].text.toString().trim(), inputs[4].text.toString().trim(),
                        inputs[5].text.toString(), currentEmail)
                }.onSuccess {
                    Toast.makeText(this, "Informações registradas.", Toast.LENGTH_SHORT).show(); dialog.dismiss(); refreshOrders()
                }.onFailure { Toast.makeText(this, it.message ?: "Erro ao salvar rastreio", Toast.LENGTH_LONG).show() }
            }
        }
        dialog.show()
    }

    private fun editarNota(pedido: PedidoLocal) {
        if (!PedidoLocalStore.podeGerenciarPedidos(this, currentEmail)) {
            Toast.makeText(this, "Somente o CEO ou funcionários autorizados podem editar notas.", Toast.LENGTH_LONG).show()
            return
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(6), dp(4), 0)
        }
        form.addView(text("Esta nota é interna e não aparece para o cliente.", 12f, muted).apply {
            setPadding(0, 0, 0, dp(10))
        })
        val input = EditText(this).apply {
            setText(pedido.notaInterna)
            hint = "Ex.: confirmar compatibilidade, separar peça, retorno ao cliente..."
            setHintTextColor(Color.rgb(130, 130, 136))
            setTextColor(white)
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 7
            background = rounded(Color.rgb(27, 27, 30), 12f, Color.rgb(75, 75, 80))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        form.addView(input, LinearLayout.LayoutParams(-1, -2))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Nota interna • Pedido #${pedido.id}")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar nota", null)
            .create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = PedidoLocalStore.salvarNota(this, pedido.id, input.text.toString(), currentEmail)
                if (!ok) {
                    Toast.makeText(this, "Não foi possível salvar. Verifique se o CEO mantém sua permissão.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(input.windowToken, 0)
                Toast.makeText(this, "Nota interna salva.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                refreshOrders()
            }
        }
        dialog.show()
    }

    private fun selecionarStatus(pedido: PedidoLocal) {
        if (!PedidoLocalStore.podeGerenciarPedidos(this, currentEmail)) {
            Toast.makeText(this, "Você não tem permissão para alterar pedidos.", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Status do pedido #${pedido.id}")
            .setItems(PedidoLocalStore.statusDisponiveis.toTypedArray()) { _, index ->
                val novoStatus = PedidoLocalStore.statusDisponiveis[index]
                if (PedidoLocalStore.atualizarStatus(this, pedido.id, novoStatus, currentEmail)) {
                    Toast.makeText(this, "Status atualizado para: $novoStatus", Toast.LENGTH_SHORT).show()
                    refreshOrders()
                } else {
                    Toast.makeText(this, "Não foi possível atualizar o status.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun statusBadge(status: String): TextView {
        val color = when (status) {
            "Entregue" -> green
            "Enviado" -> blue
            "Cancelado" -> red
            "Separando produtos" -> orange
            else -> Color.rgb(255, 193, 7)
        }
        val base = when (status) {
            "Entregue" -> Color.rgb(20, 57, 44)
            "Enviado" -> Color.rgb(22, 43, 68)
            "Cancelado" -> Color.rgb(65, 28, 31)
            else -> Color.rgb(62, 44, 22)
        }
        return text("●  $status", 9.5f, color, true).apply {
            background = rounded(base, 30f)
            setPadding(dp(9), dp(7), dp(9), dp(7))
            maxLines = 1
        }
    }

    private fun customerStatusDescription(status: String): String = when (status) {
        "Entregue" -> "Pedido entregue com sucesso. Obrigado por comprar na RCar Premium!"
        "Cancelado" -> "Este pedido foi cancelado. Entre em contato com o suporte se precisar de ajuda."
        "Enviado" -> "Seu pedido foi enviado e está a caminho."
        "Separando produtos" -> "Estamos separando suas peças para envio."
        else -> "Seu pedido foi recebido e está em acompanhamento."
    }

    private fun progressFor(status: String): Int = when (status) {
        "Em andamento" -> 1
        "Separando produtos" -> 2
        "Enviado" -> 3
        "Entregue" -> 4
        "Cancelado" -> 0
        else -> 1
    }

    private fun sectionLabel(label: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(12), dp(2), 0)
        addView(text(label, 9f, muted, true))
        addView(text(value, 12f, white).apply { setPadding(0, dp(4), 0, 0); maxLines = 4 })
    }

    private fun sectionHeader(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(19), dp(2), dp(10))
        addView(text(title, 13f, orange, true))
        addView(text(subtitle, 10.5f, muted).apply { setPadding(0, dp(4), 0, 0) })
    }

    private fun actionButton(title: String, primary: Boolean, action: () -> Unit): TextView = text(
        title, 10f, if (primary) Color.BLACK else white, true
    ).apply {
        gravity = Gravity.CENTER
        background = rounded(if (primary) orange else panelRaised, 12f, if (primary) orange else Color.rgb(66, 66, 70))
        setPadding(dp(8), dp(10), dp(8), dp(10))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun card(color: Int, radius: Float, stroke: Int? = null): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, radius, stroke)
    }

    private fun rounded(color: Int, radius: Float, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = false
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
