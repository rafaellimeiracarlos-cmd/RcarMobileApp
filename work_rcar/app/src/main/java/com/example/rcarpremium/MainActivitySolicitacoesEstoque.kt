package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Central CEO para aprovar pausas de anúncios, compras pedidas pela equipe e recebimentos. */
class MainActivitySolicitacoesEstoque : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = RcarLocalStore.currentEmail(this).orEmpty()
        if (!UsuarioLocalRepository(this).isCeo(email)) {
            Toast.makeText(this, "Apenas o CEO pode aprovar solicitações.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        render()
    }
    override fun onResume() { super.onResume(); if (RcarLocalStore.currentEmail(this)?.let { UsuarioLocalRepository(this).isCeo(it) } == true) render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)); setBackgroundColor(0xFF0A0A0A.toInt()) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(button("←  VOLTAR AO PAINEL", false).apply { setOnClickListener { finish() } }, lp())
        content.addView(text("SOLICITAÇÕES DA EQUIPE", 23f, orange, true), lp(14))
        content.addView(text("Aprove pausas e compras. O recebimento físico só aumenta estoque após conferência supervisionada.", 13f, Color.LTGRAY), lp(5))

        val requests = SolicitacaoEquipeStore.todas(this)
        val pending = requests.count { it.status == SolicitacaoEquipeStore.STATUS_PENDENTE }
        val toReceive = requests.count { it.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && it.status == SolicitacaoEquipeStore.STATUS_APROVADO }
        content.addView(text("$pending pendente(s) • $toReceive compra(s) aprovada(s) para receber", 13f, Color.WHITE, true), lp(12))

        if (requests.isEmpty()) content.addView(infoCard("Nenhuma solicitação recebida ainda.", "As solicitações de pausa de anúncio e reposição enviadas pelos funcionários aparecerão aqui."), lp(10))
        requests.forEach { request ->
            val card = card()
            val titulo = when (request.tipo) {
                SolicitacaoEquipeStore.PAUSAR_ANUNCIO -> "PAUSA DE ANÚNCIO"
                SolicitacaoEquipeStore.REPOSICAO_ESTOQUE -> "COMPRA / REPOSIÇÃO DE ESTOQUE"
                else -> request.tipo.replace('_', ' ')
            }
            card.addView(text(titulo, 11f, orange, true))
            card.addView(text(request.produto.ifBlank { "Solicitação de acesso" }, 17f, Color.WHITE, true), lp(5))
            card.addView(text("Funcionário: ${request.solicitante}", 12f, Color.LTGRAY), lp(4))
            if (request.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE) card.addView(text("Quantidade solicitada: ${request.quantidade} unidade(s)", 13f, Color.WHITE), lp(4))
            card.addView(text("Enviada em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(request.criadoEm))}", 11f, Color.GRAY), lp(4))
            card.addView(text("Status: ${labelStatus(request)}", 12f, if (request.status == SolicitacaoEquipeStore.STATUS_PENDENTE) orange else Color.LTGRAY, true), lp(7))

            when {
                request.tipo == SolicitacaoEquipeStore.PAUSAR_ANUNCIO && request.status == SolicitacaoEquipeStore.STATUS_PENDENTE -> {
                    card.addView(button("✓  APROVAR PAUSA", true).apply { setOnClickListener { aprovarPausa(request) } }, lp(10))
                    card.addView(button("✕  REJEITAR", false).apply { setOnClickListener { negar(request) } }, lp(6))
                }
                request.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && request.status == SolicitacaoEquipeStore.STATUS_PENDENTE -> {
                    card.addView(button("✓  APROVAR COMPRA", true).apply { setOnClickListener { aprovarCompra(request) } }, lp(10))
                    card.addView(button("✕  REJEITAR PEDIDO", false).apply { setOnClickListener { negar(request) } }, lp(6))
                }
                request.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && request.status == SolicitacaoEquipeStore.STATUS_APROVADO -> {
                    card.addView(button("□  CONFERIR RECEBIMENTO", true).apply { setOnClickListener { marcarRecebida(request) } }, lp(10))
                }
            }
            content.addView(card, lp(10))
        }

        content.addView(text("ESTOQUE BAIXO", 22f, orange, true), lp(24))
        content.addView(text("Produtos com quantidade igual ou inferior ao mínimo configurado no cadastro.", 13f, Color.LTGRAY), lp(5))
        val lowStock = AdminLocalStore.products(this).filter { it.ativo && it.estoque <= it.estoqueMinimo }.sortedBy { it.estoque - it.estoqueMinimo }
        if (lowStock.isEmpty()) content.addView(infoCard("Nenhum item abaixo do nível mínimo.", "Quando um produto atingir o mínimo, ele será mostrado automaticamente aqui."), lp(10))
        lowStock.forEach { product ->
            val card = card()
            val approvedOrder = requests.any {
                it.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE &&
                    it.produto.equals(product.nome, true) && it.status == SolicitacaoEquipeStore.STATUS_APROVADO
            }
            card.addView(text(product.nome, 16f, Color.WHITE, true))
            card.addView(text("Estoque: ${product.estoque} • Mínimo: ${product.estoqueMinimo} • Custo unitário: ${money(product.precoCusto)}", 12f, Color.LTGRAY), lp(5))
            if (approvedOrder) {
                card.addView(text("Há uma compra aprovada deste produto. Faça a conferência com a quantidade física recebida.", 12f, orange), lp(8))
            } else {
                card.addView(button("✓  ABRIR CONFERÊNCIA DE RECEBIMENTO", false).apply {
                    setOnClickListener {
                        startActivity(android.content.Intent(this@MainActivitySolicitacoesEstoque, MainActivityConferenciaEstoque::class.java))
                    }
                }, lp(9))
            }
            content.addView(card, lp(9))
        }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun aprovarPausa(request: SolicitacaoEquipe) {
        val paused = AdminLocalStore.setAnuncioAtivo(this, request.produto, false)
        if (!paused) { Toast.makeText(this, "Não foi possível localizar o produto para pausar o anúncio.", Toast.LENGTH_LONG).show(); return }
        SolicitacaoEquipeStore.atualizarStatus(this, request.id, SolicitacaoEquipeStore.STATUS_APROVADO)
        Toast.makeText(this, "Pausa aprovada. O produto continua no estoque, mas deixa de aparecer no catálogo público.", Toast.LENGTH_LONG).show()
        render()
    }

    private fun aprovarCompra(request: SolicitacaoEquipe) {
        SolicitacaoEquipeStore.atualizarStatus(this, request.id, SolicitacaoEquipeStore.STATUS_APROVADO)
        Toast.makeText(this, "Compra aprovada. Ao receber os produtos, marque como recebida para atualizar o estoque.", Toast.LENGTH_LONG).show()
        render()
    }

    private fun marcarRecebida(request: SolicitacaoEquipe) {
        AlertDialog.Builder(this)
            .setTitle("Conferir antes da entrada")
            .setMessage("A compra foi aprovada, mas o recebimento físico requer conferência e aprovação separadas. Crie a conferência manual informando ${request.produto}, ${request.quantidade} unidades previstas e a contagem real. O estoque não muda até o CEO aprovar.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir conferência") { _, _ ->
                startActivity(android.content.Intent(this, MainActivityConferenciaEstoque::class.java).apply {
                    putExtra("REPOSICAO_ID", request.id)
                    putExtra("REPOSICAO_PRODUTO", request.produto)
                    putExtra("REPOSICAO_QTD", request.quantidade)
                })
            }.show()
    }

    private fun negar(request: SolicitacaoEquipe) {
        AlertDialog.Builder(this).setTitle("Rejeitar solicitação?").setMessage("O pedido de ${request.produto} será marcado como recusado.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Rejeitar") { _, _ ->
                SolicitacaoEquipeStore.atualizarStatus(this, request.id, SolicitacaoEquipeStore.STATUS_NEGADO)
                Toast.makeText(this, "Solicitação rejeitada.", Toast.LENGTH_SHORT).show(); render()
            }.show()
    }

    private fun pedirQuantidade(nome: String, inicial: Int, onConfirm: (Int) -> Unit) {
        val input = EditText(this).apply { hint = "Quantidade recebida"; inputType = InputType.TYPE_CLASS_NUMBER; setText(inicial.toString()); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val wrapper = LinearLayout(this).apply { setPadding(dp(20), dp(6), dp(20), 0); addView(input) }
        AlertDialog.Builder(this).setTitle("Registrar entrada • $nome").setView(wrapper)
            .setNegativeButton("Cancelar", null).setPositiveButton("Confirmar") { _, _ ->
                val qty = input.text.toString().toIntOrNull()
                if (qty == null || qty <= 0) Toast.makeText(this, "Use quantidade maior que zero.", Toast.LENGTH_SHORT).show() else onConfirm(qty)
            }.show()
    }

    private fun labelStatus(r: SolicitacaoEquipe) = when (r.status) {
        SolicitacaoEquipeStore.STATUS_PENDENTE -> "Aguardando CEO"
        SolicitacaoEquipeStore.STATUS_APROVADO -> if (r.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE) "Compra aprovada • aguardando recebimento" else "Aprovada"
        SolicitacaoEquipeStore.STATUS_NEGADO -> "Rejeitada"
        SolicitacaoEquipeStore.STATUS_RECEBIDO -> "Recebida e estoque atualizado"
        else -> r.status
    }
    private fun infoCard(title: String, sub: String) = card().apply { addView(text(title, 15f, Color.WHITE, true)); addView(text(sub, 12f, Color.LTGRAY), lp(5)) }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = GradientDrawable().apply { setColor(0xFF191919.toInt()); cornerRadius = dp(16).toFloat(); setStroke(dp(1), 0xFF55320F.toInt()) } }
    private fun text(v: String, s: Float, c: Int, bold: Boolean = false) = TextView(this).apply { text = v; textSize = s; setTextColor(c); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun button(v: String, primary: Boolean) = Button(this).apply { text = v; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE); background = GradientDrawable().apply { setColor(if (primary) orange else 0xFF292929.toInt()); cornerRadius = dp(13).toFloat(); setStroke(dp(1), if (primary) orange else 0xFF555555.toInt()) } }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (top > 0) topMargin = dp(top) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Double) = String.format(Locale("pt", "BR"), "R$ %.2f", v)
}
