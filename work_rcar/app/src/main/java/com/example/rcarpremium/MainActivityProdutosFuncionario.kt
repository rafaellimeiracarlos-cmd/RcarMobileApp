package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Locale

/** Área operacional: cada funcionário enxerga os produtos associados ao seu e-mail. */
class MainActivityProdutosFuncionario : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    private val bgColor = Color.rgb(10, 10, 10)
    private lateinit var email: String
    private lateinit var repo: UsuarioLocalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = RcarLocalStore.currentEmail(this).orEmpty()
        repo = UsuarioLocalRepository(this)
        if (email.isBlank() || !repo.usuarioAtivo(email) || repo.papel(email) !in UsuarioLocalRepository.PAPEIS_EQUIPE) {
            Toast.makeText(this, "Esta tela é exclusiva para funcionários autenticados.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::email.isInitialized && !isFinishing) render()
    }

    private fun render() {
        if (!::email.isInitialized) return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            setBackgroundColor(bgColor)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(button("←  VOLTAR", false).apply { setOnClickListener { finish() } }, lp())
        content.addView(text("MEUS PRODUTOS", 24f, orange, true), lp(14))
        content.addView(text("Edite os produtos cadastrados por você, registre vendas e envie pedidos para o CEO.", 13f, Color.LTGRAY), lp(5))
        val products = AdminLocalStore.products(this).filter { it.cadastradoPor.equals(email, true) }
        content.addView(text("${products.size} produto(s) associados à sua conta", 13f, Color.WHITE, true), lp(14))

        if (products.isEmpty()) {
            val empty = card()
            empty.addView(text("Nenhum produto está associado ao seu usuário ainda.", 15f, Color.WHITE, true))
            empty.addView(text("Produtos novos cadastrados com esta conta aparecerão aqui. Produtos antigos sem responsável registrado precisam ser identificados pelo CEO.", 12f, Color.LTGRAY), lp(7))
            content.addView(empty, lp(10))
        }

        products.forEach { product ->
            val reqs = SolicitacaoEquipeStore.todas(this).filter {
                it.produto.equals(product.nome, true) && it.solicitante.equals(email, true)
            }
            val currentRequest = reqs.firstOrNull {
                it.status == SolicitacaoEquipeStore.STATUS_PENDENTE ||
                    (it.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && it.status == SolicitacaoEquipeStore.STATUS_APROVADO)
            }
            val card = card()
            card.addView(text(product.nome, 17f, Color.WHITE, true))
            card.addView(text("${product.categoria}  •  ${money(product.preco)}", 13f, Color.LTGRAY), lp(4))
            card.addView(text("Estoque: ${product.estoque} un.  •  Mínimo: ${product.estoqueMinimo} un.", 13f, if (product.estoque <= product.estoqueMinimo) orange else Color.WHITE, true), lp(4))
            val status = when {
                !product.ativo -> "Produto desativado no cadastro"
                !product.anuncioAtivo -> "Anúncio pausado pelo CEO"
                currentRequest != null && currentRequest.tipo == SolicitacaoEquipeStore.PAUSAR_ANUNCIO && currentRequest.status == SolicitacaoEquipeStore.STATUS_PENDENTE -> "Pausa do anúncio aguardando o CEO"
                currentRequest != null && currentRequest.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && currentRequest.status == SolicitacaoEquipeStore.STATUS_PENDENTE -> "Reposição aguardando o CEO (${currentRequest.quantidade} un.)"
                currentRequest != null && currentRequest.tipo == SolicitacaoEquipeStore.REPOSICAO_ESTOQUE && currentRequest.status == SolicitacaoEquipeStore.STATUS_APROVADO -> "Compra aprovada; aguardando recebimento (${currentRequest.quantidade} un.)"
                else -> "Anúncio ativo"
            }
            card.addView(text(status, 12f, if (status.contains("aguardando")) orange else Color.LTGRAY), lp(5))

            val edit = button("✎  EDITAR PRODUTO", true).apply {
                setOnClickListener {
                    if (!repo.temPermissao(email, "cadastrar_produtos")) avisarPermissao("cadastrar_produtos")
                    else startActivity(Intent(this@MainActivityProdutosFuncionario, MainActivityCadastroProduto::class.java).putExtra("EDITAR_PRODUTO_NOME", product.nome))
                }
            }
            val sell = button("＋  REGISTRAR VENDA", false).apply {
                setOnClickListener {
                    if (!repo.temPermissao(email, "registrar_vendas")) avisarPermissao("registrar_vendas")
                    else pedirQuantidade("Registrar venda • ${product.nome}", "Quantidade vendida", 1) { qty ->
                        val error = AdminLocalStore.registrarVendaProduto(this@MainActivityProdutosFuncionario, product.nome, qty, email)
                        if (error == null) {
                            Toast.makeText(this@MainActivityProdutosFuncionario, "Venda registrada e estoque atualizado.", Toast.LENGTH_LONG).show()
                            render()
                        } else Toast.makeText(this@MainActivityProdutosFuncionario, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
            val pause = button(if (!product.anuncioAtivo) "ANÚNCIO JÁ PAUSADO" else "Ⅱ  SOLICITAR PAUSA DO ANÚNCIO", false).apply {
                isEnabled = product.anuncioAtivo && currentRequest?.tipo != SolicitacaoEquipeStore.PAUSAR_ANUNCIO
                alpha = if (isEnabled) 1f else .55f
                setOnClickListener {
                    if (!repo.temPermissao(email, "pausar_anuncios")) avisarPermissao("pausar_anuncios")
                    else if (SolicitacaoEquipeStore.existeAberta(this@MainActivityProdutosFuncionario, SolicitacaoEquipeStore.PAUSAR_ANUNCIO, product.nome, email)) {
                        Toast.makeText(this@MainActivityProdutosFuncionario, "Já existe uma solicitação de pausa em aberto.", Toast.LENGTH_SHORT).show()
                    } else {
                        SolicitacaoEquipeStore.criar(this@MainActivityProdutosFuncionario, SolicitacaoEquipeStore.PAUSAR_ANUNCIO, product.nome, email)
                        Toast.makeText(this@MainActivityProdutosFuncionario, "Solicitação enviada ao CEO. O anúncio só pausa após aprovação.", Toast.LENGTH_LONG).show()
                        render()
                    }
                }
            }
            val restock = button("□  SOLICITAR REPOSIÇÃO", false).apply {
                setOnClickListener {
                    if (!repo.temPermissao(email, "solicitar_reposicao_estoque")) avisarPermissao("solicitar_reposicao_estoque")
                    else pedirQuantidade("Solicitar compra • ${product.nome}", "Quantidade desejada", (product.estoqueMinimo - product.estoque).coerceAtLeast(1)) { qty ->
                        if (SolicitacaoEquipeStore.existeAberta(this@MainActivityProdutosFuncionario, SolicitacaoEquipeStore.REPOSICAO_ESTOQUE, product.nome, email)) {
                            Toast.makeText(this@MainActivityProdutosFuncionario, "Já existe uma solicitação de reposição em aberto.", Toast.LENGTH_LONG).show()
                        } else {
                            SolicitacaoEquipeStore.criar(this@MainActivityProdutosFuncionario, SolicitacaoEquipeStore.REPOSICAO_ESTOQUE, product.nome, email, qty)
                            Toast.makeText(this@MainActivityProdutosFuncionario, "Pedido de reposição enviado para aprovação do CEO.", Toast.LENGTH_LONG).show()
                            render()
                        }
                    }
                }
            }
            card.addView(edit, lp(10))
            card.addView(sell, lp(7))
            card.addView(pause, lp(7))
            card.addView(restock, lp(7))
            content.addView(card, lp(11))
        }

        if (repo.temPermissao(email, "cadastrar_produtos") || repo.temPermissao(email, "conferir_recebimento")) {
            content.addView(button("✓  MINHAS CONFERÊNCIAS / RECEBIMENTOS", true).apply {
                setOnClickListener { startActivity(Intent(this@MainActivityProdutosFuncionario, MainActivityConferenciaEstoque::class.java)) }
            }, lp(14))
        }
        if (repo.temPermissao(email, "cadastrar_produtos")) {
            content.addView(button("▣  IMPORTAR NOTA XML / QR", false).apply {
                setOnClickListener { startActivity(Intent(this@MainActivityProdutosFuncionario, MainActivityImportarNota::class.java)) }
            }, lp(14))
            content.addView(button("＋  CADASTRAR PRODUTO", true).apply {
                setOnClickListener { startActivity(Intent(this@MainActivityProdutosFuncionario, MainActivityCadastroProduto::class.java)) }
            }, lp(14))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun pedirQuantidade(titulo: String, hintText: String, inicial: Int, onConfirm: (Int) -> Unit) {
        val input = EditText(this).apply {
            hint = hintText
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(inicial.toString())
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val wrapper = LinearLayout(this).apply { setPadding(dp(20), dp(6), dp(20), 0); addView(input) }
        AlertDialog.Builder(this).setTitle(titulo).setView(wrapper)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                val qty = input.text.toString().toIntOrNull()
                if (qty == null || qty <= 0) Toast.makeText(this, "Informe uma quantidade maior que zero.", Toast.LENGTH_SHORT).show()
                else onConfirm(qty)
            }.show()
    }

    private fun avisarPermissao(permission: String) {
        val descricao = when (permission) {
            "cadastrar_produtos" -> "editar e cadastrar produtos"
            "pausar_anuncios" -> "solicitar pausa de anúncios"
            "registrar_vendas" -> "registrar vendas"
            else -> "solicitar reposição de estoque"
        }
        AlertDialog.Builder(this).setTitle("Permissão do CEO necessária")
            .setMessage("Peça ao CEO para habilitar a permissão '$descricao' em Logins e permissões. Pausas e reposições continuam sujeitas à aprovação do CEO.")
            .setPositiveButton("Entendi", null).show()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { setColor(0xFF191919.toInt()); cornerRadius = dp(16).toFloat(); setStroke(dp(1), 0xFFFF7A00.toInt()) }
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun button(value: String, primary: Boolean) = Button(this).apply {
        text = value; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(if (primary) orange else 0xFF292929.toInt())
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), if (primary) orange else 0xFF555555.toInt())
        }
    }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (top > 0) topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun money(value: Double) = String.format(Locale("pt", "BR"), "R$ %.2f", value)
}
