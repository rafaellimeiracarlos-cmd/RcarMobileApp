package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Conferência do funcionário e aprovação/ajustes auditados pelo CEO. */
class MainActivityConferenciaEstoque : AppCompatActivity() {
    private val orange = Color.rgb(255, 122, 0)
    private val white = Color.WHITE
    private val muted = Color.rgb(182, 186, 194)
    private lateinit var root: LinearLayout
    private lateinit var listArea: LinearLayout
    private lateinit var scroll: ScrollView
    private var manualLines = mutableListOf<ConferenciaLinha>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conta = SessaoRcar.atual(this)
        val repo = UsuarioLocalRepository(this)
        if (conta == null || !(repo.isCeo(conta.email) || repo.temPermissao(conta.email, "conferir_recebimento") ||
                    repo.temPermissao(conta.email, "cadastrar_produtos") || repo.temPermissao(conta.email, "lancar_notas"))) {
            Toast.makeText(this, "Sem permissão para conferir recebimentos.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        build()
    }

    override fun onResume() { super.onResume(); if (::listArea.isInitialized) renderList() }
    private fun ceo(): Boolean = SessaoRcar.atual(this)?.let { UsuarioLocalRepository(this).isCeo(it.email) } == true

    private fun build() {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF090909.toInt()) }
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(40))
        }
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.imglogo); scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "RCarPremium"
        }, LinearLayout.LayoutParams(-1, dp(73)))
        root.addView(label("CONFERÊNCIA DE PEÇAS", 22f, orange, true), lp(10))
        root.addView(label(if (ceo()) "Supervisão CEO • aprovar, devolver e corrigir" else
            "Contagem do recebimento • sujeito à aprovação do CEO", 13f, muted), lp(5))
        root.addView(button("← VOLTAR AO PAINEL", false) { finish() }, lp(12))
        root.addView(card().apply {
            addView(label("ENTRADA SEGURA NO ESTOQUE", 15f, white, true))
            addView(label("A conferência fica pendente até aprovação do CEO. Uma quantidade contada não aumenta o estoque automaticamente. Todo ajuste tem responsável e justificativa.", 12f, muted), lp(8))
            addView(button("IMPORTAR NF-e PARA CONFERÊNCIA", true) {
                startActivity(android.content.Intent(this@MainActivityConferenciaEstoque, MainActivityImportarNota::class.java))
            }, lp(12))
            addView(button("+ CONFERÊNCIA MANUAL DE RECEBIMENTO", false) { manualForm() }, lp(8))
        }, lp(15))
        root.addView(label("CONFERÊNCIAS E HISTÓRICO", 18f, white, true), lp(21))
        listArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listArea, lp(6))
        scroll.addView(root)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        outer.addView(floatingFooter(), LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(outer)
        renderList()
    }

    private fun renderList() {
        listArea.removeAllViews()
        val account = SessaoRcar.atual(this)?.email.orEmpty()
        val all = EstoqueConferenciaStore.list(this)
            .filter { ceo() || it.funcionario.equals(account, true) }
            .sortedWith(compareBy<ConferenciaEstoque> { if (it.situacao == EstoqueConferenciaStore.PENDENTE) 0 else if (it.situacao == EstoqueConferenciaStore.DEVOLVIDA) 1 else 2 }
                .thenByDescending { it.criadoEm })
        val waiting = all.count { it.situacao == EstoqueConferenciaStore.PENDENTE }
        listArea.addView(label("$waiting aguardando aprovação • ${all.size} registros", 13f, orange, true), lp(3))
        if (all.isEmpty()) listArea.addView(card().apply {
            addView(label("Nenhuma conferência cadastrada.", 14f, white))
            addView(label("Importe o XML da nota ou crie uma conferência de recebimento manual.", 12f, muted), lp(7))
        }, lp(12))
        all.forEach { doc -> listArea.addView(conferenceCard(doc), lp(11)) }
    }

    private fun conferenceCard(doc: ConferenciaEstoque): View {
        val c = card()
        val statusColor = when (doc.situacao) {
            EstoqueConferenciaStore.APROVADA -> Color.rgb(110, 224, 160)
            EstoqueConferenciaStore.DEVOLVIDA -> Color.rgb(255, 191, 105)
            else -> orange
        }
        c.addView(label("${doc.situacao}  •  ${doc.origem}", 12f, statusColor, true))
        c.addView(label("${doc.documento} • ${doc.linhas.size} itens", 17f, white, true), lp(6))
        c.addView(label("Conferente: ${doc.funcionario}\n${doc.fornecedor}\n${date(doc.criadoEm)}", 11f, muted), lp(5))
        if (doc.observacao.isNotBlank()) c.addView(label("Observações: ${doc.observacao}", 12f, muted), lp(7))
        doc.linhas.forEach { line ->
            val diff = line.contado != line.previsto
            c.addView(label("${line.numero}. ${line.produto}", 13f, white, true), lp(11))
            c.addView(label("Previsto: ${line.previsto}  •  Conferido: ${line.contado}" +
                if (doc.situacao == EstoqueConferenciaStore.APROVADA) "  •  Aprovado: ${line.aprovado}" else "",
                12f, if (diff) Color.rgb(255, 183, 65) else muted), lp(3))
            if (diff) c.addView(label("⚠ Divergência na contagem", 11f, orange, true))
        }
        if (doc.eventos.isNotEmpty()) {
            c.addView(label("HISTÓRICO", 12f, orange, true), lp(13))
            doc.eventos.takeLast(6).forEach { event ->
                c.addView(label("${date(event.data)} · ${event.autor}\n${event.acao}: ${event.detalhe}", 11f, muted), lp(5))
            }
        }
        when {
            doc.situacao == EstoqueConferenciaStore.PENDENTE && ceo() -> {
                c.addView(button("✓ CONFERIR E APROVAR ENTRADA", true) { approveDialog(doc) }, lp(14))
                c.addView(button("↶ DEVOLVER AO FUNCIONÁRIO", false) { returnDialog(doc) }, lp(8))
            }
            doc.situacao == EstoqueConferenciaStore.DEVOLVIDA &&
                (ceo() || doc.funcionario.equals(SessaoRcar.atual(this)?.email, true)) -> {
                c.addView(button("↻ CORRIGIR CONTAGEM E REENVIAR", true) { resubmitDialog(doc) }, lp(14))
            }
            doc.situacao == EstoqueConferenciaStore.APROVADA && ceo() -> {
                c.addView(button("✎ AJUSTAR ESTOQUE COM AUDITORIA", false) { adjustmentDialog(doc) }, lp(14))
            }
        }
        return c
    }

    private fun approveDialog(doc: ConferenciaEstoque) {
        val fields = doc.linhas.map { line -> numeric("${line.produto} • Conferido: ${line.contado}", line.contado.toString()) }
        val reason = field("Justificativa se alterar quantidade (obrigatória)")
        val box = formBox("QUANTIDADES QUE ENTRARÃO NO ESTOQUE")
        doc.linhas.forEachIndexed { index, line ->
            box.addView(label("${line.produto}  •  Previsto ${line.previsto}  •  Contado ${line.contado}", 12f, white), lp(8))
            box.addView(fields[index], lp(4))
        }
        box.addView(reason, lp(14))
        dialog("Aprovação final do CEO", box, "APROVAR E LANÇAR") {
            val approved = fields.map { it.text.toString().toIntOrNull() ?: error("Informe todas as quantidades inteiras.") }
            val result = EstoqueConferenciaStore.approve(this, doc.id, approved, reason.text.toString())
            notice(result); renderList()
        }
    }
    private fun returnDialog(doc: ConferenciaEstoque) {
        val reason = field("Ex.: NF mostra 10 unidades, chegaram 8; recontar.").apply { minLines = 2 }
        val box = formBox("JUSTIFICATIVA DA DEVOLUÇÃO")
        box.addView(reason)
        dialog("Devolver conferência", box, "DEVOLVER") {
            EstoqueConferenciaStore.returnToEmployee(this, doc.id, reason.text.toString())
            notice("Devolvida ao funcionário responsável."); renderList()
        }
    }
    private fun resubmitDialog(doc: ConferenciaEstoque) {
        val fields = doc.linhas.map { numeric("Nova contagem", it.contado.toString()) }
        val chosenTargets = doc.linhas.map { it.existente }.toMutableList()
        val targetNames = doc.linhas.map { line ->
            label("Destino: ${line.existente ?: "Criar novo: ${line.produto}"}", 11f, muted)
        }
        val note = field("Explique o que foi corrigido")
        val box = formBox("RECONTAR E REVISAR VÍNCULOS")
        doc.linhas.forEachIndexed { i, line ->
            box.addView(label("${line.produto} • Previsto: ${line.previsto}", 12f, white), lp(9))
            box.addView(fields[i], lp(4))
            box.addView(targetNames[i], lp(5))
            box.addView(button("ALTERAR PRODUTO VINCULADO", false) {
                val available = AdminLocalStore.products(this)
                val allowNew = doc.origem == "NFE"
                val labels = (if (allowNew) listOf("+ CRIAR PRODUTO NOVO") else emptyList()) + available.map { it.nome }
                AlertDialog.Builder(this).setTitle("Vincular item da linha ${line.numero}")
                    .setItems(labels.toTypedArray()) { _, selected ->
                        val dest = if (allowNew && selected == 0) null else available[selected - (if (allowNew) 1 else 0)].nome
                        chosenTargets[i] = dest
                        targetNames[i].text = "Destino: ${dest ?: "Criar novo: ${line.produto}"}"
                    }.setNegativeButton("Cancelar", null).show()
            }, lp(4))
        }
        box.addView(note, lp(13))
        dialog("Reenviar ao CEO", box, "REENVIAR") {
            val updated = doc.linhas.mapIndexed { index, original ->
                val qty = fields[index].text.toString().toIntOrNull() ?: error("Preencha as quantidades inteiras.")
                original.copy(contado = qty, aprovado = qty, existente = chosenTargets[index])
            }
            EstoqueConferenciaStore.resubmitLines(this, doc.id, updated, note.text.toString())
            notice("Conferência corrigida e reenviada para análise do CEO."); renderList()
        }
    }

    private fun adjustmentDialog(doc: ConferenciaEstoque) {
        val items = doc.linhas.map { it.produto }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Selecionar produto para ajuste")
            .setItems(items) { _, index ->
                val amount = field("Ajuste: +5 para somar ou -3 para retirar").apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                }
                val reason = field("Motivo obrigatório da correção")
                val box = formBox("CORREÇÃO APÓS APROVAÇÃO")
                box.addView(label("${doc.linhas[index].produto}\nAjusta somente o saldo atual e registra a auditoria.", 12f, muted))
                box.addView(amount, lp(12)); box.addView(reason, lp(9))
                dialog("Ajuste autorizado pelo CEO", box, "CONFIRMAR AJUSTE") {
                    val delta = amount.text.toString().trim().toIntOrNull() ?: error("Informe um número inteiro com sinal.")
                    EstoqueConferenciaStore.correctStock(this, doc.id, index, delta, reason.text.toString())
                    notice("Saldo ajustado e evento registrado."); renderList()
                }
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun manualForm() {
        manualLines.clear()
        val box = formBox("RECEBIMENTO MANUAL DE PEÇAS")
        val reference = field("NF, compra ou referência da entrega")
        val note = field("Observações, embalagem avariada ou divergências")
        val selectedName = label("Nenhuma peça selecionada", 13f, muted)
        val expected = numeric("Quantidade prevista", "")
        val counted = numeric("Quantidade realmente recebida", "")
        var chosen: AdminProduto? = null
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(reference)
        box.addView(button("ESCOLHER PEÇA DO ESTOQUE", false) {
            val products = AdminLocalStore.products(this)
            AlertDialog.Builder(this).setTitle("Selecione o produto cadastrado")
                .setItems(products.map { it.nome }.toTypedArray()) { _, idx ->
                    chosen = products[idx]; selectedName.text = products[idx].nome
                }.show()
        }, lp(10))
        box.addView(selectedName, lp(6))
        box.addView(expected, lp(9)); box.addView(counted, lp(8))
        box.addView(button("+ ADICIONAR PEÇA À CONFERÊNCIA", false) {
            runCatching {
                val product = chosen ?: error("Escolha o produto cadastrado.")
                val forecast = expected.text.toString().toIntOrNull() ?: error("Informe a quantidade prevista.")
                val delivered = counted.text.toString().toIntOrNull() ?: error("Informe a quantidade conferida.")
                require(forecast in 0..1_000_000 && delivered in 0..1_000_000) { "Quantidade inválida." }
                require(manualLines.none { it.existente.equals(product.nome, true) }) { "Esse produto já foi adicionado." }
                manualLines.add(ConferenciaLinha(manualLines.size + 1, product.nome, product.nome, product.categoria,
                    product.sku, product.codigoBarras, product.precoCusto, product.preco, forecast, delivered))
                rows.addView(label("✓ ${product.nome}: $delivered de $forecast unidades", 12f, white), lp(6))
                expected.text.clear(); counted.text.clear()
            }.onFailure { errorMessage(it) }
        }, lp(9))
        box.addView(rows, lp(10))
        box.addView(note, lp(13))
        val purchaseId = intent.getStringExtra("REPOSICAO_ID").orEmpty()
        val productName = intent.getStringExtra("REPOSICAO_PRODUTO").orEmpty()
        val requested = intent.getIntExtra("REPOSICAO_QTD", 0)
        if (purchaseId.isNotBlank()) reference.setText("REPO:$purchaseId")
        if (productName.isNotBlank()) {
            chosen = AdminLocalStore.products(this).firstOrNull { it.nome.equals(productName, true) }
            if (chosen != null) selectedName.text = chosen!!.nome
        }
        if (requested > 0) expected.setText(requested.toString())
        dialog("Nova conferência de recebimento", box, "ENVIAR AO CEO") {
            val saved = EstoqueConferenciaStore.submitManual(this, reference.text.toString(), note.text.toString(), manualLines.toList())
            notice("Conferência ${saved.documento} enviada. Estoque não alterado."); renderList()
        }
    }

    private fun dialog(title: String, body: View, positive: String, action: () -> Unit) {
        val scroll = ScrollView(this)
        scroll.addView(body)
        val dialog = AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setNegativeButton("Cancelar", null).setPositiveButton(positive, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching { action() }.onSuccess { dialog.dismiss() }.onFailure { errorMessage(it) }
            }
        }
        dialog.show()
    }
    private fun formBox(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(12))
        addView(label(title, 13f, 0xFFAA5B13.toInt(), true))
    }
    private fun label(s: String, size: Float, tint: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(tint); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText; textSize = 13f; setSingleLine(false)
        setTextColor(white); setHintTextColor(Color.GRAY)
        setPadding(dp(10), dp(9), dp(10), dp(9)); background = shape(0xFF171717.toInt(), 0xFF444444.toInt())
    }
    private fun numeric(hint: String, value: String) = field(hint).apply {
        inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine(true); setText(value)
    }
    private fun button(s: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        text = s; textSize = 12f; isAllCaps = false; setTextColor(white)
        background = shape(if (primary) orange else 0xFF252525.toInt(), orange)
        setOnClickListener { click() }
    }
    /** Barra inferior flutuante no mesmo padrão preto/laranja da loja. */
    private fun floatingFooter(): View {
        val container = LinearLayout(this).apply {
            setPadding(dp(16), dp(5), dp(16), dp(13))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = shape(0xFF101010.toInt(), orange)
            elevation = dp(9).toFloat()
        }
        listOf("⌂  Painel", "▣  NF-e", "✓  Conferências").forEachIndexed { index, title ->
            val chip = TextView(this).apply {
                text = title; gravity = Gravity.CENTER; textSize = 12f
                typeface = if (index == 2) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (index == 2) orange else white)
                setPadding(dp(5), dp(10), dp(5), dp(10))
                setOnClickListener {
                    when (index) {
                        0 -> finish()
                        1 -> startActivity(android.content.Intent(this@MainActivityConferenciaEstoque, MainActivityImportarNota::class.java))
                        else -> scroll.smoothScrollTo(0, 0)
                    }
                }
            }
            bar.addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        container.addView(bar, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        return container
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = shape(0xFF171717.toInt(), 0xFF744210.toInt())
    }
    private fun shape(fill: Int, stroke: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(16).toFloat(); setStroke(dp(1), stroke)
    }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun date(time: Long) = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(time))
    private fun notice(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun errorMessage(err: Throwable) = AlertDialog.Builder(this).setTitle("Conferência não concluída")
        .setMessage(err.message ?: "Verifique os dados e tente novamente.").setPositiveButton("OK", null).show()
}
