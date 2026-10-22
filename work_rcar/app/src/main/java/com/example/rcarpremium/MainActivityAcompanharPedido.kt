package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import java.util.Locale

/** Acompanhamento, consulta de itens comprados e recompra; dados de transporte não são inventados. */
class MainActivityAcompanharPedido : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    private val white = Color.WHITE
    private val muted = Color.rgb(178, 183, 194)
    private val surface = Color.rgb(20, 21, 24)
    private val border = Color.rgb(120, 62, 15)
    private val green = Color.rgb(108, 229, 100)
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            if (::content.isInitialized) render()
            handler.postDelayed(this, 30_000L)
        }
    }
    private var pedidoId = ""
    private var lastSnapshot = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedidoId = intent.getStringExtra("PEDIDO_ID").orEmpty()
        if (buscarPedido() == null) {
            Toast.makeText(this, "Pedido não encontrado ou acesso não autorizado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        window.statusBarColor = Color.rgb(8, 9, 11)
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(8, 9, 11)) }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            isVerticalScrollBarEnabled = false
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(9), dp(14), dp(148))
        }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(9))
            background = rounded(Color.rgb(12, 13, 16), 23, orange)
            elevation = dp(16).toFloat()
        }
        val buy = Button(this).apply {
            text = "🛒   COMPRAR NOVAMENTE  ›"
            textSize = 15f
            isAllCaps = false
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(orange, 15)
            minHeight = dp(54)
            setOnClickListener { prepararRecompra() }
        }
        footer.addView(buy, LinearLayout.LayoutParams(-1, dp(54)))
        footer.addView(label("Novos preços e disponibilidade serão conferidos no catálogo.", 10f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(7), dp(4), dp(1))
        })
        root.addView(footer, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            setMargins(dp(14), 0, dp(14), dp(17))
        })
        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) {
            lastSnapshot = ""
            render()
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, 30_000L)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun buscarPedido(): PedidoLocal? {
        val user = SessaoRcar.atual(this) ?: return null
        val repo = UsuarioLocalRepository(this)
        if (!repo.usuarioAtivo(user.email)) return null
        val p = PedidoLocalStore.listar(this).firstOrNull { it.id == pedidoId } ?: return null
        return p.takeIf {
            it.clienteEmail.equals(user.email, true) || repo.isCeo(user.email) ||
                repo.temPermissao(user.email, "gerenciar_pedidos")
        }
    }

    private fun render() {
        val pedido = buscarPedido() ?: run { finish(); return }
        val tracking = PedidoRastreamentoStore.get(this, pedido.id)
        val notes = ArquivoNotasStore.paraPedido(this, pedido)
        val snapshot = "${pedido.status}|${pedido.atualizadoEm}|${tracking?.atualizadoEm}|${notes.joinToString { it.id }}|${pedido.itens.size}"
        if (snapshot == lastSnapshot && content.childCount > 0) return
        lastSnapshot = snapshot
        content.removeAllViews()
        buildHeader(pedido)
        buildStatus(pedido)
        buildTimeline(pedido)
        buildDelivery(tracking)
        buildProducts(pedido)
        buildFiscalNotes(notes)
        content.addView(label(
            "Dados de entrega atualizados enquanto esta tela está aberta, quando novas informações são registradas. " +
                "Nesta versão, diferentes aparelhos ainda não sincronizam automaticamente; o rastreio ao vivo requer integração com a transportadora.",
            10f, muted
        ), lp(18))
    }

    private fun buildHeader(pedido: PedidoLocal) {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = label("‹", 36f, orange, true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar aos pedidos"
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() }
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(48), dp(50)))
        bar.addView(ImageView(this).apply {
            setImageResource(R.drawable.imglogo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "RCarPremium"
        }, LinearLayout.LayoutParams(0, dp(59), 1f))
        bar.addView(label("", 10f, white), LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(bar)

        val heading = SpannableString("ACOMPANHE SUA COMPRA")
        heading.setSpan(ForegroundColorSpan(orange), 10, heading.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        content.addView(label("", 24f, white, true).apply { text = heading }, lp(10))
        content.addView(label("Pedido #${pedido.id}  •  ${PedidoLocalStore.dataFormatada(pedido.criadoEm)}", 12f, muted), lp(5))
        if (pedido.demonstracao) content.addView(label("DADOS DE DEMONSTRAÇÃO • PEDIDO ILUSTRATIVO", 10f, orange, true), lp(8))
    }

    private fun buildStatus(pedido: PedidoLocal) {
        val card = box()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(label("🚚", 28f, orange).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(45, 30, 17), 45, orange)
        }, LinearLayout.LayoutParams(dp(66), dp(66)))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(4), 0) }
        details.addView(label("STATUS ATUAL", 10f, muted, true))
        val statusTint = if (pedido.status == "Entregue" || pedido.status == "Enviado") green else orange
        details.addView(label("●  ${pedido.status.uppercase(Locale("pt", "BR"))}", 16f, statusTint, true), lp(5))
        details.addView(label(when (pedido.status) {
            "Enviado" -> "Seu pedido está a caminho."
            "Entregue" -> "Seu pedido foi entregue."
            "Separando produtos" -> "Estamos preparando suas peças."
            "Cancelado" -> "Este pedido foi cancelado."
            else -> "Seu pedido foi recebido."
        }, 12f, white), lp(3))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(row)
        card.addView(divider(), lp(12))
        val totalRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        totalRow.addView(label("TOTAL DO PEDIDO", 11f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        totalRow.addView(label(money.format(pedido.total), 22f, orange, true))
        card.addView(totalRow, lp(11))
        card.addView(label("${pedido.quantidadeItens} unidade(s)  •  Pagamento: ${pedido.formaPagamento}", 11f, muted), lp(5))
        content.addView(card, lp(15))
    }

    private fun buildTimeline(pedido: PedidoLocal) {
        val card = box()
        card.addView(label("▤   ETAPAS DO PEDIDO", 16f, white, true))
        card.addView(label("Acompanhe o andamento da sua compra.", 11f, muted), lp(5))
        if (pedido.status == "Cancelado") {
            card.addView(label("Pedido cancelado. Entre em contato com a loja caso precise de ajuda.", 13f, orange), lp(15))
        } else {
            val stages = listOf(
                "Pedido recebido" to "Recebemos o seu pedido.",
                "Separando produtos" to "Seus produtos estão sendo preparados.",
                "Enviado" to "Pedido despachado para entrega.",
                "Entregue" to "Pedido entregue no destino."
            )
            val currentStage = when (pedido.status) {
                "Entregue" -> 3
                "Enviado" -> 2
                "Separando produtos" -> 1
                else -> 0
            }
            stages.forEachIndexed { index, (title, sub) ->
                val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                val active = index <= currentStage
                val current = index == currentStage
                line.addView(label(if (index < currentStage) "✓" else if (current) "●" else "○", 20f, if (active) orange else muted, true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(if (active) Color.rgb(55, 30, 12) else Color.rgb(33, 35, 39), 40)
                }, LinearLayout.LayoutParams(dp(35), dp(35)))
                val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
                texts.addView(label(title, 13f, if (active) orange else muted, active))
                texts.addView(label(if (active) sub else "Aguardando esta etapa", 11f, muted), lp(2))
                line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
                card.addView(line, lp(if (index == 0) 17 else 12))
                if (index < 3) card.addView(divider(), lp(10))
            }
        }
        content.addView(card, lp(13))
    }

    private fun buildDelivery(r: RastreamentoInfo?) {
        val card = box()
        card.addView(label("🚚   ENTREGA E RASTREAMENTO", 16f, white, true))
        card.addView(label("Consulte as informações recebidas da operação de entrega.", 11f, muted), lp(6))
        infoRow(card, "Transportadora", r?.transportadora?.takeIf { it.isNotBlank() } ?: "Ainda não informada")
        infoRow(card, "Código de rastreio", r?.codigo?.takeIf { it.isNotBlank() } ?: "Aguardando envio")
        infoRow(card, "Previsão de entrega", r?.previsao?.takeIf { it.isNotBlank() } ?: "Aguardando transportadora")
        r?.observacao?.takeIf { it.isNotBlank() }?.let { infoRow(card, "Informações da entrega", it) }

        if (r?.latitude != null && r.longitude != null) {
            val lat = r.latitude!!
            val lon = r.longitude!!
            card.addView(label("ÚLTIMA POSIÇÃO RECEBIDA • ${PedidoLocalStore.dataFormatada(r.atualizadoEm)}", 11f, orange, true), lp(15))
            val outdated = System.currentTimeMillis() - r.atualizadoEm > 30L * 60L * 1000L
            card.addView(label(if (outdated) "Esta posição está desatualizada e não representa rastreamento ao vivo."
                else "Posição informada pela transportadora. Não representa movimento contínuo.", 11f, muted), lp(5))
            val delta = 0.014
            val url = "https://www.openstreetmap.org/export/embed.html?bbox=${lon-delta}%2C${lat-delta}%2C${lon+delta}%2C${lat+delta}&layer=mapnik&marker=$lat%2C$lon"
            card.addView(WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.allowContentAccess = false
                settings.allowFileAccess = false
                isVerticalScrollBarEnabled = false
                loadUrl(url)
            }, LinearLayout.LayoutParams(-1, dp(215)).apply { topMargin = dp(11) })
            card.addView(button("ABRIR MAPA", false) {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=15/$lat/$lon")))
                }.onFailure { Toast.makeText(this, "Nenhum aplicativo de mapas disponível.", Toast.LENGTH_LONG).show() }
            }, lp(9))
        } else {
            val map = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(17), dp(18), dp(17))
                background = rounded(Color.rgb(14, 16, 19), 14, Color.rgb(68, 71, 77))
            }
            map.addView(label("⌖", 35f, muted, true))
            map.addView(label("MAPA AINDA INDISPONÍVEL", 13f, white, true), lp(5))
            map.addView(label("A transportadora ainda não informou coordenadas. Nenhuma posição fictícia será exibida.", 11f, muted).apply {
                gravity = Gravity.CENTER
            }, lp(7))
            card.addView(map, lp(15))
        }
        card.addView(button("⟳   ATUALIZAR ACOMPANHAMENTO", false) {
            lastSnapshot = ""
            render()
            Toast.makeText(this, "Informações locais verificadas.", Toast.LENGTH_SHORT).show()
        }, lp(13))
        content.addView(card, lp(13))
    }

    private fun buildProducts(pedido: PedidoLocal) {
        val card = box()
        card.addView(label("◈   PRODUTOS DO PEDIDO", 16f, white, true))
        card.addView(label("Confira o que foi comprado e abra os detalhes do item.", 11f, muted), lp(6))
        val items = PedidoLocalStore.produtosDoPedido(pedido)
        if (items.isEmpty()) {
            card.addView(label("Não há detalhes individuais para este pedido antigo.", 12f, muted), lp(12))
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) card.addView(divider(), lp(14))
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(ImageView(this).apply {
                    setImageResource(R.drawable.pecasautomotiva)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = "Imagem ilustrativa do catálogo automotivo"
                    background = rounded(Color.rgb(28, 29, 32), 12)
                    clipToOutline = true
                }, LinearLayout.LayoutParams(dp(102), dp(94)))
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
                info.addView(label(item.nome, 14f, white, true))
                info.addView(label("${item.quantidade} unidade(s)", 11f, muted), lp(5))
                info.addView(label(item.precoUnitario?.let { money.format(it * item.quantidade) }
                    ?: "Valor do item indisponível no histórico antigo", 13f, orange, true), lp(7))
                info.addView(label("Imagem ilustrativa", 10f, muted), lp(4))
                row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                card.addView(row, lp(14))
                card.addView(button("◉   VER PRODUTO COMPRADO  ›", false) { abrirProdutoComprado(pedido, item) }, lp(10))
            }
        }
        content.addView(card, lp(13))
    }

    private fun abrirProdutoComprado(pedido: PedidoLocal, item: PedidoProduto) {
        val atual = AdminLocalStore.products(this).firstOrNull { it.nome.equals(item.nome, true) }
        val mensagem = buildString {
            append("PRODUTO COMPRADO\n\n")
            append("${item.nome}\n")
            append("Quantidade: ${item.quantidade} unidade(s)\n")
            append("Preço registrado na compra: ${item.precoUnitario?.let { money.format(it) } ?: "indisponível no pedido antigo"}\n")
            append("Pedido #${pedido.id}\n")
            append("Data: ${PedidoLocalStore.dataFormatada(pedido.criadoEm)}\n\n")
            append(if (atual == null) "Este produto não está cadastrado atualmente no catálogo."
                else "O anúncio atual pode ter preço, características e disponibilidade diferentes dos da compra original.")
        }
        val builder = AlertDialog.Builder(this)
            .setTitle("Detalhes da compra")
            .setMessage(mensagem)
            .setNegativeButton("Fechar", null)
        if (atual != null) builder.setPositiveButton("Ver anúncio atual") { _, _ ->
            startActivity(Intent(this, MainActivityVerProdutos::class.java).apply {
                putExtra("PRODUTO_NOME", atual.nome)
                putExtra("PRODUTO_PRECO", atual.preco)
            })
        }
        builder.show()
    }

    private fun buildFiscalNotes(notes: List<NotaArquivo>) {
        val card = box()
        card.addView(label("▣   NOTA FISCAL ELETRÔNICA", 16f, white, true))
        if (notes.isEmpty()) {
            card.addView(label("A nota fiscal ainda não foi anexada pela loja. Ela aparecerá aqui quando estiver disponível para este pedido.", 12f, muted), lp(10))
        } else notes.forEach { note ->
            card.addView(label("NF-e ${note.numero.ifBlank { "Sem número" }} • ${PedidoLocalStore.dataFormatada(note.lancadoEm)}", 12f, white), lp(11))
            card.addView(button("ABRIR NF-e / DANFE  ›", false) {
                runCatching { ArquivoNotasStore.abrir(this, note) }.onFailure {
                    AlertDialog.Builder(this).setTitle("Nota fiscal")
                        .setMessage(it.message ?: "Não foi possível abrir o documento.")
                        .setPositiveButton("Entendi", null).show()
                }
            }, lp(6))
        }
        content.addView(card, lp(13))
    }

    /** Recompra sempre consulta preços e estoque atuais e adiciona ao carrinho, sem efetuar cobrança. */
    private fun prepararRecompra() {
        val pedido = buscarPedido() ?: return
        val sessao = SessaoRcar.atual(this)
        val simulacao = CompraTesteCeoStore.ativo(this)
        if (sessao == null || (!simulacao && !SessaoRcar.cliente(this))) {
            AlertDialog.Builder(this).setTitle("Conta de cliente necessária")
                .setMessage("Para comprar novamente, entre com uma conta de cliente. O CEO também pode utilizar o Modo Teste, sem cobrança real.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        if (!simulacao && !pedido.clienteEmail.equals(sessao.email, true)) {
            Toast.makeText(this, "A recompra está disponível apenas ao titular do pedido.", Toast.LENGTH_LONG).show()
            return
        }
        val items = PedidoLocalStore.produtosDoPedido(pedido)
        if (items.isEmpty()) {
            Toast.makeText(this, "Este pedido não contém produtos identificáveis para recompra.", Toast.LENGTH_LONG).show()
            return
        }
        val catalog = AdminLocalStore.products(this).associateBy { it.nome.trim().lowercase(Locale.ROOT) }
        val existingCart = RcarLocalStore.cart(this).groupBy { it.nome.trim().lowercase(Locale.ROOT) }
            .mapValues { (_, list) -> list.sumOf { it.quantidade } }
        val toAdd = mutableListOf<ProdutoItem>()
        val unavailable = mutableListOf<String>()
        items.groupBy { it.nome.trim().lowercase(Locale.ROOT) }.forEach { (name, sameName) ->
            val product = catalog[name]
            val needed = sameName.sumOf { it.quantidade }
            val alreadyInCart = existingCart[name] ?: 0
            if (product == null || !product.ativo || !product.anuncioAtivo || needed <= 0 || needed > product.estoque - alreadyInCart) {
                unavailable.add(sameName.first().nome)
            } else {
                toAdd.add(ProdutoItem(product.nome, AdminLocalStore.promotionalPrice(this, product), needed, R.drawable.pecasautomotiva))
            }
        }
        if (unavailable.isNotEmpty()) {
            AlertDialog.Builder(this).setTitle("Confira a disponibilidade")
                .setMessage("Não foi possível preparar a recompra completa. Estes produtos estão ausentes, pausados ou sem estoque suficiente:\n\n${unavailable.joinToString("\n")}.\n\nNenhum item foi adicionado ao carrinho.")
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Buscar alternativas") { _, _ ->
                    val termo = unavailable.first().split(" ").filter { it.length > 3 }.take(3).joinToString(" ")
                        .ifBlank { unavailable.first() }
                    startActivity(Intent(this, MainActivityListaProdutos::class.java).putExtra("BUSCA_PRODUTO", termo))
                }.show()
            return
        }
        val count = toAdd.sumOf { it.quantidade }
        val total = toAdd.sumOf { it.preco * it.quantidade }
        AlertDialog.Builder(this)
            .setTitle("Comprar novamente")
            .setMessage("Adicionar $count unidade(s) ao seu carrinho pelo valor atual de ${money.format(total)}?\n\nOs preços podem ter mudado desde a compra original. O frete será calculado no checkout.${if (simulacao) "\n\nMODO TESTE CEO — nenhuma cobrança real." else ""}")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Ir para o carrinho") { _, _ ->
                toAdd.forEach { RcarLocalStore.addToCart(this, it) }
                startActivity(Intent(this, MainActivityItemCarrinho::class.java))
            }.show()
    }

    private fun infoRow(parent: LinearLayout, title: String, value: String) {
        parent.addView(label(title, 11f, muted), lp(12))
        parent.addView(label(value, 13f, white, true), lp(3))
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(16))
        background = rounded(surface, 18, border)
    }

    private fun rounded(fill: Int, cornerDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(cornerDp).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun divider() = View(this).apply { setBackgroundColor(Color.rgb(51, 53, 58)) }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(1)) }

    private fun button(s: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        textSize = 12f
        setTextColor(white)
        background = rounded(if (primary) orange else Color.rgb(30, 31, 35), 13, orange)
        minHeight = dp(46)
        setOnClickListener { click() }
    }

    private fun label(s: String, fontSize: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = fontSize
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun lp(marginTopDp: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(marginTopDp)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
