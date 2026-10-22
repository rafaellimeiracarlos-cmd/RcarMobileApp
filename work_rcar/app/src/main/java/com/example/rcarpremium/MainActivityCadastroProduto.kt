package com.example.rcarpremium

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.text.NumberFormat
import java.util.Locale

class MainActivityCadastroProduto : RcarBaseActivity() {
    private val categorias = listOf(
        "Selecione uma categoria",
        "Automotivo",
        "Freios",
        "Óleos e Lubrificantes",
        "Pneus e Rodas",
        "Suspensão",
        "Elétrica",
        "Ferramentas",
        "Acessórios",
        "Shampoo Automotivo",
        "Cera e Cristalização",
        "Higienização e Limpeza",
        "Motor",
        "Transmissão",
        "Arrefecimento",
        "Filtros",
        "Outros"
    )

    private lateinit var nome: EditText
    private lateinit var sku: EditText
    private lateinit var marca: EditText
    private lateinit var codigoBarras: EditText
    private lateinit var categoria: Spinner
    private lateinit var descricao: EditText
    private lateinit var precoCusto: EditText
    private lateinit var precoVenda: EditText
    private lateinit var estoque: EditText
    private lateinit var estoqueMinimo: EditText
    private lateinit var fornecedor: EditText
    private lateinit var localizacao: EditText
    private lateinit var produtoAtivo: SwitchCompat
    private lateinit var resumoNome: TextView
    private lateinit var resumoPreco: TextView
    private lateinit var resumoMargem: TextView
    private lateinit var resumoEstoque: TextView
    private var nomeOriginalEdicao: String? = null

    private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val emailAtual = RcarLocalStore.currentEmail(this)
        if (!UsuarioLocalRepository(this).temPermissao(emailAtual, "cadastrar_produtos")) {
            Toast.makeText(this, "Seu perfil não tem permissão para cadastrar produtos.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_cadastro_produto)
        bindViews()
        configurarCategorias()
        nomeOriginalEdicao = intent.getStringExtra("EDITAR_PRODUTO_NOME")
        if (!nomeOriginalEdicao.isNullOrBlank()) {
            val original = AdminLocalStore.products(this).firstOrNull { it.nome.equals(nomeOriginalEdicao, true) }
            if (original == null || (!UsuarioLocalRepository(this).isCeo(emailAtual.orEmpty()) && !original.cadastradoPor.equals(emailAtual, true))) {
                Toast.makeText(this, "Você só pode editar os produtos que cadastrou.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            preencherParaEdicao(nomeOriginalEdicao!!)
            findViewById<Button>(R.id.btnSalvarProduto).text = "SALVAR ALTERAÇÕES"
        }
        configurarResumoAoVivo()

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnGerarSku).setOnClickListener {
            sku.setText(gerarSku())
            sku.setSelection(sku.text.length)
        }
        findViewById<Button>(R.id.btnSalvarProduto).setOnClickListener { salvarProduto() }
    }

    private fun bindViews() {
        nome = findViewById(R.id.editNomeProduto)
        sku = findViewById(R.id.editSkuProduto)
        marca = findViewById(R.id.editMarcaProduto)
        codigoBarras = findViewById(R.id.editCodigoBarrasProduto)
        categoria = findViewById(R.id.editCategoriaProduto)
        descricao = findViewById(R.id.editDescricaoProduto)
        precoCusto = findViewById(R.id.editPrecoCustoProduto)
        precoVenda = findViewById(R.id.editPrecoProduto)
        estoque = findViewById(R.id.editEstoqueProduto)
        estoqueMinimo = findViewById(R.id.editEstoqueMinimoProduto)
        fornecedor = findViewById(R.id.editFornecedorProduto)
        localizacao = findViewById(R.id.editLocalizacaoProduto)
        produtoAtivo = findViewById(R.id.switchProdutoAtivo)
        resumoNome = findViewById(R.id.txtResumoNome)
        resumoPreco = findViewById(R.id.txtResumoPreco)
        resumoMargem = findViewById(R.id.txtResumoMargem)
        resumoEstoque = findViewById(R.id.txtResumoEstoque)
    }

    private fun configurarCategorias() {
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_spinner_produto,
            categorias
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(if (position == 0) Color.rgb(133, 133, 133) else Color.WHITE)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setBackgroundColor(Color.rgb(32, 32, 32))
                return view
            }
        }
        categoria.adapter = adapter
        categoria.setSelection(0)
    }

