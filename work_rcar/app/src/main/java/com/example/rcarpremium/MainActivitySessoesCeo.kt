package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

/**
 * Auditoria de acessos visível somente ao CEO.
 * Os registros são locais e servem para acompanhamento no dispositivo atual.
 */
class MainActivitySessoesCeo : RcarBaseActivity() {

    private enum class AccessFilter(val label: String) {
        ALL("Todos"), ACTIVE("Ativos"), CLIENTS("Clientes"), STAFF("Equipe"), CEOS("CEOs")
    }

    private lateinit var root: LinearLayout
    private lateinit var statsGrid: LinearLayout
    private lateinit var recordsContainer: LinearLayout
    private lateinit var recordsCount: TextView
    private lateinit var searchInput: EditText
    private lateinit var filterContainer: LinearLayout

    private val statValues = mutableMapOf<String, TextView>()
    private val durationViews = mutableMapOf<String, TextView>()
    private val handler = Handler(Looper.getMainLooper())
    private var currentFilter = AccessFilter.ALL
    private var searchQuery = ""
    private var tickCount = 0

    private val ticker = object : Runnable {
        override fun run() {
            updateLiveDurations()
            tickCount++
            // Atualiza novos acessos periodicamente sem recriar o campo de pesquisa.
            if (tickCount % 5 == 0) refreshData()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentEmail = RcarLocalStore.currentEmail(this).orEmpty()
        if (!UsuarioLocalRepository(this).isCeo(currentEmail)) {
            Toast.makeText(this, "Apenas o CEO pode consultar a auditoria.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        window.statusBarColor = color("#0B0B0B")
        window.navigationBarColor = color("#0B0B0B")
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            setBackgroundColor(color("#0B0B0B"))
            clipToPadding = false
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        buildHeader()
        buildSummarySection()
        buildSearchAndFilters()
        buildRecordsSection()
        buildFooterActions()
        refreshData()
        handler.post(ticker)
    }

    private fun buildHeader() {
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded("#1D1D1D", 14, "#303030")
            contentDescription = "Voltar"
            setOnClickListener { finish() }
        }
        topRow.addView(back, LinearLayout.LayoutParams(dp(44), dp(42)))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(label("RCAR PREMIUM", 12f, "#FF6B22", true))
        brand.addView(label("CENTRAL DE SEGURANÇA", 10f, "#9A9A9A", true).apply {
            letterSpacing = 0.06f
        })
        topRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val liveBadge = TextView(this).apply {
            text = "● AO VIVO"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color("#66E0A3"))
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded("#163025", 20, "#28533F")
        }
        topRow.addView(liveBadge)
        root.addView(topRow)

        root.addView(label("Auditoria de acessos", 27f, "#FFFFFF", true).apply {
            setPadding(0, dp(23), 0, dp(5))
        })
        root.addView(label(
            "Acompanhe sessões de clientes e equipe, horários de entrada e saída e duração de cada acesso.",
            13f,
            "#B6B6B6",
            false
        ).apply { setLineSpacing(dp(2).toFloat(), 1f) })

        val privacy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = rounded("#21170F", 12, "#54301C")
        }
        val privacyTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        privacyTitle.addView(label("▣", 16f, "#FF7A2F", true))
        privacyTitle.addView(label("  ACESSO RESTRITO AO CEO", 11f, "#FFC19F", true))
        privacy.addView(privacyTitle)
        privacy.addView(label("Dados locais deste dispositivo • atualização automática", 10f, "#C0A99B", false).apply {
            setPadding(dp(24), dp(4), 0, 0)
        })
        root.addView(privacy, marginParams(-1, -2, 0, 16, 0, 0))
    }

