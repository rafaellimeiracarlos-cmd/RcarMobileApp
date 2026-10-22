package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.net.Uri
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

/** Central privada: notas de ENTRADA (gerente/CEO) e SAÍDA (somente CEO). */
class MainActivityCentralNotas : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    private val white = Color.WHITE
    private val muted = Color.rgb(179, 182, 188)
    private lateinit var list: LinearLayout
    private var pendingPedido = ""
    private var pendingNumero = ""
    private var pendingChave = ""
    private val pickSaleNote = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { ArquivoNotasStore.anexarNotaPedido(this, pendingPedido, uri, pendingNumero, pendingChave) }
                .onSuccess { Toast.makeText(this, "NF do pedido arquivada para o cliente.", Toast.LENGTH_LONG).show(); render() }
                .onFailure { message(it.message ?: "Erro ao arquivar documento") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessaoRcar.atual(this)
        val repo = UsuarioLocalRepository(this)
        if (session == null || !(repo.isCeo(session.email) || repo.temPermissao(session.email, "lancar_notas") ||
                    repo.temPermissao(session.email, "conferir_recebimento"))) {
            Toast.makeText(this, "Área restrita à gestão de notas.", Toast.LENGTH_LONG).show(); finish(); return
        }
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(9,9,9)) }
        val scroller = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(38)) }
        content.addView(ImageView(this).apply { setImageResource(R.drawable.imglogo); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(-1, dp(67)))
        content.addView(label("CENTRAL DE NOTAS FISCAIS", 23f, orange, true), lp(12))
        content.addView(label("Arquivo privativo • código, responsável, data e hora • documentos organizados por mês", 12f, muted), lp(5))
        content.addView(button("← VOLTAR AO PAINEL", false) { finish() }, lp(12))
        content.addView(button("+ LANÇAR NOTA DE ENTRADA XML / QR / LINK", true) {
            startActivity(android.content.Intent(this, MainActivityImportarNota::class.java))
        }, lp(12))
        if (repo.isCeo(session.email)) {
            content.addView(button("ANEXAR NF-e/DANFE A PEDIDO DO CLIENTE", false) { askSaleNote() }, lp(8))
        }
        content.addView(label(if (repo.isCeo(session.email)) "ALERTAS PARA O CEO" else "MEUS LANÇAMENTOS", 17f, white, true), lp(18))
        content.addView(label("Notas lançadas aguardam conferência e aprovação de estoque. O anexo de entrada nunca é exibido como NF-e de compra do cliente.", 12f, muted), lp(5))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list, lp(12))
        scroller.addView(content)
        outer.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)
        render()
    }
    override fun onResume() { super.onResume(); if (::list.isInitialized) render() }
    private fun render() {
        val me = SessaoRcar.atual(this)?.email.orEmpty()
        val ceo = UsuarioLocalRepository(this).isCeo(me)
        list.removeAllViews()
        val docs = ArquivoNotasStore.listar(this).filter { ceo || (it.tipo == ArquivoNotasStore.ENTRADA && it.lancadoPor.equals(me, true)) }
        if (docs.isEmpty()) list.addView(label("Nenhum documento registrado neste aparelho.", 13f, muted))
        if (ceo) {
            val unread = docs.count { it.tipo == ArquivoNotasStore.ENTRADA && it.lidaPeloCeoEm == 0L }
            list.addView(label("${if (unread > 0) "●" else "✓"}  $unread nota(s) nova(s) lançada(s) pela equipe", 15f, if (unread > 0) orange else white, true), lp(5))
        }
        val byMonth = docs.groupBy { java.text.SimpleDateFormat("yyyy / MM", java.util.Locale("pt", "BR")).format(java.util.Date(it.lancadoEm)) }
        byMonth.forEach { (month, items) ->
            list.addView(label("ARQUIVO  •  $month", 14f, orange, true), lp(17))
            items.forEach { n ->
                val conf = EstoqueConferenciaStore.list(this).firstOrNull { it.id == n.conferenciaId }
                val status = if (n.tipo == ArquivoNotasStore.SAIDA) "DISPONÍVEL AO CLIENTE" else conf?.situacao ?: "CONFERÊNCIA NÃO LOCALIZADA"
                val panel = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(13), dp(14), dp(13))
                    background = shape(Color.rgb(23,23,24), Color.rgb(112,66,25), 16)
                }
                panel.addView(label("${if (n.tipo == ArquivoNotasStore.ENTRADA) "ENTRADA" else "VENDA"}  •  NF ${n.numero.ifBlank { "S/N" }}", 15f, white, true))
                panel.addView(label("Chave/código: ${n.chave.ifBlank { n.sha256.take(18) + "…" }}", 11f, muted), lp(6))
                panel.addView(label("Registrada: ${PedidoLocalStore.dataFormatada(n.lancadoEm)}", 12f, white), lp(4))
                panel.addView(label("Responsável: ${n.lancadoPor}", 12f, muted), lp(4))
                panel.addView(label("Origem: ${n.fornecedor} ${if (n.pedidoId.isNotBlank()) "• Pedido #${n.pedidoId}" else ""}", 11f, muted), lp(4))
                panel.addView(label("Status: $status${if (n.tipo == ArquivoNotasStore.ENTRADA && n.lidaPeloCeoEm == 0L) " • NÃO LIDA PELO CEO" else ""}", 12f, orange, true), lp(7))
                panel.addView(button(if (n.arquivo.isNotBlank()) "ABRIR DOCUMENTO" else "DOCUMENTO NÃO DISPONÍVEL", false) {
                    runCatching { ArquivoNotasStore.abrir(this, n) }.onFailure { message(it.message ?: "Sem visualizador compatível") }
                }, lp(8))
                if (ceo && n.tipo == ArquivoNotasStore.ENTRADA && n.lidaPeloCeoEm == 0L) {
                    panel.addView(button("MARCAR COMO LIDA PELO CEO", true) {
                        runCatching { ArquivoNotasStore.marcarLida(this, n.id) }.onSuccess { render() }.onFailure { message(it.message.orEmpty()) }
                    }, lp(8))
                }
                list.addView(panel, lp(9))
            }
        }
    }
    private fun askSaleNote() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(8)); background = shape(Color.rgb(15,17,23), orange, 18) }
        val pedido = EditText(this).apply { hint = "Número do pedido"; RcarDialogUi.styleInput(this) }
        val nf = EditText(this).apply { hint = "Número da NF-e"; RcarDialogUi.styleInput(this) }
        val key = EditText(this).apply { hint = "Chave de acesso (44 dígitos, opcional)"; RcarDialogUi.styleInput(this) }
        listOf(pedido, nf, key).forEachIndexed { index, it -> form.addView(it, lp(if (index == 0) 10 else 8)) }
        val dialog = AlertDialog.Builder(this).setTitle("Vincular nota fiscal de venda")
            .setMessage("Somente o CEO pode anexar a nota do pedido. O cliente verá esse documento no acompanhamento da própria compra.")
            .setView(RcarDialogUi.wrapPanel(this, form)).setNegativeButton("Cancelar", null)
            .setPositiveButton("Selecionar XML ou PDF", null).create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                pendingPedido = pedido.text.toString().trim()
                pendingNumero = nf.text.toString().trim()
                pendingChave = key.text.toString().trim()
                if (PedidoLocalStore.listar(this).none { it.id == pendingPedido }) message("Pedido não encontrado.")
                else { dialog.dismiss(); pickSaleNote.launch(arrayOf("application/pdf", "application/xml", "text/xml")) }
            }
        }
        dialog.show()
    }
    private fun message(m: String) = AlertDialog.Builder(this).setTitle("Central de notas").setMessage(m).setPositiveButton("OK", null).show()
    private fun label(v: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = v; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun button(v: String, solid: Boolean, cb: () -> Unit) = Button(this).apply {
        text = v; textSize = 12f; isAllCaps = false; setTextColor(white)
        background = shape(if (solid) orange else Color.rgb(35,35,37), orange, 18)
        setOnClickListener { cb() }
    }
    private fun shape(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