    private fun preencherParaEdicao(nomeProduto: String) {
        val p = AdminLocalStore.products(this).firstOrNull { it.nome.equals(nomeProduto, true) } ?: return
        nome.setText(p.nome); sku.setText(p.sku); marca.setText(p.marca); codigoBarras.setText(p.codigoBarras)
        descricao.setText(p.descricao); precoCusto.setText(p.precoCusto.toString()); precoVenda.setText(p.preco.toString())
        estoque.setText(p.estoque.toString()); estoqueMinimo.setText(p.estoqueMinimo.toString())
        fornecedor.setText(p.fornecedor); localizacao.setText(p.localizacao); produtoAtivo.isChecked = p.ativo
        val pos = categorias.indexOfFirst { it.equals(p.categoria, true) }
        if (pos >= 0) categoria.setSelection(pos)
    }

    private fun configurarResumoAoVivo() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = atualizarResumo()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        listOf(nome, precoCusto, precoVenda, estoque, estoqueMinimo).forEach { it.addTextChangedListener(watcher) }
        atualizarResumo()
    }

    private fun atualizarResumo() {
        val nomeProduto = nome.text.toString().trim()
        val custo = parseDecimal(precoCusto.text.toString()) ?: 0.0
        val venda = parseDecimal(precoVenda.text.toString()) ?: 0.0
        val quantidade = estoque.text.toString().trim().toIntOrNull() ?: 0
        val minimo = estoqueMinimo.text.toString().trim().toIntOrNull() ?: 0

        resumoNome.text = nomeProduto.ifBlank { "Nome do produto aparecerá aqui" }
        resumoPreco.text = "Preço de venda: ${moeda.format(venda.coerceAtLeast(0.0))}"
        resumoMargem.text = if (venda > 0.0 && custo > 0.0) {
            val lucroBruto = venda - custo
            val margem = (lucroBruto / venda) * 100.0
            "Diferença bruta estimada: ${moeda.format(lucroBruto)} por unidade (${String.format(Locale("pt", "BR"), "%.1f", margem)}% do preço de venda)"
        } else {
            "Informe o custo e o preço para estimar a diferença bruta."
        }
        resumoEstoque.text = if (quantidade <= minimo) {
            "Estoque inicial: $quantidade unidade(s) • Atenção: nível de reposição em $minimo."
        } else {
            "Estoque inicial: $quantidade unidade(s) • Reposição sugerida em $minimo."
        }
    }

    private fun salvarProduto() {
        val nomeValor = nome.text.toString().trim()
        val skuValor = sku.text.toString().trim().ifBlank { gerarSku() }
        val marcaValor = marca.text.toString().trim()
        val codigoValor = codigoBarras.text.toString().trim()
        val categoriaValor = categoria.selectedItem?.toString().orEmpty()
        val descricaoValor = descricao.text.toString().trim()
        val custoRaw = precoCusto.text.toString().trim()
        val custoValor = if (custoRaw.isBlank()) 0.0 else parseDecimal(custoRaw)
        val vendaValor = parseDecimal(precoVenda.text.toString())
        val estoqueValor = estoque.text.toString().trim().toIntOrNull()
        val minimoValor = estoqueMinimo.text.toString().trim().toIntOrNull() ?: 0
        val fornecedorValor = fornecedor.text.toString().trim()
        val localizacaoValor = localizacao.text.toString().trim()

        when {
            nomeValor.isBlank() -> nome.error = "Informe o nome do produto"
            categoriaValor == categorias.firstOrNull() || categoriaValor.isBlank() ->
                Toast.makeText(this, "Selecione a categoria do produto.", Toast.LENGTH_SHORT).show()
            custoValor == null || !custoValor.isFinite() || custoValor < 0.0 ->
                precoCusto.error = "Informe um custo válido ou deixe o campo vazio"
            vendaValor == null || !vendaValor.isFinite() || vendaValor <= 0.0 ->
                precoVenda.error = "Informe um preço de venda maior que zero"
            estoqueValor == null || estoqueValor < 0 ->
                estoque.error = "Informe o estoque inicial (zero ou maior)"
            minimoValor < 0 -> estoqueMinimo.error = "O nível mínimo não pode ser negativo"
            codigoValor.isNotBlank() && !codigoValor.all { it.isDigit() } ->
                codigoBarras.error = "Use apenas números no código de barras"
            else -> {
                if (sku.text.toString().isBlank()) sku.setText(skuValor)
                val produtoOriginal = nomeOriginalEdicao?.let { originalNome ->
                    AdminLocalStore.products(this).firstOrNull { it.nome.equals(originalNome, true) }
                }
                // Funcionário não pode lançar estoque pelo cadastro: envio à conferência para o CEO.
                val currentUser = SessaoRcar.atual(this)?.email.orEmpty()
                val ceo = UsuarioLocalRepository(this).isCeo(currentUser)
                if (!ceo && produtoOriginal != null && estoqueValor != produtoOriginal.estoque) {
                    estoque.error = "Altere quantidades pela Conferência de Peças; CEO precisa aprovar."
                    return
                }
                val requestedReceiving = if (!ceo && produtoOriginal == null) (estoqueValor ?: 0) else 0
                val produto = AdminProduto(
                    nome = nomeValor,
                    categoria = categoriaValor,
                    preco = vendaValor ?: 0.0,
                    estoque = if (ceo) (estoqueValor ?: 0) else produtoOriginal?.estoque ?: 0,
                    sku = skuValor,
                    marca = marcaValor,
                    codigoBarras = codigoValor,
                    descricao = descricaoValor,
                    precoCusto = custoValor ?: 0.0,
                    estoqueMinimo = minimoValor,
                    fornecedor = fornecedorValor,
                    localizacao = localizacaoValor,
                    ativo = produtoAtivo.isChecked,
                    cadastradoPor = produtoOriginal?.cadastradoPor ?: RcarLocalStore.currentEmail(this).orEmpty(),
                    anuncioAtivo = produtoOriginal?.anuncioAtivo ?: ceo
                )
                try {
                    val original = nomeOriginalEdicao
                    if (original.isNullOrBlank()) AdminLocalStore.addProduct(this, produto)
                    else AdminLocalStore.updateProduct(this, original, produto)
                    if (requestedReceiving > 0) {
                        val row = ConferenciaLinha(1, produto.nome, produto.nome, produto.categoria,
                            produto.sku, produto.codigoBarras, produto.precoCusto, produto.preco,
                            requestedReceiving, requestedReceiving)
                        val conference = EstoqueConferenciaStore.submitManual(this, "CADASTRO-${produto.sku}",
                            "Estoque inicial contado no cadastro. Aguardar aprovação do CEO.", listOf(row))
                        Toast.makeText(this, "Produto cadastrado com estoque zero. ${requestedReceiving} unidades enviadas ao CEO para conferir.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, if (original.isNullOrBlank()) "Produto cadastrado com sucesso!" else "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                } catch (exception: Exception) {
                    Toast.makeText(
                        this,
                        exception.message ?: "Não foi possível cadastrar o produto.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseDecimal(value: String): Double? {
        val normalized = value.trim().replace("R$", "", ignoreCase = true).replace(" ", "")
        if (normalized.isBlank()) return null
        // Aceita os formatos mais comuns digitados no Brasil: 149,90 ou 1.499,90.
        val canonical = if (normalized.contains(',')) {
            normalized.replace(".", "").replace(',', '.')
        } else {
            normalized
        }
        return canonical.toDoubleOrNull()
    }

    private fun gerarSku(): String = "RCP-${System.currentTimeMillis()}"
}
