package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivityPainelInterno : RcarBaseActivity() {
    override fun onResume() {
        super.onResume()
        findViewById<Button>(R.id.btnEncerrarTesteCompraCeo)?.visibility =
            if (CompraTesteCeoStore.ativo(this)) View.VISIBLE else View.GONE
        val email = SessaoRcar.atual(this)?.email.orEmpty()
        if (UsuarioLocalRepository(this).isCeo(email)) {
            val novos = ArquivoNotasStore.listar(this).count {
                it.tipo == ArquivoNotasStore.ENTRADA && it.lidaPeloCeoEm == 0L
            }
            findViewById<Button>(R.id.btnCentralNotas)?.text =
                if (novos > 0) "● CENTRAL DE NOTAS  •  $novos NOVA(S)" else "▣ CENTRAL DE NOTAS FISCAIS"
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painel_interno)

        val repo = UsuarioLocalRepository(this)
        val emailAtual = SessaoRcar.atual(this)?.email
        if (emailAtual.isNullOrBlank() || repo.papel(emailAtual) == UsuarioLocalRepository.PAPEL_CLIENTE) {
            finish(); return
        }
        // O perfil é lido do repositório autenticado; não confiamos apenas no extra do Intent.
        val papel = repo.papel(emailAtual)
        val isCeo = repo.isCeo(emailAtual)

        findViewById<TextView>(R.id.txtTituloPainel).text = if (isCeo) "Painel do CEO" else if (papel == UsuarioLocalRepository.PAPEL_GERENTE) "Painel do Gerente" else "Painel • ${papel.lowercase().replaceFirstChar { it.uppercase() }}"
        findViewById<TextView>(R.id.txtDescricaoPainel).text = if (isCeo) {
            "Acesso administrativo completo"
        } else {
            "Acesso conforme permissões liberadas pelo CEO"
        }

        if (!isCeo) {
            findViewById<Button>(R.id.btnClientesInterno).visibility = View.GONE
            findViewById<Button>(R.id.btnConfiguracoesInterno).visibility = View.GONE
            findViewById<Button>(R.id.btnRelatoriosInterno).visibility = View.GONE
            findViewById<Button>(R.id.btnGestaoUsuariosInterno).visibility = View.GONE
            findViewById<Button>(R.id.btnInsightsCeo).visibility = View.GONE
            findViewById<Button>(R.id.btnSessoesCeo).visibility = View.GONE
            findViewById<Button>(R.id.btnCadastrarProduto).visibility = if (repo.temPermissao(emailAtual, "cadastrar_produtos")) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.btnEstoqueInterno).visibility = if (repo.temPermissao(emailAtual, "gerenciar_estoque")) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.btnVendasInterno).visibility = if (repo.temPermissao(emailAtual, "ver_vendas")) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.btnPedidosInterno).visibility = if (repo.temPermissao(emailAtual, "gerenciar_pedidos")) View.VISIBLE else View.GONE
        }
        findViewById<Button>(R.id.btnConferenciaEstoque).visibility =
            if (isCeo || repo.temPermissao(emailAtual, "conferir_recebimento") || repo.temPermissao(emailAtual, "cadastrar_produtos") || repo.temPermissao(emailAtual, "lancar_notas")) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnConferenciaEstoque).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "conferir_recebimento") || repo.temPermissao(emailAtual, "cadastrar_produtos") || repo.temPermissao(emailAtual, "lancar_notas"))
                startActivity(Intent(this, MainActivityConferenciaEstoque::class.java))
        }
        findViewById<Button>(R.id.btnImportarNotaInterno).visibility =
            if (isCeo || (repo.temPermissao(emailAtual, "cadastrar_produtos") || repo.temPermissao(emailAtual, "conferir_recebimento") || repo.temPermissao(emailAtual, "lancar_notas"))) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnCentralNotas).visibility =
            if (isCeo || repo.temPermissao(emailAtual, "lancar_notas") || repo.temPermissao(emailAtual, "conferir_recebimento")) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnCentralNotas).setOnClickListener {
            startActivity(Intent(this, MainActivityCentralNotas::class.java))
        }
        findViewById<Button>(R.id.btnImportarNotaInterno).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "cadastrar_produtos") || repo.temPermissao(emailAtual, "conferir_recebimento") || repo.temPermissao(emailAtual, "lancar_notas"))
                startActivity(Intent(this, MainActivityImportarNota::class.java))
        }
        findViewById<Button>(R.id.btnLojasParceirasInterno).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnCompatibilidadeCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnCompatibilidadeCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityCompatibilidadeCeo::class.java))
        }
        findViewById<Button>(R.id.btnBuscaPlacaPainel).setOnClickListener {
            startActivity(Intent(this, MainActivityBuscaPlaca::class.java))
        }
        findViewById<Button>(R.id.btnMarketplaceInterno).setOnClickListener {
            startActivity(Intent(this, MainActivityMarketplace::class.java))
        }
        findViewById<Button>(R.id.btnLojasParceirasInterno).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityLojasParceiras::class.java))
        }
        findViewById<Button>(R.id.btnProdutosCadastrados).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnPainelLoja).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnOfertasCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnConfigurarPixCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnMeusProdutosFuncionario).visibility = if (!isCeo && repo.temPermissao(emailAtual, "cadastrar_produtos")) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnSolicitacoesEquipeCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnMargemLucroCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnModoTesteCompraCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnHistoricoComprasTesteCeo).visibility = if (isCeo) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnEncerrarTesteCompraCeo).visibility =
            if (isCeo && CompraTesteCeoStore.ativo(this)) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnEncerrarTesteCompraCeo).setOnClickListener {
            if (isCeo) {
                CompraTesteCeoStore.encerrar(this)
                android.widget.Toast.makeText(this, "Modo de teste encerrado. Histórico preservado.", android.widget.Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btnEncerrarTesteCompraCeo).visibility = View.GONE
            }
        }
        findViewById<Button>(R.id.btnModoTesteCompraCeo).setOnClickListener {
            if (isCeo && CompraTesteCeoStore.autorizado(this)) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Compra de teste — CEO")
                    .setMessage("Você poderá navegar pela loja, adicionar produtos ao carrinho e simular endereço e pagamento. Nenhum Pix será cobrado, pedido comercial será criado ou estoque será reduzido.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Iniciar teste") { _, _ ->
                        if (CompraTesteCeoStore.iniciar(this)) {
                            startActivity(Intent(this, MainActivitycomprasonline::class.java).putExtra("MODO_PREVIA_CEO", true))
                        }
                    }.show()
            }
        }
        findViewById<Button>(R.id.btnHistoricoComprasTesteCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityHistoricoTesteCeo::class.java))
        }
        findViewById<Button>(R.id.btnConfigurarPixCeo).setOnClickListener { if (isCeo) startActivity(Intent(this, MainActivityConfigurarPix::class.java)) }
        findViewById<Button>(R.id.btnMeusProdutosFuncionario).setOnClickListener {
            if (!isCeo) startActivity(Intent(this, MainActivityProdutosFuncionario::class.java))
        }
        findViewById<Button>(R.id.btnSolicitacoesEquipeCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivitySolicitacoesEstoque::class.java))
        }
        findViewById<Button>(R.id.btnMargemLucroCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityMargemLucro::class.java))
        }
        findViewById<Button>(R.id.btnProdutosCadastrados).setOnClickListener { if (isCeo) startActivity(Intent(this, MainActivityProdutosCadastrados::class.java)) }
        findViewById<Button>(R.id.btnPainelLoja).setOnClickListener { if (isCeo) startActivity(Intent(this, MainActivitycomprasonline::class.java).putExtra("MODO_PREVIA_CEO", true)) }
        findViewById<Button>(R.id.btnOfertasCeo).setOnClickListener { if (isCeo) startActivity(Intent(this, MainActivityOfertas::class.java)) }
        findViewById<Button>(R.id.btnGestaoUsuariosInterno).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityGestaoUsuarios::class.java))
        }
        findViewById<Button>(R.id.btnInsightsCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityInsightsCeo::class.java))
        }
        findViewById<Button>(R.id.btnSessoesCeo).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivitySessoesCeo::class.java))
        }

        findViewById<Button>(R.id.btnPedidosInterno).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "gerenciar_pedidos")) startActivity(Intent(this, MainActivityPedidos::class.java))
        }
        findViewById<Button>(R.id.btnProdutosInterno).setOnClickListener {
            startActivity(Intent(this, MainActivityListaProdutos::class.java))
        }
        findViewById<Button>(R.id.btnCadastrarProduto).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "cadastrar_produtos")) startActivity(Intent(this, MainActivityCadastroProduto::class.java))
        }
        findViewById<Button>(R.id.btnClientesInterno).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityClientes::class.java))
        }
        findViewById<Button>(R.id.btnConfiguracoesInterno).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityConfiguracoes::class.java))
        }
        findViewById<Button>(R.id.btnVendasInterno).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "ver_vendas")) startActivity(Intent(this, MainActivityVendas::class.java))
        }
        findViewById<Button>(R.id.btnEstoqueInterno).setOnClickListener {
            if (isCeo || repo.temPermissao(emailAtual, "gerenciar_estoque")) startActivity(Intent(this, MainActivityEstoque::class.java))
        }
        findViewById<Button>(R.id.btnRelatoriosInterno).setOnClickListener {
            if (isCeo) startActivity(Intent(this, MainActivityRelatorios::class.java))
        }
        findViewById<Button>(R.id.btnSairInterno).setOnClickListener {
            startActivity(Intent(this, MainActivityPerfilInterno::class.java))
        }
    }
}
