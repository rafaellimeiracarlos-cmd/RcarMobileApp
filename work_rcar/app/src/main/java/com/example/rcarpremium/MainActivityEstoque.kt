package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class MainActivityEstoque : RcarBaseActivity() {
    private lateinit var content: LinearLayout
    private val money by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    private val bg = Color.rgb(8, 8, 8)
    private val panel = Color.rgb(17, 17, 17)
    private val orange = Color.rgb(255, 122, 0)
    private val orangeLight = Color.rgb(255, 151, 22)
    private val muted = Color.rgb(174, 190, 204)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!UsuarioLocalRepository(this).temPermissao(RcarLocalStore.currentEmail(this), "gerenciar_estoque")) {
            Toast.makeText(this, "Sem permissão para acessar o estoque.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        buildScreen()
        renderProducts()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setBackgroundColor(bg)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(22))
        }
        scroll.addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(buildFloatingNav(), LinearLayout.LayoutParams(-1, -2))
        setContentView(page)
    }

    private fun renderProducts() {
        content.removeAllViews()

        // Cabeçalho de marca
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(14))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.rcarlogo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Logo RCarPremium"
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(158), dp(52)))
        val spacer = View(this)
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        val role = TextView(this).apply {
            text = "♛  ADMIN"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(orangeLight)
            gravity = Gravity.CENTER
            setPadding(dp(11), dp(9), dp(11), dp(9))
            background = rounded(Color.rgb(20, 15, 9), orange, 28)
        }
        header.addView(role)
        content.addView(header)

        val titlePanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(15))
            background = rounded(panel, Color.rgb(105, 54, 7), 20)
        }
        val icon = TextView(this).apply {
            text = "▤"
            textSize = 31f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(orange)
        }
        titlePanel.addView(icon, LinearLayout.LayoutParams(dp(48), dp(52)))
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), 0, 0, 0)
        }
        titleColumn.addView(text("CONTROLE DE ESTOQUE", 20f, white, true))
        titleColumn.addView(text("Gerencie seus produtos", 12f, muted).apply {
            setPadding(0, dp(4), 0, 0)
        })
        titlePanel.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(titlePanel, bottomMargin(-1, -2, 0, 0, 0, 16))

        val items = AdminLocalStore.products(this).filter { it.ativo }
        val units = items.sumOf { it.estoque }
        val value = items.sumOf { it.preco * it.estoque }
        val low = items.count { it.estoque <= it.estoqueMinimo }

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(Color.rgb(15, 15, 15), Color.rgb(55, 42, 26), 16)
        }
        summary.addView(text("RESUMO DO INVENTÁRIO", 11f, orange, true))
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(9), 0, dp(8))
        }
        stats.addView(statBlock(items.size.toString(), "PRODUTOS"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statBlock(units.toString(), "UNIDADES"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statBlock(low.toString(), "ESTOQUE BAIXO"), LinearLayout.LayoutParams(0, -2, 1f))
        summary.addView(stats)
        summary.addView(text("${money.format(value)} em mercadorias", 13f, white, true))
        content.addView(summary, bottomMargin(-1, -2, 0, 0, 0, 12))
        content.addView(text("As entradas de produtos são conferidas por funcionários e só entram no estoque com aprovação do CEO.", 12f, muted), bottomMargin(-1, -2, 0, 0, 0, 16))

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        section.addView(text("PRODUTOS", 14f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        section.addView(text("${items.size} itens", 11f, muted))
        content.addView(section, bottomMargin(-1, -2, 2, 0, 2, 10))

        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Nenhum produto cadastrado."
                textSize = 14f
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(30), dp(20), dp(30))
                background = rounded(panel, Color.rgb(55, 55, 55), 16)
            }
            content.addView(empty)
        } else {
            items.sortedBy { it.estoque }.forEach { product ->
                content.addView(productCard(product), bottomMargin(-1, -2, 0, 0, 0, 11))
            }
        }

        val back = TextView(this).apply {
            text = "‹     VOLTAR AO PAINEL"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedGradient(orange, Color.rgb(215, 83, 0), 28)
            setOnClickListener { finish() }
            contentDescription = "Voltar ao painel"
        }
        content.addView(back, bottomMargin(-1, -2, 0, 0, 0, 12))
    }

    private fun productCard(product: AdminProduto): View {
        val lowStock = product.estoque <= product.estoqueMinimo
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(13), dp(12), dp(13))
            background = rounded(panel, if (lowStock) Color.rgb(170, 83, 0) else Color.rgb(72, 55, 34), 17)
        }

        val productIcon = TextView(this).apply {
            text = productIconFor(product.nome)
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(orangeLight)
            background = rounded(Color.rgb(25, 21, 16), Color.rgb(76, 49, 20), 13)
        }
        card.addView(productIcon, LinearLayout.LayoutParams(dp(54), dp(64)))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(8), 0)
        }
        info.addView(text(product.nome, 14f, white, true))
        info.addView(text("${product.categoria}  •  ${money.format(product.preco)}", 11f, muted).apply {
            setPadding(0, dp(4), 0, dp(7))
        })
        val stockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = TextView(this).apply {
            text = if (lowStock) "!" else "▦"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = rounded(if (lowStock) Color.rgb(255, 177, 0) else orange, null, 50)
        }
        stockRow.addView(dot, LinearLayout.LayoutParams(dp(22), dp(22)))
        stockRow.addView(text(
            if (lowStock) "  Estoque baixo: ${product.estoque}" else "  Estoque atual: ${product.estoque}",
            11f, if (lowStock) orangeLight else white, true
        ))
        info.addView(stockRow)
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        // Não permitir lançar/baixar quantidades sem supervisão; entrada passa pela conferência.
        val conference = TextView(this).apply {
            text = "CONFERIR"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(8), dp(11), dp(8), dp(11))
            background = rounded(Color.rgb(54, 33, 12), orange, 14)
            setOnClickListener {
                startActivity(Intent(this@MainActivityEstoque, MainActivityConferenciaEstoque::class.java))
            }
        }
        card.addView(conference)

        return card
    }

    private fun stockButton(label: String, primary: Boolean, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (primary) white else muted)
            background = if (primary) roundedGradient(orange, Color.rgb(224, 91, 0), 10)
                else rounded(Color.rgb(25, 25, 25), Color.rgb(76, 76, 76), 10)
            setOnClickListener { action() }
            contentDescription = if (primary) "Adicionar uma unidade" else "Retirar uma unidade"
        }

    private fun buildFloatingNav(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(Color.rgb(14, 14, 14), orange, 28)
            elevation = dp(12).toFloat()
        }
        val navItems = listOf(
            Triple("⌂", "Início", "home"),
            Triple("▣", "Estoque", "stock"),
            Triple("◇", "Vendas", "sales"),
            Triple("☰", "Mais", "more")
        )
        navItems.forEach { (symbol, label, key) ->
            val active = key == "stock"
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(5), dp(4), dp(5))
                background = if (active) rounded(Color.rgb(39, 24, 10), Color.rgb(104, 56, 10), 18) else null
                setOnClickListener {
                    when (key) {
                        "home" -> startActivity(Intent(this@MainActivityEstoque, MainActivitycomprasonline::class.java))
                        "sales" -> {
                            if (UsuarioLocalRepository(this@MainActivityEstoque).temPermissao(
                                    RcarLocalStore.currentEmail(this@MainActivityEstoque), "ver_vendas"
                                )) startActivity(Intent(this@MainActivityEstoque, MainActivityVendas::class.java))
                            else Toast.makeText(this@MainActivityEstoque, "Seu perfil não tem permissão para ver vendas.", Toast.LENGTH_SHORT).show()
                        }
                        "more" -> startActivity(Intent(this@MainActivityEstoque, MainActivityPainelInterno::class.java))
                    }
                }
            }
            item.addView(TextView(this).apply {
                text = symbol
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(if (active) orange else muted)
            })
            item.addView(TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (active) orange else muted)
            })
            outer.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(5), dp(18), dp(12))
            setBackgroundColor(bg)
            addView(outer, LinearLayout.LayoutParams(-1, -2))
        }
        return container
    }

    private fun statBlock(value: String, label: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(text(value, 20f, orange, true))
        box.addView(text(label, 9f, muted, true).apply { setPadding(0, dp(2), 0, 0) })
        return box
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun rounded(color: Int, stroke: Int?, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun roundedGradient(start: Int, end: Int, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Color.rgb(255, 151, 22))
        }

    private fun bottomMargin(
        width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int
    ) = LinearLayout.LayoutParams(width, height).apply {
        setMargins(dp(left), dp(top), dp(right), dp(bottom))
    }

    private fun productIconFor(name: String): String {
        val n = name.lowercase(Locale.ROOT)
        return when {
            "óleo" in n || "oleo" in n -> "◉"
            "filtro" in n -> "▤"
            "freio" in n || "pastilha" in n -> "◈"
            "amortecedor" in n || "suspensão" in n || "suspensao" in n -> "↕"
            else -> "⚙"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
