package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivityCategorias : RcarBaseActivity() {
    private lateinit var container: LinearLayout
    private lateinit var searchField: EditText
    private lateinit var totalLabel: TextView
    private var allProducts: List<AdminProduto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        findViewById<TextView>(R.id.btnVoltarCategorias).setOnClickListener { finish() }
        container = findViewById(R.id.listaCategorias)
        searchField = findViewById(R.id.edtBuscaCategoria)
        totalLabel = findViewById(R.id.txtTotalCategorias)
        allProducts = AdminLocalStore.products(this).filter { it.ativo && it.anuncioAtivo }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderCategories(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        renderCategories("")
        BottomNav.setup(this, "categorias")
    }

    override fun onResume() {
        super.onResume()
        if (::container.isInitialized) {
            allProducts = AdminLocalStore.products(this).filter { it.ativo && it.anuncioAtivo }
            renderCategories(searchField.text?.toString().orEmpty())
        }
    }

    private fun renderCategories(queryRaw: String) {
        val query = queryRaw.trim()
        val categories = CategoryCatalog.categories.filter { category ->
            query.isBlank() || category.name.contains(query, true) ||
                category.description.contains(query, true) || category.keywords.any { it.contains(query, true) }
        }

        totalLabel.text = if (query.isBlank()) {
            "${categories.size} categorias"
        } else {
            "${categories.size} resultado${if (categories.size == 1) "" else "s"}"
        }
        container.removeAllViews()

        if (categories.isEmpty()) {
            container.addView(createEmptyState(query))
            return
        }

        categories.chunked(2).forEachIndexed { rowIndex, rowCategories ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { if (rowIndex > 0) topMargin = dp(0) }
            }

            rowCategories.forEachIndexed { columnIndex, category ->
                val card = createCategoryCard(category)
                val params = LinearLayout.LayoutParams(0, dp(194), 1f).apply {
                    bottomMargin = dp(12)
                    if (columnIndex == 0) rightMargin = dp(6) else leftMargin = dp(6)
                }
                row.addView(card, params)
            }
            if (rowCategories.size == 1) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(194), 1f).apply {
                    leftMargin = dp(6)
                    bottomMargin = dp(12)
                })
            }
            container.addView(row)
        }
    }

    private fun createCategoryCard(category: ProductCategory): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(13), dp(13), dp(12))
            setBackgroundResource(R.drawable.bg_category_card)
            isClickable = true
            isFocusable = true
            contentDescription = "${category.name}. ${category.description}"
            setOnClickListener {
                startActivity(Intent(this@MainActivityCategorias, MainActivityListaProdutos::class.java).apply {
                    putExtra("CATEGORIA", category.name)
                })
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = TextView(this).apply {
            text = category.icon
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_category_icon)
            contentDescription = null
        }
        topRow.addView(icon, LinearLayout.LayoutParams(dp(46), dp(46)))
        val topSpacer = View(this)
        topRow.addView(topSpacer, LinearLayout.LayoutParams(0, dp(1), 1f))
        topRow.addView(TextView(this).apply {
            text = "↗"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 132, 18))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }, LinearLayout.LayoutParams(dp(24), dp(30)))
        card.addView(topRow)

        card.addView(TextView(this).apply {
            text = category.name
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 2
            setLineSpacing(dp(1).toFloat(), 1.0f)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })

        card.addView(TextView(this).apply {
            text = category.description
            textSize = 11f
            setTextColor(Color.rgb(157, 160, 166))
            maxLines = 2
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(5)
        })

        val spacer = View(this)
        card.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val divider = View(this).apply { setBackgroundColor(Color.rgb(45, 47, 51)) }
        card.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })

        val matchingCount = CategoryCatalog.matchingProducts(category.name, allProducts).size
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        footer.addView(TextView(this).apply {
            text = if (matchingCount > 0) "$matchingCount produto${if (matchingCount == 1) "" else "s"}" else "Explorar itens"
            textSize = 10f
            setTextColor(Color.rgb(255, 148, 38))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        footer.addView(TextView(this).apply {
            text = "→"
            textSize = 17f
            setTextColor(Color.rgb(255, 132, 18))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.WRAP_CONTENT))
        card.addView(footer)
        return card
    }

    private fun createEmptyState(query: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(32), dp(24), dp(32))
        background = GradientDrawable().apply {
            setColor(Color.rgb(22, 24, 27))
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.rgb(44, 47, 52))
        }
        addView(TextView(this@MainActivityCategorias).apply {
            text = "⌕"
            textSize = 38f
            setTextColor(Color.rgb(255, 132, 18))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(TextView(this@MainActivityCategorias).apply {
            text = "Nenhuma categoria encontrada"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })
        addView(TextView(this@MainActivityCategorias).apply {
            text = if (query.isBlank()) "Tente novamente mais tarde." else "Não encontramos resultados para \"$query\". Tente outro termo."
            textSize = 12f
            setTextColor(Color.rgb(157, 160, 166))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