    private fun buildSummarySection() {
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label("RESUMO DE ACESSOS", 13f, "#F4F4F4", true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label("ATUALIZAÇÃO AUTOMÁTICA", 9f, "#898989", true))
        root.addView(heading, marginParams(-1, -2, 0, 10, 0, 0))

        statsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(statsGrid)
        val rowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowOne.addView(statCard("total", "REGISTROS", "0", "Histórico disponível", "#FF762B"), weightParams())
        rowOne.addView(spaceView(10), LinearLayout.LayoutParams(dp(10), 1))
        rowOne.addView(statCard("active", "ACESSOS ATIVOS", "0", "Sessões abertas agora", "#64DDA2"), weightParams())
        rowTwo.addView(statCard("clients", "CLIENTES HOJE", "0", "Sessões iniciadas hoje", "#79B8FF"), weightParams())
        rowTwo.addView(spaceView(10), LinearLayout.LayoutParams(dp(10), 1))
        rowTwo.addView(statCard("team", "EQUIPE HOJE", "0", "CEO e funcionários", "#FFB76B"), weightParams())
        statsGrid.addView(rowOne, marginParams(-1, -2, 0, 10, 0, 0))
        statsGrid.addView(rowTwo)
    }

    private fun buildSearchAndFilters() {
        root.addView(label("LOCALIZAR SESSÃO", 12f, "#EEEEEE", true).apply {
            setPadding(0, dp(22), 0, dp(9))
        })

        val searchFrame = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(10), 0)
            background = rounded("#191919", 14, "#333333")
        }
        searchFrame.addView(label("⌕", 25f, "#FF7A2F", false).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(30), dp(48)))
        searchInput = EditText(this).apply {
            hint = "Buscar por e-mail ou perfil..."
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(color("#777777"))
            setSingleLine(true)
            background = null
            setPadding(dp(7), 0, 0, 0)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        searchFrame.addView(searchInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(searchFrame)

        val filtersScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        filterContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(4))
        }
        filtersScroll.addView(filterContainer)
        root.addView(filtersScroll)
        renderFilters()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                renderRecords()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun buildRecordsSection() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("HISTÓRICO DE SESSÕES", 13f, "#F4F4F4", true), LinearLayout.LayoutParams(0, -2, 1f))
        recordsCount = label("0 registros", 11f, "#A5A5A5", false)
        header.addView(recordsCount)
        root.addView(header, marginParams(-1, -2, 0, 12, 0, 0))

        recordsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(recordsContainer)
    }

    private fun buildFooterActions() {
        val refresh = TextView(this).apply {
            text = "↻   ATUALIZAR HISTÓRICO"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded("#FF6B22", 14)
            setPadding(dp(14), dp(15), dp(14), dp(15))
            setOnClickListener {
                refreshData()
                Toast.makeText(this@MainActivitySessoesCeo, "Histórico atualizado", Toast.LENGTH_SHORT).show()
            }
        }
        val floatingActions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(11), dp(11), dp(11))
            background = rounded("#101215", 22, "#FF7A00")
            elevation = dp(14).toFloat()
        }
        floatingActions.addView(refresh, LinearLayout.LayoutParams(-1, -2))

        root.addView(floatingActions, marginParams(-1, -2, 0, 16, 0, 0))

        root.addView(label("O histórico é armazenado neste dispositivo. Para auditoria centralizada, use um servidor seguro.", 10f, "#777777", false).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), 0)
        })
    }

    private fun statCard(key: String, title: String, initialValue: String, note: String, accent: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(14))
            background = rounded("#1B1B1B", 16, "#303030")
        }
        val accentLine = View(this).apply { setBackgroundColor(color(accent)) }
        card.addView(accentLine, LinearLayout.LayoutParams(dp(34), dp(3)))
        card.addView(label(title, 10f, "#A7A7A7", true).apply {
            setPadding(0, dp(10), 0, dp(3))
            letterSpacing = 0.04f
        })
        val value = label(initialValue, 26f, "#FFFFFF", true)
        card.addView(value)
        statValues[key] = value
        card.addView(label(note, 10f, "#878787", false).apply {
            setPadding(0, dp(3), 0, 0)
        })
        return card
    }

    private fun renderFilters() {
        filterContainer.removeAllViews()
        AccessFilter.values().forEach { filter ->
            val selected = currentFilter == filter
            val chip = TextView(this).apply {
                text = filter.label
                textSize = 12f
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (selected) Color.WHITE else color("#B5B5B5"))
                gravity = Gravity.CENTER
                setPadding(dp(15), dp(10), dp(15), dp(10))
                background = if (selected) rounded("#FF6B22", 24) else rounded("#1C1C1C", 24, "#343434")
                setOnClickListener {
                    currentFilter = filter
                    renderFilters()
                    renderRecords()
                }
            }
            filterContainer.addView(chip, marginParams(-2, -2, 0, 0, 8, 0))
        }
    }

    private fun refreshData() {
        val records = SessaoLocalStore.historico(this)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        statValues["total"]?.text = records.size.toString()
        statValues["active"]?.text = records.count { it.fim == 0L }.toString()
        statValues["clients"]?.text = records.count { it.inicio >= today && normalizeRole(it.papel) == "CLIENTE" }.toString()
        statValues["team"]?.text = records.count {
            it.inicio >= today && normalizeRole(it.papel) in setOf("CEO", "FUNCIONARIO", "FUNCIONÁRIO")
        }.toString()
        renderRecords(records)
    }

    private fun renderRecords(source: List<SessaoAcesso> = SessaoLocalStore.historico(this)) {
        durationViews.clear()
        recordsContainer.removeAllViews()

        val q = searchQuery.lowercase(Locale.ROOT)
        val filtered = source.filter { session ->
            val role = normalizeRole(session.papel)
            val matchesFilter = when (currentFilter) {
                AccessFilter.ALL -> true
                AccessFilter.ACTIVE -> session.fim == 0L
                AccessFilter.CLIENTS -> role == "CLIENTE"
                AccessFilter.STAFF -> role == "FUNCIONARIO" || role == "FUNCIONÁRIO"
                AccessFilter.CEOS -> role == "CEO"
            }
            val matchesSearch = q.isBlank() || session.email.lowercase(Locale.ROOT).contains(q) || session.papel.lowercase(Locale.ROOT).contains(q)
            matchesFilter && matchesSearch
        }

        recordsCount.text = "${filtered.size} ${if (filtered.size == 1) "registro" else "registros"}"
        if (filtered.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(28), dp(20), dp(28))
                background = rounded("#171717", 16, "#2D2D2D")
            }
            empty.addView(label("⌕", 30f, "#FF7A2F", false).apply { gravity = Gravity.CENTER })
            empty.addView(label("Nenhuma sessão encontrada", 15f, "#FFFFFF", true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, dp(4))
            })
            empty.addView(label("Tente alterar a pesquisa ou selecionar outro filtro.", 12f, "#939393", false).apply { gravity = Gravity.CENTER })
            recordsContainer.addView(empty)
            return
        }

        filtered.forEach { session -> recordsContainer.addView(sessionCard(session), marginParams(-1, -2, 0, 0, 0, 12)) }
    }

    private fun sessionCard(session: SessaoAcesso): View {
        val active = session.fim == 0L
        val role = normalizeRole(session.papel)
        val roleLabel = when (role) {
            "CEO" -> "CEO"
            "CLIENTE" -> "CLIENTE"
            "FUNCIONARIO", "FUNCIONÁRIO" -> "FUNCIONÁRIO"
            else -> session.papel.uppercase(Locale.ROOT)
        }
        val roleColor = when (role) {
            "CEO" -> "#FF8A45"
            "CLIENTE" -> "#7AB7FF"
            else -> "#D1A2FF"
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(14))
            background = rounded("#1B1B1B", 17, if (active) "#624027" else "#303030")
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val roleBadge = TextView(this).apply {
            text = roleLabel
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color(roleColor))
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = rounded("#2A211B", 20, roleColor)
        }
        top.addView(roleBadge)
        top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val statusBadge = TextView(this).apply {
            text = if (active) "● CONECTADO" else "● ENCERRADO"
            textSize = 9f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color(if (active) "#65DDA0" else "#999999"))
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(if (active) "#173124" else "#282828", 20, if (active) "#2A6648" else "#454545")
        }
        top.addView(statusBadge)
        card.addView(top)

        card.addView(label(session.email, 15f, "#FFFFFF", true).apply {
            setPadding(0, dp(12), 0, dp(3))
            setTextIsSelectable(true)
        })
        card.addView(label(if (active) "Esta sessão permanece aberta" else "Sessão finalizada", 11f, if (active) "#FFB58E" else "#969696", false))

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(15), 0, dp(13))
        }
        val entryColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        entryColumn.addView(label("ENTRADA", 9f, "#8F8F8F", true))
        entryColumn.addView(label(SessaoLocalStore.dataFormatada(session.inicio), 11f, "#E0E0E0", false).apply { setPadding(0, dp(4), 0, 0) })
        val exitColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        exitColumn.addView(label("SAÍDA", 9f, "#8F8F8F", true))
        exitColumn.addView(label(if (active) "Ainda conectado" else SessaoLocalStore.dataFormatada(session.fim), 11f, if (active) "#65DDA0" else "#E0E0E0", false).apply {
            setPadding(0, dp(4), 0, 0)
        })
        details.addView(entryColumn, LinearLayout.LayoutParams(0, -2, 1f))
        details.addView(spaceView(8), LinearLayout.LayoutParams(dp(8), 1))
        details.addView(exitColumn, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(details)

        card.addView(View(this).apply { setBackgroundColor(color("#343434")) }, LinearLayout.LayoutParams(-1, dp(1)))

        val durationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(11), 0, 0)
        }
        val timerIcon = TextView(this).apply {
            text = "◷"
            textSize = 23f
            setTextColor(color(if (active) "#FF7A2F" else "#9B9B9B"))
        }
        durationRow.addView(timerIcon, LinearLayout.LayoutParams(dp(30), -2))
        val durationLabel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        durationLabel.addView(label("TEMPO DE ACESSO", 9f, "#929292", true))
        val durationText = label(SessaoLocalStore.duracaoFormatada(session.inicio, session.fim), 17f, if (active) "#FF8A45" else "#E6E6E6", true)
        durationText.setPadding(0, dp(2), 0, 0)
        durationLabel.addView(durationText)
        durationRow.addView(durationLabel, LinearLayout.LayoutParams(0, -2, 1f))
        val idLabel = label("ID • ${session.id.takeLast(8).uppercase(Locale.ROOT)}", 9f, "#777777", false)
        durationRow.addView(idLabel)
        card.addView(durationRow)
        if (active) durationViews[session.id] = durationText
        return card
    }

    private fun updateLiveDurations() {
        val sessionsById = SessaoLocalStore.historico(this).associateBy { it.id }
        durationViews.forEach { (id, view) ->
            val session = sessionsById[id]
            if (session != null && session.fim == 0L) {
                view.text = SessaoLocalStore.duracaoFormatada(session.inicio, session.fim)
            }
        }
    }

    private fun normalizeRole(role: String): String = role.trim().uppercase(Locale.ROOT)

    private fun label(value: String, size: Float, hex: String, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color(hex))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = true
    }

    private fun rounded(fill: String, radiusDp: Int, stroke: String? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color(fill))
        if (stroke != null) setStroke(dp(1), color(stroke))
    }

    private fun color(hex: String): Int = Color.parseColor(hex)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun weightParams() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun spaceView(widthDp: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(1)) }
    private fun marginParams(width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(width, height).apply {
            setMargins(dp(left), dp(top), dp(right), dp(bottom))
        }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }
}
