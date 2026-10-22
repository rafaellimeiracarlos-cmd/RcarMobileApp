package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivityClientes : RcarBaseActivity() {

    private lateinit var repository: UsuarioLocalRepository
    private lateinit var container: LinearLayout
    private lateinit var busca: EditText
    private lateinit var totalClientes: TextView
    private lateinit var cadastrosCompletos: TextView

    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    private val bg = Color.rgb(9, 9, 9)
    private val panel = Color.rgb(20, 22, 24)
    private val panelSoft = Color.rgb(24, 27, 31)
    private val orange = Color.rgb(255, 122, 0)
    private val orangeSoft = Color.rgb(82, 49, 18)
    private val white = Color.WHITE
    private val muted = Color.rgb(162, 168, 177)
    private val line = Color.rgb(63, 67, 73)
    private val green = Color.rgb(68, 201, 129)
    private val yellow = Color.rgb(255, 187, 92)

    private var allClients: List<UsuarioLocalRepository.ClienteDetalhes> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clientes)

        repository = UsuarioLocalRepository(this)
        container = findViewById(R.id.containerListaClientes)
        busca = findViewById(R.id.edtBuscaClientes)
        totalClientes = findViewById(R.id.txtTotalClientes)
        cadastrosCompletos = findViewById(R.id.txtCadastrosCompletos)

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener { finish() }
        busca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = render(s?.toString().orEmpty())
        })

        reload()
    }

    private fun reload() {
        allClients = repository.listarClientesDetalhados()
        updateStats(allClients)
        render(busca.text?.toString().orEmpty())
    }

    private fun updateStats(source: List<UsuarioLocalRepository.ClienteDetalhes>) {
        totalClientes.text = source.size.toString()
        cadastrosCompletos.text = source.count { cadastroCompleto(it) }.toString()
    }

    private fun render(query: String) {
        container.removeAllViews()
        val normalized = query.trim().lowercase(Locale.getDefault())
        val filtered = allClients.filter { cliente ->
            normalized.isBlank() || listOf(
                cliente.nome,
                cliente.email,
                cliente.cpf,
                cliente.telefone,
                cliente.cidade,
                cliente.estado
            ).any { it.lowercase(Locale.getDefault()).contains(normalized) }
        }

        if (allClients.isEmpty()) {
            container.addView(emptyState(
                "Nenhum cliente cadastrado ainda",
                "Assim que os clientes criarem conta no app, eles aparecerão aqui para consulta administrativa."
            ))
            return
        }

        if (filtered.isEmpty()) {
            container.addView(emptyState(
                "Nenhum cliente encontrado",
                "Tente buscar pelo nome, e-mail, CPF ou telefone."
            ))
            return
        }

        filtered.forEachIndexed { index, cliente ->
            container.addView(clientCard(cliente), LinearLayout.LayoutParams(-1, -2).apply {
                if (index > 0) topMargin = dp(12)
            })
        }
    }

    private fun clientCard(cliente: UsuarioLocalRepository.ClienteDetalhes): View {
        val pedidos = PedidoLocalStore.listarDoCliente(this, cliente.email)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(panel, orangeSoft, 18f)
            isClickable = true
            isFocusable = true
            setOnClickListener { openClientDetails(cliente) }
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val avatar = TextView(this).apply {
            text = initials(cliente.nome)
            setTextColor(white)
            setTypeface(typeface, Typeface.BOLD)
            textSize = 15f
            gravity = Gravity.CENTER
            background = rounded(orange, null, 50f)
        }
        top.addView(avatar, LinearLayout.LayoutParams(dp(46), dp(46)))

        val nameBlock = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        nameBlock.addView(label(cliente.nome, 17f, white, true))
        nameBlock.addView(label(cliente.email, 12f, muted))
        top.addView(nameBlock, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })

        val status = pill(if (cliente.ativo) "ATIVO" else "INATIVO", if (cliente.ativo) green else yellow)
        top.addView(status)
        card.addView(top)

        val docLine = buildString {
            append(if (cliente.cpf.isBlank()) "CPF não informado" else "CPF ${cliente.cpf}")
            append("  •  ")
            append(if (cliente.telefone.isBlank()) "Telefone não informado" else cliente.telefone)
        }
        card.addView(label(docLine, 12f, muted), lp(top = 10))

        val address = formatShortAddress(cliente)
        card.addView(label(address, 12f, if (address == "Endereço não informado") muted else white), lp(top = 4))

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statMini("Pedidos", pedidos.size.toString()), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statMini("Total", money.format(pedidos.sumOf { it.total })), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        stats.addView(statMini("Cadastro", if (cadastroCompleto(cliente)) "Completo" else "Pendente"), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        card.addView(stats, lp(top = 12))

        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        footer.addView(label("Toque para ver dados completos", 12f, Color.rgb(255, 185, 115), true), LinearLayout.LayoutParams(0, -2, 1f))
        footer.addView(label("→", 18f, orange, true))
        card.addView(footer, lp(top = 12))

        return card
    }

    private fun openClientDetails(cliente: UsuarioLocalRepository.ClienteDetalhes) {
        val pedidos = PedidoLocalStore.listarDoCliente(this, cliente.email)
        val last = pedidos.maxByOrNull { it.criadoEm }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(8))
            background = rounded(Color.rgb(15, 17, 23), orange, 20f)
        }
        root.addView(label(cliente.nome, 21f, white, true))
        root.addView(label(cliente.email, 13f, muted), lp(top = 4))

        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chips.addView(pill(if (cliente.ativo) "Conta ativa" else "Conta inativa", if (cliente.ativo) green else yellow))
        chips.addView(pill(if (cadastroCompleto(cliente)) "Cadastro completo" else "Cadastro incompleto", orange), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        root.addView(chips, lp(top = 12))

        root.addView(sectionTitle("Dados cadastrais"), lp(top = 16))
        root.addView(infoRow("CPF", valueOrDash(cliente.cpf)), lp(top = 8))
        root.addView(infoRow("RG", valueOrDash(cliente.rg)), lp(top = 8))
        root.addView(infoRow("Telefone", valueOrDash(cliente.telefone)), lp(top = 8))
        root.addView(infoRow("Cliente desde", if (cliente.criadoEm > 0L) dateFmt.format(Date(cliente.criadoEm)) else "Não informado"), lp(top = 8))

        root.addView(sectionTitle("Endereço"), lp(top = 16))
        root.addView(infoRow("Rua", valueOrDash(cliente.endereco)), lp(top = 8))
        root.addView(infoRow("Número", valueOrDash(cliente.numero)), lp(top = 8))
        root.addView(infoRow("Bairro", valueOrDash(cliente.bairro)), lp(top = 8))
        root.addView(infoRow("Cidade / UF", listOf(cliente.cidade, cliente.estado).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "Não informado" }), lp(top = 8))
        root.addView(infoRow("CEP", valueOrDash(cliente.cep)), lp(top = 8))
        root.addView(infoRow("Complemento", valueOrDash(cliente.complemento)), lp(top = 8))

        root.addView(sectionTitle("Resumo do cliente"), lp(top = 16))
        root.addView(infoRow("Pedidos registrados", pedidos.size.toString()), lp(top = 8))
        root.addView(infoRow("Valor total comprado", money.format(pedidos.sumOf { it.total })), lp(top = 8))
        root.addView(infoRow("Último pedido", last?.let { "#${it.id} • ${PedidoLocalStore.dataFormatada(it.criadoEm)}" } ?: "Nenhum pedido registrado"), lp(top = 8))
        root.addView(infoRow("Observações", valueOrDash(cliente.observacoes)), lp(top = 8))

        val scroll = ScrollView(this).apply { addView(root) }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ficha do cliente")
            .setView(scroll)
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Editar dados", null)
            .create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialog.dismiss()
                openEditClient(cliente)
            }
        }
        dialog.show()
    }

    private fun openEditClient(cliente: UsuarioLocalRepository.ClienteDetalhes) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(10))
            background = rounded(Color.rgb(15, 17, 23), orange, 20f)
        }

        fun field(hint: String, value: String, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText {
            return EditText(this).apply {
                this.hint = hint
                setText(value)
                this.inputType = inputType
                RcarDialogUi.styleInput(this)
                form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            }
        }

        form.addView(label("Atualize as informações cadastrais do cliente para manter o CRM local do app mais completo.", 12f, muted))
        val cpf = field("CPF", cliente.cpf, InputType.TYPE_CLASS_TEXT)
        val rg = field("RG", cliente.rg, InputType.TYPE_CLASS_TEXT)
        val telefone = field("Telefone", cliente.telefone, InputType.TYPE_CLASS_PHONE)
        val endereco = field("Rua / endereço", cliente.endereco)
        val numero = field("Número", cliente.numero)
        val bairro = field("Bairro", cliente.bairro)
        val cidade = field("Cidade", cliente.cidade)
        val estado = field("UF", cliente.estado)
        val cep = field("CEP", cliente.cep, InputType.TYPE_CLASS_NUMBER)
        val complemento = field("Complemento", cliente.complemento)
        val observacoes = EditText(this).apply {
            hint = "Observações administrativas"
            setText(cliente.observacoes)
            RcarDialogUi.styleInput(this, multiline = true)
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        form.addView(observacoes, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar cliente")
            .setView(ScrollView(this).apply { addView(form) })
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = repository.salvarDetalhesCliente(
                    email = cliente.email,
                    cpf = cpf.text.toString(),
                    rg = rg.text.toString(),
                    telefone = telefone.text.toString(),
                    endereco = endereco.text.toString(),
                    numero = numero.text.toString(),
                    bairro = bairro.text.toString(),
                    cidade = cidade.text.toString(),
                    estado = estado.text.toString(),
                    cep = cep.text.toString(),
                    complemento = complemento.text.toString(),
                    observacoes = observacoes.text.toString()
                )
                if (ok) {
                    Toast.makeText(this, "Dados do cliente atualizados.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    reload()
                } else {
                    Toast.makeText(this, "Não foi possível salvar os dados do cliente.", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    private fun statMini(title: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = rounded(panelSoft, line, 14f)
        addView(label(title.uppercase(Locale.getDefault()), 10f, muted, true))
        addView(label(value, 13f, white, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun emptyState(title: String, message: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(18), dp(32), dp(18), dp(32))
        background = rounded(panel, orangeSoft, 18f)
        addView(label(title, 18f, white, true))
        addView(label(message, 13f, muted).apply { gravity = Gravity.CENTER_HORIZONTAL }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun sectionTitle(text: String): TextView = label(text.uppercase(Locale.getDefault()), 12f, orange, true)

    private fun infoRow(title: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(panelSoft, line, 14f)
        addView(label(title, 11f, muted, true))
        addView(label(value, 14f, white), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
    }

    private fun pill(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text
        setTextColor(if (color == yellow) Color.BLACK else white)
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = rounded(color, null, 40f)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(fill: Int, stroke: Int?, radius: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        if (top > 0) topMargin = dp(top)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "CL"
            parts.size == 1 -> parts.first().take(2).uppercase(Locale.getDefault())
            else -> (parts.first().first().toString() + parts.last().first().toString()).uppercase(Locale.getDefault())
        }
    }

    private fun cadastroCompleto(cliente: UsuarioLocalRepository.ClienteDetalhes): Boolean =
        cliente.cpf.isNotBlank() && cliente.endereco.isNotBlank() && cliente.cidade.isNotBlank() && cliente.cep.isNotBlank()

    private fun formatShortAddress(cliente: UsuarioLocalRepository.ClienteDetalhes): String {
        val rua = cliente.endereco.takeIf { it.isNotBlank() } ?: return "Endereço não informado"
        val numero = cliente.numero.takeIf { it.isNotBlank() }
        val bairro = cliente.bairro.takeIf { it.isNotBlank() }
        val cidadeUf = listOf(cliente.cidade.takeIf { it.isNotBlank() }, cliente.estado.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" - ")
        return listOfNotNull(
            listOfNotNull(rua, numero).joinToString(", ").ifBlank { null },
            bairro,
            cidadeUf.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
    }

    private fun valueOrDash(value: String): String = value.ifBlank { "Não informado" }
}
