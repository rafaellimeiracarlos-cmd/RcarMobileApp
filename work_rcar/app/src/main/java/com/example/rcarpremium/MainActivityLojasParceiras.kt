package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** O CEO controla o cadastro de lojas e o vínculo de anúncios a vendedores. */
class MainActivityLojasParceiras : RcarBaseActivity() {
    private lateinit var container: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = RcarLocalStore.currentEmail(this)
        if (email == null || !UsuarioLocalRepository(this).isCeo(email)) {
            Toast.makeText(this, "Apenas o CEO pode gerenciar lojas parceiras.", Toast.LENGTH_LONG).show(); finish(); return
        }
        val (root, body) = MarketplaceUi.page(this, "LOJAS E VENDEDORES",
            "Gestão administrativa do marketplace • informações armazenadas localmente.",
            "⌂  INÍCIO DA LOJA", { finish() }, {
                startActivity(Intent(this, MainActivitycomprasonline::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })
        body.addView(MarketplaceUi.button(this, "＋ CADASTRAR LOJA", true).apply { setOnClickListener { criarOuEditarLoja(null) } }, MarketplaceUi.lp(this, 15))
        body.addView(MarketplaceUi.button(this, "⚙ CONFIGURAR SERVIDOR E IA").apply { setOnClickListener { configurarBackend() } }, MarketplaceUi.lp(this, 7))
        body.addView(MarketplaceUi.text(this, "LOJAS CADASTRADAS", 17f, Color.WHITE, true), MarketplaceUi.lp(this, 21))
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(container, MarketplaceUi.lp(this, 8))
        setContentView(root)
        render()
    }
    override fun onResume() { super.onResume(); if (::container.isInitialized) render() }
    private fun render() {
        container.removeAllViews()
        MarketplaceStore.lojas(this).forEach { loja ->
            val card = MarketplaceUi.card(this)
            card.addView(MarketplaceUi.text(this, loja.nome, 17f, Color.WHITE, true))
            card.addView(MarketplaceUi.text(this, "${loja.cidade} / ${loja.uf} • ${if (loja.ativa) "ATIVA" else "PAUSADA"}", 12f, MarketplaceUi.ORANGE), MarketplaceUi.lp(this, 5))
            val total = AdminLocalStore.products(this).count { MarketplaceStore.lojaDeProduto(this, it) == loja.id }
            card.addView(MarketplaceUi.text(this, "$total produtos vinculados • ${if (loja.latitude != null) "Coordenadas configuradas" else "Sem coordenadas"}", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 5))
            card.addView(MarketplaceUi.button(this, "ATRIBUIR PRODUTO", true).apply { setOnClickListener { escolherProduto(loja.id) } }, MarketplaceUi.lp(this, 9))
            card.addView(MarketplaceUi.button(this, "✎ EDITAR DADOS / LOCALIZAÇÃO").apply { setOnClickListener { criarOuEditarLoja(loja) } }, MarketplaceUi.lp(this, 6))
            if (loja.id != MarketplaceStore.LOJA_PRINCIPAL_ID) {
                card.addView(MarketplaceUi.button(this, if (loja.ativa) "PAUSAR LOJA" else "REATIVAR LOJA").apply {
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivityLojasParceiras)
                            .setTitle(if (loja.ativa) "Pausar ${loja.nome}?" else "Reativar ${loja.nome}?")
                            .setMessage("Os anúncios vinculados ${if (loja.ativa) "deixarão de aparecer" else "voltarão a aparecer"} na vitrine local.")
                            .setNegativeButton("Cancelar", null).setPositiveButton("Confirmar") { _, _ ->
                                MarketplaceStore.definirAtiva(this@MainActivityLojasParceiras, loja.id, !loja.ativa); render()
                            }.show()
                    }
                }, MarketplaceUi.lp(this, 6))
            }
            container.addView(card, MarketplaceUi.lp(this, 8))
        }
    }
    private fun escolherProduto(lojaId: String) {
        val products = AdminLocalStore.products(this)
        if (products.isEmpty()) { Toast.makeText(this, "Cadastre produtos primeiro.", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("Vincular produto à loja")
            .setItems(products.map { "${it.nome} • ${it.sku.ifBlank { "sem SKU" }}" }.toTypedArray()) { _, position ->
                MarketplaceStore.vincular(this, products[position], lojaId)
                Toast.makeText(this, "Produto vinculado à loja.", Toast.LENGTH_SHORT).show(); render()
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun criarOuEditarLoja(existing: LojaParceira?) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(MarketplaceUi.dp(this@MainActivityLojasParceiras, 18), MarketplaceUi.dp(this@MainActivityLojasParceiras, 14), MarketplaceUi.dp(this@MainActivityLojasParceiras, 18), MarketplaceUi.dp(this@MainActivityLojasParceiras, 10)); background = RcarDialogUi.panel(this@MainActivityLojasParceiras) }
        fun input(hint: String, numeric: Boolean = false): EditText = EditText(this).apply {
            this.hint=hint; textSize=14f
            if(numeric) inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            RcarDialogUi.styleInput(this)
            form.addView(this, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = MarketplaceUi.dp(this@MainActivityLojasParceiras, 8) })
        }
        val nome=input("Nome da loja *")
        val cidade=input("Cidade *")
        val uf=input("UF (SP, RJ, MG...) *")
        val descricao=input("Descrição (opcional)")
        val lat=input("Latitude, se conhecida (opcional)",true)
        val lng=input("Longitude, se conhecida (opcional)",true)
        if (existing != null) {
            nome.setText(existing.nome); cidade.setText(existing.cidade); uf.setText(existing.uf); descricao.setText(existing.descricao)
            existing.latitude?.let { lat.setText(it.toString()) }; existing.longitude?.let { lng.setText(it.toString()) }
        }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Nova loja parceira" else "Editar loja parceira").setView(RcarDialogUi.wrapPanel(this, form))
            .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create().also { dialog ->
                dialog.setOnShowListener {
                    RcarDialogUi.styleDialog(dialog)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try {
                            fun coord(value: String) = value.trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
                            if (lat.text.isNotBlank() && coord(lat.text.toString()) == null) throw IllegalArgumentException("Latitude inválida.")
                            if (lng.text.isNotBlank() && coord(lng.text.toString()) == null) throw IllegalArgumentException("Longitude inválida.")
                            if (existing == null) {
                                MarketplaceStore.criar(this, nome.text.toString(), cidade.text.toString(), uf.text.toString(), descricao.text.toString(), coord(lat.text.toString()), coord(lng.text.toString()))
                            } else {
                                MarketplaceStore.atualizar(this, existing.copy(nome = nome.text.toString(), cidade = cidade.text.toString(), uf = uf.text.toString(), descricao = descricao.text.toString(), latitude = coord(lat.text.toString()), longitude = coord(lng.text.toString())))
                            }
                            dialog.dismiss(); render()
                        } catch (e: IllegalArgumentException) { nome.error=e.message; Toast.makeText(this,e.message,Toast.LENGTH_LONG).show() }
                    }
                }
                dialog.show()
            }
    }
    private fun configurarBackend() {
        val field = EditText(this).apply { hint="https://seu-servidor.com"; setText(MarketplaceStore.urlBackend(this@MainActivityLojasParceiras)); RcarDialogUi.styleInput(this) }
        AlertDialog.Builder(this).setTitle("Servidor HTTPS para IA")
            .setMessage("Informe somente a URL do seu backend publicado, sem chave de API. Uma URL vazia mantém as sugestões locais.")
            .setView(RcarDialogUi.wrapPanel(this, LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = RcarDialogUi.panel(this@MainActivityLojasParceiras); setPadding(MarketplaceUi.dp(this@MainActivityLojasParceiras,18), MarketplaceUi.dp(this@MainActivityLojasParceiras,14), MarketplaceUi.dp(this@MainActivityLojasParceiras,18), MarketplaceUi.dp(this@MainActivityLojasParceiras,10)); addView(field) })).setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null).create().also { dialog ->
                dialog.setOnShowListener {
                    RcarDialogUi.styleDialog(dialog)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try { MarketplaceStore.salvarUrlBackend(this, field.text.toString()); Toast.makeText(this, "Configuração salva.", Toast.LENGTH_SHORT).show(); dialog.dismiss() }
                        catch (e: IllegalArgumentException) { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
                    }
                }
                dialog.show()
            }
    }
}
