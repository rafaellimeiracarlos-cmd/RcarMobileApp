package com.example.rcarpremium

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class MainActivityVerProdutos : RcarBaseActivity() {

    private var quantidade: Int = 1
    private var precoUnitario: Double = 150.00
    private var nomeProdutoAtual: String = "Pastilha de Freio RCar"
    private var produtoAtual: AdminProduto? = null
    private lateinit var scrollDetalhe: ScrollView
    private lateinit var txtQuantidade: TextView
    private lateinit var txtPreco: TextView
    private lateinit var txtParcelamento: TextView
    private lateinit var txtTotalSelecionado: TextView
    private lateinit var txtPrecoAntigo: TextView
    private lateinit var txtDesconto: TextView
    private lateinit var relatedProductsContainer: LinearLayout
    private lateinit var txtConteudoAba: TextView
    private lateinit var tabDescricao: TextView
    private lateinit var tabEspecificacoes: TextView
    private lateinit var tabAvaliacoes: TextView
    private val currency: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    // A base atual possui uma fotografia original; as demais entradas são recortes
    // fiéis da mesma imagem, até que o cadastro suporte fotos distintas por anúncio.
    private val galeriaFotos = intArrayOf(
        R.drawable.pecasautomotiva,
        R.drawable.pecas_detalhe_freios,
        R.drawable.pecas_detalhe_filtros,
        R.drawable.pecas_detalhe_pastilhas
    )
    private val galeriaTitulos = arrayOf("Visão geral", "Detalhe do disco", "Filtros e componentes", "Detalhe das pastilhas")
    private var imagemAtual = 0
    private lateinit var fotoProduto: ImageView
    private lateinit var indicadorGaleria: TextView


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ver_produtos)
        window.statusBarColor = Color.rgb(7, 8, 9)
        window.navigationBarColor = Color.BLACK

        scrollDetalhe = findViewById(R.id.scrollDetalheProduto)
        txtQuantidade = findViewById(R.id.txtQuantidadeDetalhe)
        txtPreco = findViewById(R.id.txtPrecoProdutoDetalhe)
        txtParcelamento = findViewById(R.id.txtParcelamento)
        txtTotalSelecionado = findViewById(R.id.txtTotalSelecionado)
        txtPrecoAntigo = findViewById(R.id.txtPrecoAntigo)
        txtDesconto = findViewById(R.id.txtDescontoDetalhe)
        relatedProductsContainer = findViewById(R.id.relatedProductsContainer)
        txtConteudoAba = findViewById(R.id.txtConteudoAbaDetalhe)
        tabDescricao = findViewById(R.id.tabDescricaoDetalhe)
        tabEspecificacoes = findViewById(R.id.tabEspecificacoesDetalhe)
        tabAvaliacoes = findViewById(R.id.tabAvaliacoesDetalhe)
        fotoProduto = findViewById(R.id.imgProdutoDetalhe)
        indicadorGaleria = findViewById(R.id.txtIndicadorGaleria)
        configurarGaleria()
        configurarConsultaEntrega()
        if (CompraTesteCeoStore.ativo(this)) {
            findViewById<TextView>(R.id.txtBannerPromocional).apply {
                text = "🧪 SIMULAÇÃO DO CEO — PRODUTO SEM COBRANÇA REAL"
                setBackgroundColor(Color.rgb(148, 75, 0))
                setTextColor(Color.WHITE)
            }
        }

        val requestedName = intent.getStringExtra("PRODUTO_NOME")?.trim().orEmpty()
            .ifBlank { "Kit Pastilha de Freio Dianteira RCar Premium Alta Performance" }
        produtoAtual = encontrarProduto(requestedName)
        nomeProdutoAtual = produtoAtual?.nome ?: requestedName
        val lojaProduto = produtoAtual?.let { produto ->
            MarketplaceStore.lojas(this).firstOrNull { it.id == MarketplaceStore.lojaDeProduto(this, produto) }
        }
        findViewById<TextView>(R.id.txtVendedorProdutoDetalhe).text =
            "Vendido por ${lojaProduto?.nome ?: "RCarPremium"}"

        val precoRecebido = intent.getDoubleExtra("PRODUTO_PRECO", 150.00)
        val precoBase = (produtoAtual?.preco ?: precoRecebido).takeIf { it > 0.0 } ?: 150.00
        val descontoPercentual = produtoAtual?.let { AdminLocalStore.offerPercent(this, it.nome) } ?: 0
        precoUnitario = if (produtoAtual != null) AdminLocalStore.promotionalPrice(this, produtoAtual!!) else precoBase

        findViewById<TextView>(R.id.txtNomeProdutoDetalhe).text = nomeProdutoAtual
        findViewById<TextView>(R.id.txtSeloProduto).text = produtoAtual?.categoria?.uppercase(Locale("pt", "BR"))?.takeIf { it.isNotBlank() } ?: "PEÇA AUTOMOTIVA"
        txtPreco.text = currency.format(precoUnitario)
        if (descontoPercentual > 0 && produtoAtual != null) {
            txtPrecoAntigo.visibility = View.VISIBLE
            txtPrecoAntigo.text = currency.format(produtoAtual!!.preco)
            txtPrecoAntigo.paintFlags = txtPrecoAntigo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            txtDesconto.visibility = View.VISIBLE
            txtDesconto.text = "$descontoPercentual% OFF"
        } else {
            txtPrecoAntigo.visibility = View.GONE
            txtDesconto.visibility = View.GONE
        }
        atualizarValores()

        findViewById<ImageView>(R.id.btnVoltarinicial).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnCarrinhoTopo).setOnClickListener {
            startActivity(Intent(this, MainActivityItemCarrinho::class.java))
        }
        findViewById<View>(R.id.btnFavoritoDetalhe).setOnClickListener { view ->
            val label = view as? TextView ?: return@setOnClickListener
            val favorito = label.text.toString() == "♥"
            label.text = if (favorito) "♡" else "♥"
            label.setTextColor(if (favorito) Color.WHITE else Color.rgb(255, 122, 0))
            Toast.makeText(this, if (favorito) "Removido dos favoritos" else "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAumentarQtd).setOnClickListener {
            val disponivel = produtoAtual?.estoque
            if (disponivel != null && quantidade >= disponivel) {
                Toast.makeText(this, "Quantidade máxima disponível: $disponivel", Toast.LENGTH_SHORT).show()
            } else if (quantidade < 99) {
                quantidade++
                atualizarValores()
            }
        }
        findViewById<Button>(R.id.btnDiminuirQtd).setOnClickListener {
            if (quantidade > 1) {
                quantidade--
                atualizarValores()
            }
        }

        findViewById<Button>(R.id.btnAdicionarAoCarrinho).setOnClickListener {
            if (!produtoDisponivelParaCompra(nomeProdutoAtual)) return@setOnClickListener
            if (!exigirContaAtiva()) return@setOnClickListener
            RcarLocalStore.addToCart(this, ProdutoItem(nomeProdutoAtual, precoUnitario, quantidade, R.drawable.pecasautomotiva))
            Toast.makeText(this, "Produto adicionado ao carrinho", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnComprarAgora).setOnClickListener {
            if (!produtoDisponivelParaCompra(nomeProdutoAtual)) return@setOnClickListener
            if (!exigirLoginCliente()) return@setOnClickListener
            val totalCalculado = precoUnitario * quantidade
            RcarLocalStore.addToCart(this, ProdutoItem(nomeProdutoAtual, precoUnitario, quantidade, R.drawable.pecasautomotiva))
            startActivity(Intent(this, MainActivityPagamento::class.java).apply {
                putExtra("TOTAL_PEDIDO", currency.format(totalCalculado))
                putExtra("QTD_PRODUTOS", quantidade)
            })
        }

        findViewById<Button>(R.id.btnVoltarAoTopoDetalhe).setOnClickListener { scrollDetalhe.smoothScrollTo(0, 0) }
        findViewById<EditText>(R.id.editPesquisaDetalhe).setOnEditorActionListener { field, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = field.text.toString().trim()
                if (query.isNotBlank()) startActivity(Intent(this, MainActivityListaProdutos::class.java).putExtra("BUSCA_PRODUTO", query))
                true
            } else false
        }

        val description = produtoAtual?.descricao?.takeIf { it.isNotBlank() }
            ?: "Peça automotiva selecionada para a sua manutenção. Confira as especificações e a compatibilidade antes de concluir a compra. Em caso de dúvida, fale com o atendimento RCarPremium."
        tabDescricao.setOnClickListener { selecionarAba("descricao", description) }
        tabEspecificacoes.setOnClickListener { selecionarAba("especificacoes", textoEspecificacoes()) }
        tabAvaliacoes.setOnClickListener {
            selecionarAba("avaliacoes", "Ainda não há avaliações registradas para este produto. Após a compra, esta área poderá exibir a experiência dos clientes.")
        }
        selecionarAba("descricao", description)
        montarProdutosRelacionados()
        BottomNav.setup(this, "inicio")
    }

    private fun atualizarImagemGaleria() {
        fotoProduto.setImageResource(galeriaFotos[imagemAtual])
        indicadorGaleria.text = galeriaFotos.indices.joinToString("  ") { if (it == imagemAtual) "●" else "○" }
        indicadorGaleria.contentDescription = "Imagem ${imagemAtual + 1} de ${galeriaFotos.size}: ${galeriaTitulos[imagemAtual]}"
    }

    private fun avancarImagem(direcao: Int) {
        imagemAtual = (imagemAtual + direcao + galeriaFotos.size) % galeriaFotos.size
        atualizarImagemGaleria()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarGaleria() {
        atualizarImagemGaleria()
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                when {
                    e.x < fotoProduto.width * 0.30f -> avancarImagem(-1)
                    e.x > fotoProduto.width * 0.70f -> avancarImagem(1)
                    else -> abrirImagemAmpliada()
                }
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val inicio = e1 ?: return false
                val distancia = e2.x - inicio.x
                if (kotlin.math.abs(distancia) > dp(45) && kotlin.math.abs(distancia) > kotlin.math.abs(e2.y - inicio.y)) {
                    avancarImagem(if (distancia < 0f) 1 else -1)
                    return true
                }
                return false
            }
        })
        fotoProduto.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) view.parent.requestDisallowInterceptTouchEvent(true)
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) view.parent.requestDisallowInterceptTouchEvent(false)
            detector.onTouchEvent(event)
            true
        }
        // Ação acessível quando o TalkBack seleciona a imagem.
        fotoProduto.setOnClickListener { abrirImagemAmpliada() }
    }

    private fun abrirImagemAmpliada() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(5, 6, 8))
            setPadding(dp(14), dp(24), dp(14), dp(24))
        }
        val titulo = TextView(this).apply {
            text = "FOTOS DO PRODUTO"
            setTextColor(Color.rgb(255, 138, 0))
            typeface = Typeface.DEFAULT_BOLD
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
        }
        val fechar = TextView(this).apply {
            text = "✕  Fechar"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            contentDescription = "Fechar imagem ampliada"
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { dialog.dismiss() }
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(fechar)
        container.addView(header)

        val tela = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            setImageResource(galeriaFotos[imagemAtual])
            contentDescription = "Foto ampliada de $nomeProdutoAtual"
        }
        container.addView(tela, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val legenda = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, dp(9), 0, dp(9))
        }
        val atualizarDialogo = {
            tela.setImageResource(galeriaFotos[imagemAtual])
            legenda.text = "${imagemAtual + 1} de ${galeriaFotos.size}  •  ${galeriaTitulos[imagemAtual]}"
        }
        atualizarDialogo()
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val anterior = TextView(this).apply {
            text = "‹  ANTERIOR"
            setTextColor(Color.rgb(255, 138, 0))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { avancarImagem(-1); atualizarDialogo() }
        }
        val proximo = TextView(this).apply {
            text = "PRÓXIMA  ›"
            setTextColor(Color.rgb(255, 138, 0))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { avancarImagem(1); atualizarDialogo() }
        }
        nav.addView(anterior, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(proximo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(legenda)
        container.addView(nav)
        dialog.setContentView(container)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun configurarConsultaEntrega() {
        val cabecalho = findViewById<View>(R.id.btnExpandirEntrega)
        val conteudo = findViewById<LinearLayout>(R.id.conteudoConsultaEntrega)
        val indicador = findViewById<TextView>(R.id.iconeExpandirEntrega)
        val cepField = findViewById<EditText>(R.id.editCepEntrega)
        val consultar = findViewById<Button>(R.id.btnConsultarCep)
        val resultado = findViewById<TextView>(R.id.txtResultadoEntrega)
        val resumo = findViewById<TextView>(R.id.txtResumoEntrega)

        cabecalho.setOnClickListener {
            val aberto = conteudo.visibility == View.VISIBLE
            conteudo.visibility = if (aberto) View.GONE else View.VISIBLE
            indicador.text = if (aberto) "⌄" else "⌃"
            if (!aberto) {
                resultado.text = "Informe o CEP para verificar sua localidade.\n${textoEntregaConfigurada()}"
                scrollDetalhe.post { scrollDetalhe.smoothScrollTo(0, findViewById<View>(R.id.cardConsultaEntrega).top) }
            }
        }
        val buscar = {
            val cep = cepField.text.toString().filter { it.isDigit() }
            when {
                cep.length != 8 -> {
                    resultado.text = "Informe um CEP válido com 8 dígitos."
                    resultado.setTextColor(Color.rgb(255, 169, 80))
                }
                else -> {
                    resultado.text = "Consultando CEP..."
                    resultado.setTextColor(Color.LTGRAY)
                    consultar.isEnabled = false
                    Thread {
                        var conexao: HttpURLConnection? = null
                        val resposta = try {
                            val connection = (URL("https://viacep.com.br/ws/$cep/json/").openConnection() as HttpURLConnection).apply {
                                connectTimeout = 7000
                                readTimeout = 7000
                                requestMethod = "GET"
                                setRequestProperty("Accept", "application/json")
                            }
                            conexao = connection
                            if (connection.responseCode != 200) throw IllegalStateException("Consulta indisponível")
                            val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                            if (json.optBoolean("erro", false)) {
                                "CEP não encontrado. Confira os números informados."
                            } else {
                                val cidade = json.optString("localidade")
                                val uf = json.optString("uf")
                                val bairro = json.optString("bairro").takeIf { it.isNotBlank() }
                                val regiao = listOfNotNull(bairro, "$cidade - $uf").joinToString(", ")
                                "CEP $cep • $regiao\n${textoEntregaConfigurada()}"
                            }
                        } catch (_: Exception) {
                            "Não foi possível consultar o CEP. Verifique sua conexão e tente novamente."
                        } finally {
                            conexao?.disconnect()
                        }
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                consultar.isEnabled = true
                                resultado.text = resposta
                                resultado.setTextColor(if (resposta.startsWith("CEP $cep •")) Color.WHITE else Color.rgb(255, 169, 80))
                                if (resposta.startsWith("CEP $cep •")) resumo.text = "Entrega para $cep • Ver condições abaixo"
                            }
                        }
                    }.start()
                }
            }
        }
        consultar.setOnClickListener { buscar() }
        cepField.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE) {
                buscar()
                true
            } else false
        }
    }

    private fun textoEntregaConfigurada(): String {
        val config = getSharedPreferences("rcarpremium_config", MODE_PRIVATE)
        val freteTexto = if (config.contains("frete_padrao")) {
            "Frete padrão da loja: ${currency.format(config.getFloat("frete_padrao", 0f).toDouble())} (não calculado pelo CEP)"
        } else "Frete: ainda não configurado pela loja"
        val dias = config.getInt("prazo_entrega_dias_uteis", 0)
        val prazoTexto = if (dias > 0) {
            "Previsão informada pela loja: até $dias dias úteis, sujeita à confirmação no checkout."
        } else "Prazo: ainda não informado pela loja. Confirme antes de comprar."
        return "$freteTexto\n$prazoTexto"
    }

    private fun atualizarValores() {
        val total = precoUnitario * quantidade
        val parcela = precoUnitario / 3.0
        txtQuantidade.text = quantidade.toString()
        txtPreco.text = currency.format(precoUnitario)
        txtParcelamento.text = "Em 3x de ${currency.format(parcela)} sem juros"
        txtTotalSelecionado.text = if (quantidade == 1) "Total do item: ${currency.format(total)}" else "Total de $quantidade unidades: ${currency.format(total)}"
    }

    private fun encontrarProduto(nome: String): AdminProduto? {
        val produtos = AdminLocalStore.products(this)
        produtos.firstOrNull { it.nome.equals(nome, true) }?.let { return it }
        produtos.firstOrNull { nome.contains(it.nome, true) || it.nome.contains(nome, true) }?.let { return it }
        val termos = normalizar(nome).split(" ").filter { it.length > 3 }.toSet()
        return produtos.map { p -> p to normalizar(p.nome).split(" ").toSet().intersect(termos).size }
            .filter { it.second >= 2 }
            .maxByOrNull { it.second }?.first
    }

    private fun normalizar(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9áéíóúãõâêôç ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun textoEspecificacoes(): String {
        val p = produtoAtual ?: return "Categoria: Automotivo\nCompatibilidade: confira as informações do fabricante antes da compra."
        return buildString {
            append("Categoria: ${p.categoria.ifBlank { "Automotivo" }}")
            if (p.marca.isNotBlank()) append("\nMarca: ${p.marca}")
            if (p.sku.isNotBlank()) append("\nCódigo do produto: ${p.sku}")
            if (p.codigoBarras.isNotBlank()) append("\nCódigo de barras: ${p.codigoBarras}")
            append("\nDisponibilidade: ${if (p.estoque > 0 && p.ativo && p.anuncioAtivo) "Em estoque" else "Consulte disponibilidade"}")
            if (p.estoque > 0) append("\nUnidades disponíveis: ${p.estoque}")
        }
    }

    private fun selecionarAba(aba: String, texto: String) {
        val selected = Color.rgb(255, 122, 0)
        val idle = Color.rgb(174, 181, 192)
        val tabs = listOf("descricao" to tabDescricao, "especificacoes" to tabEspecificacoes, "avaliacoes" to tabAvaliacoes)
        tabs.forEach { (key, view) ->
            val active = key == aba
            view.setTextColor(if (active) selected else idle)
            view.typeface = Typeface.create("sans-serif", if (active) Typeface.BOLD else Typeface.NORMAL)
            view.setBackgroundResource(if (active) R.drawable.bg_detail_tab_selected else android.R.color.transparent)
        }
        txtConteudoAba.text = texto
    }

    private fun montarProdutosRelacionados() {
        relatedProductsContainer.removeAllViews()
        val related = AdminLocalStore.products(this)
            .filter { it.ativo && it.anuncioAtivo && it.estoque > 0 && !it.nome.equals(nomeProdutoAtual, true) }
            .take(12)
        if (related.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Novos produtos relacionados aparecerão aqui quando estiverem disponíveis na loja."
                setTextColor(Color.LTGRAY)
                textSize = 12f
                setPadding(dp(8), dp(12), dp(8), dp(12))
            }
            relatedProductsContainer.addView(empty)
            return
        }
        related.forEach { product ->
            val price = AdminLocalStore.promotionalPrice(this, product)
            val discount = AdminLocalStore.offerPercent(this, product.nome)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(9))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(15, 16, 18))
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.rgb(104, 57, 15))
                }
                isClickable = true
                isFocusable = true
                elevation = dp(2).toFloat()
                contentDescription = "Ver ${product.nome}"
            }
            val cardParams = LinearLayout.LayoutParams(dp(158), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(10) }
            val image = ImageView(this).apply {
                setImageResource(R.drawable.pecasautomotiva)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = roundedBackground(Color.rgb(25, 26, 28), Color.rgb(74, 43, 18), 10)
                clipToOutline = true
                contentDescription = "Imagem de ${product.nome}"
            }
            card.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(95)))
            if (discount > 0) {
                card.addView(TextView(this).apply {
                    text = "$discount% OFF"
                    setTextColor(Color.WHITE)
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(6), dp(4), dp(6), dp(4))
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(255, 112, 0)); cornerRadius = dp(7).toFloat()
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
            }
            card.addView(TextView(this).apply {
                text = product.nome
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                maxLines = 2
                minLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(7) })
            card.addView(TextView(this).apply {
                text = currency.format(price)
                setTextColor(Color.rgb(255, 138, 0))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(5) })
            card.addView(TextView(this).apply {
                text = if (product.estoque <= product.estoqueMinimo) "Poucas unidades" else "Disponível para compra"
                setTextColor(if (product.estoque <= product.estoqueMinimo) Color.rgb(255, 177, 65) else Color.rgb(162, 168, 177))
                textSize = 10f
                maxLines = 1
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) })
            card.setOnClickListener {
                startActivity(Intent(this, MainActivityVerProdutos::class.java).apply {
                    putExtra("PRODUTO_NOME", product.nome)
                    putExtra("PRODUTO_PRECO", price)
                })
            }
            relatedProductsContainer.addView(card, cardParams)
        }
    }

    private fun roundedBackground(fill: Int, stroke: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun produtoDisponivelParaCompra(nomeProduto: String): Boolean {
        val product = AdminLocalStore.products(this).firstOrNull { it.nome.equals(nomeProduto, true) }
        if (product != null && (!product.ativo || !product.anuncioAtivo || product.estoque <= 0)) {
            Toast.makeText(this, "Este produto está temporariamente indisponível.", Toast.LENGTH_LONG).show()
            return false
        }
        if (product != null && quantidade > product.estoque) {
            Toast.makeText(this, "Estoque disponível: ${product.estoque} unidade(s).", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun exigirContaAtiva(): Boolean {
        if (CompraTesteCeoStore.ativo(this) || SessaoRcar.atual(this) != null) return true
        return exigirLoginCliente()
    }

    private fun exigirLoginCliente(): Boolean {
        if (CompraTesteCeoStore.ativo(this) || SessaoRcar.cliente(this)) return true
        val conta = SessaoRcar.atual(this)
        if (conta != null) {
            AlertDialog.Builder(this).setTitle("Compra para clientes")
                .setMessage("Você está conectado como ${conta.papel}. Para testar o checkout sem cobrança, o CEO pode usar Simular compra no painel. A conta continuará conectada.")
                .setPositiveButton("Entendi", null).show()
            return false
        }
        AlertDialog.Builder(this).setTitle("Login necessário")
            .setMessage("Entre na sua conta de cliente para comprar este produto.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Fazer login") { _, _ -> startActivity(Intent(this, MainActivity::class.java).putExtra("IR_CHECKOUT", true)) }
            .show()
        return false
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
