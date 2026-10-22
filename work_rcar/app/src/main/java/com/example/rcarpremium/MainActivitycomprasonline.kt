package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivitycomprasonline : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_activitycomprasonline)
        if (CompraTesteCeoStore.ativo(this)) {
            mostrarFaixaCompraTeste()
        } else if (intent.getBooleanExtra("MODO_PREVIA_CEO", false)) {
            Toast.makeText(this, "Pré-visualização da loja: navegue como um cliente.", Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.btnMarketplace).setOnClickListener {
            startActivity(Intent(this, MainActivityMarketplace::class.java))
        }
        findViewById<View>(R.id.btnBuscaPlaca).setOnClickListener {
            startActivity(Intent(this, MainActivityBuscaPlaca::class.java))
        }
        BottomNav.setup(this, "inicio")
        instalarVoltarAoTopo()

        val btnIrCadastro = findViewById<View>(R.id.btnIrCadastro)
        val btnIrLogin = findViewById<View>(R.id.btnIrLogin)
        val editPesquisa = findViewById<EditText>(R.id.editPesquisa)
        val btnLupaPesquisa = findViewById<ImageButton>(R.id.btnLupaPesquisa)
        val btnVerProdutos = findViewById<View>(R.id.btnVerprodutos)
        val btnMenuCategorias = findViewById<ImageButton>(R.id.btnMenuCategorias)
        atualizarCabecalhoSessao()
        btnIrCadastro.setOnClickListener {
            if (CompraTesteCeoStore.ativo(this)) {
                CompraTesteCeoStore.encerrar(this)
                startActivity(Intent(this, MainActivityPainelInterno::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            } else if (SessaoRcar.atual(this) == null) {
                startActivity(Intent(this, MainActivityCadastro::class.java))
            } else SessaoRcar.abrirPerfil(this)
        }

        btnIrLogin.setOnClickListener {
            if (CompraTesteCeoStore.ativo(this)) {
                // Encerrar a simulação não encerra o login administrativo.
                CompraTesteCeoStore.encerrar(this)
                startActivity(Intent(this, MainActivityPerfilInterno::class.java))
                finish()
            } else if (SessaoRcar.atual(this) != null) {
                SessaoRcar.abrirPerfil(this)
            } else startActivity(Intent(this, MainActivity::class.java))
        }

        // O menu do topo fica reservado para navegação e acessos internos.
        // O banner de ofertas possui um ID separado e nunca chama o login.
        btnMenuCategorias.setOnClickListener { mostrarMenuPrincipal() }

        findViewById<View>(R.id.cardOfertasSemana).setOnClickListener { abrirOfertas() }
        findViewById<View>(R.id.btnVerCategorias).setOnClickListener {
            startActivity(Intent(this, MainActivityCategorias::class.java))
        }
        findViewById<View>(R.id.btnVerTodosProdutos).setOnClickListener {
            startActivity(Intent(this, MainActivityListaProdutos::class.java))
        }

        btnLupaPesquisa.setOnClickListener { executarBusca(editPesquisa) }
        editPesquisa.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                executarBusca(editPesquisa)
                true
            } else false
        }

        btnVerProdutos.setOnClickListener { abrirOfertas() }

        findViewById<View>(R.id.cardFreios).setOnClickListener { abrirCategoria("Freios") }
        findViewById<View>(R.id.cardSuspensao).setOnClickListener { abrirCategoria("Suspensão") }
        findViewById<View>(R.id.cardOleo).setOnClickListener { abrirCategoria("Óleos e lubrificantes") }
        findViewById<View>(R.id.cardPneus).setOnClickListener { abrirCategoria("Pneus e rodas") }
        findViewById<View>(R.id.cardFerramentas).setOnClickListener { abrirCategoria("Ferramentas") }

        findViewById<View>(R.id.btncomprar).setOnClickListener {
            adicionarAoCarrinho(RcarLocalStore.catalog[0])
        }
        findViewById<View>(R.id.btnComprarOleo).setOnClickListener {
            adicionarAoCarrinho(RcarLocalStore.catalog[1])
        }

        // Anúncios aprovados como pausados pelo CEO deixam de aparecer também nos destaques da home.
        ocultarDestaqueSeIndisponivel(RcarLocalStore.catalog[0].nome, R.id.btncomprar)
        ocultarDestaqueSeIndisponivel(RcarLocalStore.catalog[1].nome, R.id.btnComprarOleo)
    }


    override fun onResume() {
        super.onResume()
        if (findViewById<View>(R.id.btnIrLogin) != null) atualizarCabecalhoSessao()
    }

    private fun atualizarCabecalhoSessao() {
        val cadastro = findViewById<View>(R.id.btnIrCadastro)
        val acesso = findViewById<View>(R.id.btnIrLogin)
        val conta = SessaoRcar.atual(this)
        if (CompraTesteCeoStore.ativo(this)) {
            cadastro.visibility = View.VISIBLE
            (cadastro as? android.widget.Button)?.text = "FIM TESTE"
            (acesso as? android.widget.Button)?.text = "CEO"
        } else if (conta != null) {
            cadastro.visibility = View.GONE
            (acesso as? android.widget.Button)?.text = "PERFIL"
            acesso.contentDescription = "Abrir perfil de ${conta.nome}, ${conta.papel}"
        } else {
            cadastro.visibility = View.VISIBLE
            (cadastro as? android.widget.Button)?.text = "CADASTRO"
            (acesso as? android.widget.Button)?.text = "LOGIN"
        }
    }

    private fun mostrarFaixaCompraTeste() {
        val scroll = findViewById<ScrollView>(R.id.scrollLoja)
        val container = scroll.getChildAt(0) as? LinearLayout ?: return
        val aviso = TextView(this).apply {
            text = "🧪 MODO TESTE DO CEO — NENHUMA COBRANÇA OU VENDA REAL"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(148, 75, 0))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(13), dp(12), dp(13))
            contentDescription = "Ambiente de simulação de compra do CEO"
        }
        container.addView(aviso, 0, LinearLayout.LayoutParams(-1, -2))
    }

    private fun ocultarDestaqueSeIndisponivel(nomeProduto: String, buttonId: Int) {
        val product = AdminLocalStore.products(this).firstOrNull { it.nome.equals(nomeProduto, true) } ?: return
        val button = findViewById<View>(buttonId)
        val card = button.parent?.parent as? View ?: button
        card.visibility = if (product.ativo && product.anuncioAtivo && product.estoque > 0) View.VISIBLE else View.GONE
    }

    private fun abrirOfertas() {
        startActivity(Intent(this, MainActivityListaProdutos::class.java).apply {
            putExtra("MODO_OFERTAS", true)
        })
    }

    private fun adicionarAoCarrinho(produto: ProdutoItem) {
        val cadastroAdmin = AdminLocalStore.products(this).firstOrNull { it.nome.equals(produto.nome, true) }
        if (cadastroAdmin != null && (!cadastroAdmin.ativo || !cadastroAdmin.anuncioAtivo)) {
            Toast.makeText(this, "Este produto está temporariamente indisponível na loja.", Toast.LENGTH_LONG).show()
            return
        }
        if (!CompraTesteCeoStore.ativo(this) && SessaoRcar.atual(this) == null) {
            AlertDialog.Builder(this).setTitle("Entre para comprar")
                .setMessage("Faça login ou crie uma conta de cliente antes de adicionar produtos ao carrinho.")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Fazer login") { _, _ -> startActivity(Intent(this, MainActivity::class.java).putExtra("IR_CHECKOUT", true)) }
                .show()
            return
        }
        // Registra o item primeiro para que o carrinho já abra com os dados atualizados.
        RcarLocalStore.addToCart(this, produto.copy(quantidade = 1))
        startActivity(Intent(this, MainActivityItemCarrinho::class.java))
    }

    /** O atalho existe apenas ao final do conteúdo rolável, nunca sobre os produtos. */
    private fun instalarVoltarAoTopo() {
        val rolagem = findViewById<ScrollView>(R.id.scrollLoja)
        findViewById<View>(R.id.btnVoltarAoTopoLoja).setOnClickListener {
            rolagem.smoothScrollTo(0, 0)
        }
    }

    private fun mostrarMenuPrincipal() {
        val dialog = AlertDialog.Builder(this).create()
        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(16))
            background = getDrawable(R.drawable.bg_menu_dialog)
        }
        val titulo = TextView(this).apply {
            text = "RCAR PREMIUM"
            setTextColor(Color.rgb(255, 122, 0))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(2), dp(4), dp(12))
        }
        conteudo.addView(titulo)
        fun opcao(rotulo: String, detalhe: String, acao: () -> Unit) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                background = getDrawable(android.R.drawable.list_selector_background)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                contentDescription = "$rotulo. $detalhe"
            }
            item.addView(TextView(this).apply {
                text = rotulo
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            })
            item.addView(TextView(this).apply {
                text = detalhe
                setTextColor(Color.LTGRAY)
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
            })
            item.setOnClickListener { dialog.dismiss(); acao() }
            conteudo.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        opcao("Explorar categorias", "Encontre peças por tipo", {
            startActivity(Intent(this, MainActivityCategorias::class.java))
        })
        opcao("Acesso do funcionário", "Área operacional com permissões", {
            abrirLoginInterno(UsuarioLocalRepository.PAPEL_FUNCIONARIO)
        })
        opcao("Acesso do CEO", "Administração e controle total", {
            abrirLoginInterno(UsuarioLocalRepository.PAPEL_CEO)
        })
        dialog.setView(conteudo)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_menu_dialog)
    }

    private fun abrirLoginInterno(papel: String) {
        if (SessaoRcar.atual(this) != null) {
            SessaoRcar.abrirPerfil(this)
            return
        }
        startActivity(Intent(this, MainActivityAcessoInterno::class.java).apply {
            putExtra(MainActivityAcessoInterno.EXTRA_PAPEL, papel)
        })
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun abrirCategoria(nome: String) {
        startActivity(Intent(this, MainActivityListaProdutos::class.java).apply {
            putExtra("CATEGORIA", nome)
        })
    }

    private fun executarBusca(editPesquisa: EditText) {
        val texto = editPesquisa.text.toString().trim()
        if (texto.isEmpty()) {
            editPesquisa.error = "Digite o nome de um produto"
            editPesquisa.requestFocus()
        } else {
            startActivity(Intent(this, MainActivityListaProdutos::class.java).apply {
                putExtra("BUSCA_PRODUTO", texto)
            })
        }
    }
}
