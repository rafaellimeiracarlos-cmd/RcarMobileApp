package com.example.rcarpremium

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Área restrita ao CEO para criação, bloqueio, remoção e concessão de permissões.
 * Esta tela usa os dados locais do modo de demonstração/offline.
 */
class MainActivityGestaoUsuarios : RcarBaseActivity() {

    private lateinit var root: LinearLayout
    private lateinit var usersContainer: LinearLayout
    private lateinit var statsRow: LinearLayout
    private lateinit var accountCount: TextView
    private lateinit var searchField: EditText
    private lateinit var repo: UsuarioLocalRepository
    private val filterChips = mutableMapOf<String, TextView>()

    private var searchQuery = ""
    private var statusFilter = FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = UsuarioLocalRepository(this)
        val currentEmail = SessaoRcar.atual(this)?.email.orEmpty()
        if (!repo.isCeo(currentEmail)) {
            Toast.makeText(this, "Apenas o CEO pode gerenciar contas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(COLOR_BACKGROUND)
            isFillViewport = true
            clipToPadding = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(COLOR_BACKGROUND)
        }
        scroll.addView(root)
        setContentView(scroll)
        render()
    }

    private fun render() {
        root.removeAllViews()
        filterChips.clear()
        buildHeader()
        buildOverview()
        buildCreateAccessCard()
        buildAccountDirectory()
        addSpace(18)
        addActionButton("←  Voltar ao painel", COLOR_SURFACE, COLOR_TEXT) { finish() }
    }

    private fun buildHeader() {
        val brandRow = horizontalLayout().apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(textView("RCAR", 14f, true, COLOR_ORANGE))
        brandRow.addView(textView("  /  CENTRAL DE CONTROLE", 11f, true, COLOR_MUTED))
        brandRow.addView(View(this), LinearLayout.LayoutParams(0, dp(1), 1f))
        brandRow.addView(badge("ACESSO CEO", COLOR_ORANGE_DARK, COLOR_ORANGE_LIGHT))
        root.addView(brandRow)
        addSpace(20)
        root.addView(textView("Gestão de acessos", 27f, true, COLOR_TEXT))
        addSpace(5)
        root.addView(textView("Controle quem entra no sistema e quais áreas cada funcionário pode utilizar.", 14f, false, COLOR_MUTED))
        addSpace(14)
        val alert = horizontalLayout().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(COLOR_ORANGE_TINT, 12, COLOR_ORANGE_DARK)
        }
        alert.addView(textView("●", 12f, true, COLOR_ORANGE))
        alert.addView(textView("  Área restrita ao CEO • alterações salvas neste dispositivo", 11f, false, COLOR_ORANGE_LIGHT))
        root.addView(alert)
        addSpace(18)
    }

    private fun buildOverview() {
        val users = repo.listarUsuarios()
        val active = users.count { it.ativo }
        val blocked = users.size - active
        statsRow = horizontalLayout()
        statsRow.addView(statCard("CONTAS", users.size.toString(), "cadastradas", COLOR_ORANGE), weightedParams())
        statsRow.addView(spaceView(8, horizontal = true))
        statsRow.addView(statCard("ATIVAS", active.toString(), "com acesso", COLOR_GREEN), weightedParams())
        statsRow.addView(spaceView(8, horizontal = true))
        statsRow.addView(statCard("BLOQUEADAS", blocked.toString(), "sem acesso", COLOR_RED), weightedParams())
        root.addView(statsRow)
        addSpace(22)
    }

    private fun statCard(title: String, value: String, caption: String, accent: Int): LinearLayout {
        val card = verticalLayout().apply {
            setPadding(dp(12), dp(13), dp(10), dp(12))
            background = rounded(COLOR_SURFACE, 16, COLOR_BORDER)
        }
        card.addView(textView(title, 9f, true, COLOR_MUTED))
        addSpaceTo(card, 7)
        card.addView(textView(value, 25f, true, accent))
        addSpaceTo(card, 2)
        card.addView(textView(caption, 10f, false, COLOR_MUTED))
        return card
    }

    private fun buildCreateAccessCard() {
        val card = cardLayout()
        card.addView(sectionEyebrow("NOVO USUÁRIO"))
        addSpaceTo(card, 6)
        card.addView(textView("Criar novo acesso", 20f, true, COLOR_TEXT))
        addSpaceTo(card, 4)
        card.addView(textView("Crie acessos para RH, Financeiro, Estoque, Vendas, Logística, Suporte, Compras, Gerência e Clientes. O novo usuário terá de trocar a senha provisória.", 12f, false, COLOR_MUTED))
        addSpaceTo(card, 17)

        val nameField = inputField("Nome completo", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val emailField = inputField("E-mail de acesso", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val passwordField = inputField("Senha inicial (mínimo de 8 caracteres)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(nameField)
        addSpaceTo(card, 10)
        card.addView(emailField)
        addSpaceTo(card, 10)
        card.addView(passwordField)
        addSpaceTo(card, 12)
        card.addView(textView("TIPO DE CONTA  •  toque para selecionar", 10f, true, COLOR_MUTED))
        addSpaceTo(card, 6)

        val roleSpinner = Spinner(this).apply {
            background = rounded(COLOR_SURFACE_ALT, 12, COLOR_BORDER)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            adapter = object : ArrayAdapter<String>(
                this@MainActivityGestaoUsuarios,
                android.R.layout.simple_spinner_item,
                listOf("Funcionário", "Gerente", "RH", "Financeiro", "Estoque", "Vendas", "Logística", "Suporte", "Compras", "Cliente")
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        text = "${getItem(position) ?: "Conta"}   ▾"
                        setTextColor(COLOR_TEXT)
                        textSize = 14f
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                    }
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(COLOR_TEXT)
                        setBackgroundColor(COLOR_SURFACE_ALT)
                        textSize = 14f
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    }
                }
            }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        card.addView(roleSpinner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
        addSpaceTo(card, 15)

        val createButton = actionButton("＋   CRIAR CONTA", COLOR_ORANGE, Color.WHITE)
        createButton.setOnClickListener {
            val role = when (roleSpinner.selectedItemPosition) {
                0 -> UsuarioLocalRepository.PAPEL_FUNCIONARIO
                1 -> UsuarioLocalRepository.PAPEL_GERENTE
                2 -> UsuarioLocalRepository.PAPEL_RH
                3 -> UsuarioLocalRepository.PAPEL_FINANCEIRO
                4 -> UsuarioLocalRepository.PAPEL_ESTOQUE
                5 -> UsuarioLocalRepository.PAPEL_VENDAS
                6 -> UsuarioLocalRepository.PAPEL_LOGISTICA
                7 -> UsuarioLocalRepository.PAPEL_SUPORTE
                8 -> UsuarioLocalRepository.PAPEL_COMPRAS
                else -> UsuarioLocalRepository.PAPEL_CLIENTE
            }
            val created = repo.criarUsuarioPeloCeo(
                nameField.text.toString(),
                emailField.text.toString(),
                passwordField.text.toString(),
                role
            )
            if (created) {
                Toast.makeText(this, "Conta criada. Senha provisória: troca obrigatória no primeiro acesso.", Toast.LENGTH_LONG).show()
                searchQuery = ""
                statusFilter = FILTER_ALL
                render()
            } else {
                Toast.makeText(
                    this,
                    "Confira nome, e-mail válido e único e senha com pelo menos 8 caracteres.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        card.addView(createButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        root.addView(card)
        addSpace(22)
    }

    private fun buildAccountDirectory() {
        root.addView(textView("Diretório de contas", 21f, true, COLOR_TEXT))
        addSpace(4)
        root.addView(textView("Pesquise, controle o acesso e configure permissões individuais.", 12f, false, COLOR_MUTED))
        addSpace(14)

        searchField = inputField("Buscar por nome, e-mail ou perfil", InputType.TYPE_CLASS_TEXT)
        searchField.setText(searchQuery)
        root.addView(searchField)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                renderUserCards()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        addSpace(10)

        val filters = horizontalLayout()
        listOf(FILTER_ALL to "Todas", FILTER_ACTIVE to "Ativas", FILTER_BLOCKED to "Bloqueadas").forEachIndexed { index, pair ->
            val chip = filterChip(pair.first, pair.second)
            filterChips[pair.first] = chip
            filters.addView(chip, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                if (index > 0) marginStart = dp(7)
            })
        }
        root.addView(filters)
        addSpace(13)

        accountCount = textView("CONTAS CADASTRADAS", 10f, true, COLOR_MUTED)
        root.addView(accountCount)
        addSpace(8)
        usersContainer = verticalLayout()
        root.addView(usersContainer)
        updateFilterChips()
        renderUserCards()
    }

    private fun filterChip(filter: String, title: String): TextView = TextView(this).apply {
        text = title
        textSize = 12f
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isClickable = true
        isFocusable = true
        setOnClickListener {
            statusFilter = filter
            updateFilterChips()
            renderUserCards()
        }
    }

    private fun updateFilterChips() {
        filterChips.forEach { (filter, chip) ->
            val selected = filter == statusFilter
            chip.setTextColor(if (selected) Color.WHITE else COLOR_MUTED)
            chip.background = rounded(
                if (selected) COLOR_ORANGE_DARK else COLOR_SURFACE,
                12,
                if (selected) COLOR_ORANGE else COLOR_BORDER
            )
        }
    }

    private fun renderUserCards() {
        if (!::usersContainer.isInitialized) return
        usersContainer.removeAllViews()
        val query = searchQuery.trim().lowercase()
        val filteredUsers = repo.listarUsuarios().filter { user ->
            val matchesText = query.isBlank() || user.nome.lowercase().contains(query) ||
                user.email.lowercase().contains(query) || user.papel.lowercase().contains(query)
            val matchesStatus = when (statusFilter) {
                FILTER_ACTIVE -> user.ativo
                FILTER_BLOCKED -> !user.ativo
                else -> true
            }
            matchesText && matchesStatus
        }.sortedWith(compareBy<UsuarioLocalRepository.UsuarioAdmin> { it.papel != UsuarioLocalRepository.PAPEL_CEO }.thenBy { it.nome.lowercase() })

        accountCount.text = "CONTAS CADASTRADAS  •  ${filteredUsers.size}"
        if (filteredUsers.isEmpty()) {
            val emptyCard = cardLayout()
            emptyCard.gravity = Gravity.CENTER
            emptyCard.addView(textView("⌕", 30f, true, COLOR_ORANGE))
            emptyCard.addView(textView("Nenhuma conta encontrada", 15f, true, COLOR_TEXT))
            addSpaceTo(emptyCard, 4)
            emptyCard.addView(textView("Tente outro termo ou altere o filtro selecionado.", 12f, false, COLOR_MUTED))
            usersContainer.addView(emptyCard)
            return
        }

        filteredUsers.forEach { user ->
            usersContainer.addView(userCard(user))
            usersContainer.addView(spaceView(12))
        }
    }

    private fun userCard(user: UsuarioLocalRepository.UsuarioAdmin): LinearLayout {
        val card = cardLayout()
        val isCeo = user.papel == UsuarioLocalRepository.PAPEL_CEO
        val isEmployee = user.papel in UsuarioLocalRepository.PAPEIS_EQUIPE

        val identityRow = horizontalLayout().apply { gravity = Gravity.CENTER_VERTICAL }
        val avatar = TextView(this).apply {
            val letter = user.nome.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            text = letter
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(COLOR_ORANGE_LIGHT)
            background = rounded(COLOR_ORANGE_DARK, 16, COLOR_ORANGE_DARK)
        }
        identityRow.addView(avatar, LinearLayout.LayoutParams(dp(48), dp(48)))
        val identity = verticalLayout().apply { setPadding(dp(11), 0, dp(6), 0) }
        identity.addView(textView(user.nome, 15f, true, COLOR_TEXT))
        addSpaceTo(identity, 3)
        identity.addView(textView(user.email, 11f, false, COLOR_MUTED))
        identityRow.addView(identity, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        identityRow.addView(badge(if (user.ativo) "ATIVO" else "BLOQUEADO", if (user.ativo) COLOR_GREEN_BG else COLOR_RED_BG, if (user.ativo) COLOR_GREEN else COLOR_RED))
        card.addView(identityRow)
        addSpaceTo(card, 13)

        val tags = horizontalLayout().apply { gravity = Gravity.CENTER_VERTICAL }
        tags.addView(badge(roleLabel(user.papel), COLOR_SURFACE_ALT, COLOR_TEXT))
        if (isCeo) {
            tags.addView(spaceView(6, horizontal = true))
            tags.addView(badge("CONTA PROTEGIDA", COLOR_ORANGE_TINT, COLOR_ORANGE_LIGHT))
        } else if (repo.exigeTrocaSenha(user.email)) {
            tags.addView(spaceView(6, horizontal = true))
            tags.addView(badge("SENHA PROVISÓRIA", COLOR_ORANGE_TINT, COLOR_ORANGE_LIGHT))
        }
        card.addView(tags)
        addSpaceTo(card, 12)
        card.addView(separator())
        addSpaceTo(card, 12)

        if (isCeo) {
            val protectedRow = horizontalLayout().apply { gravity = Gravity.CENTER_VERTICAL }
            protectedRow.addView(textView("▣", 18f, true, COLOR_ORANGE))
            val protectedCopy = verticalLayout().apply { setPadding(dp(9), 0, 0, 0) }
            protectedCopy.addView(textView("Conta administrativa protegida", 12f, true, COLOR_TEXT))
            addSpaceTo(protectedCopy, 3)
            protectedCopy.addView(textView("Esta tela não permite bloquear ou excluir contas CEO.", 11f, false, COLOR_MUTED))
            protectedRow.addView(protectedCopy)
            card.addView(protectedRow)
            return card
        }

        val actions = horizontalLayout()
        val toggleButton = actionButton(
            if (user.ativo) "Bloquear acesso" else "Reativar acesso",
            if (user.ativo) COLOR_SURFACE_ALT else COLOR_GREEN_BG,
            if (user.ativo) COLOR_TEXT else COLOR_GREEN
        )
        toggleButton.setOnClickListener {
            if (user.ativo) SessaoLocalStore.encerrar(this, user.email)
            repo.definirAtivo(user.email, !user.ativo)
            Toast.makeText(this, if (user.ativo) "Acesso bloqueado." else "Acesso reativado.", Toast.LENGTH_SHORT).show()
            renderUserCards()
            updateOverviewStats()
        }
        val deleteButton = actionButton("Excluir", COLOR_RED_BG, COLOR_RED)
        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir conta?")
                .setMessage("A conta ${user.email} e suas permissões locais serão removidas deste dispositivo. Essa ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir") { _, _ ->
                    repo.excluirUsuario(user.email)
                    Toast.makeText(this, "Conta removida.", Toast.LENGTH_SHORT).show()
                    render()
                }
                .show()
        }
        actions.addView(toggleButton, LinearLayout.LayoutParams(0, dp(43), 1f))
        actions.addView(spaceView(8, horizontal = true))
        actions.addView(deleteButton, LinearLayout.LayoutParams(dp(92), dp(43)))
        card.addView(actions)
        addSpaceTo(card, 10)
        val recovery = actionButton("🔑  Gerar token de recuperação • CEO", COLOR_ORANGE_TINT, COLOR_ORANGE_LIGHT)
        recovery.setOnClickListener { confirmarGeracaoToken(user) }
        card.addView(recovery, LinearLayout.LayoutParams(-1, dp(44)))

        if (isEmployee) {
            addSpaceTo(card, 17)
            card.addView(textView("Permissões operacionais", 14f, true, COLOR_TEXT))
            addSpaceTo(card, 3)
            card.addView(textView("As alterações abaixo são salvas imediatamente.", 11f, false, COLOR_MUTED))
            addSpaceTo(card, 10)

            val permissionItems = listOf(
                PermissionItem("cadastrar_produtos", "Cadastrar produtos", "Criar e editar os próprios produtos"),
                PermissionItem("gerenciar_estoque", "Alterar estoque", "Consultar e ajustar quantidades"),
                PermissionItem("conferir_recebimento", "Conferir recebimentos", "Enviar contagem e NF-e para aprovação do CEO"),
                PermissionItem("lancar_notas", "Lançar notas fiscais", "Gerente registra XML e avisa CEO; estoque continua pendente"),
                PermissionItem("ver_vendas", "Consultar vendas", "Acessar o painel de vendas"),
                PermissionItem("gerenciar_pedidos", "Gerenciar pedidos", "Editar notas e atualizar status dos pedidos"),
                PermissionItem("pausar_anuncios", "Solicitar pausa de anúncio", "Pedir ao CEO para retirar um anúncio da loja"),
                PermissionItem("registrar_vendas", "Registrar venda", "Vender produto, baixar estoque e registrar faturamento"),
                PermissionItem("solicitar_reposicao_estoque", "Solicitar reposição", "Enviar pedidos de compra ao CEO")
            )
            val grid = verticalLayout()
            permissionItems.chunked(2).forEach { rowItems ->
                val row = horizontalLayout()
                rowItems.forEachIndexed { index, item ->
                    val tile = permissionTile(user.email, item)
                    row.addView(tile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                        if (index > 0) marginStart = dp(8)
                    })
                }
                if (rowItems.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(1), 1f))
                grid.addView(row)
                addSpaceTo(grid, 8)
            }
            card.addView(grid)
        } else {
            addSpaceTo(card, 10)
            card.addView(textView("Conta de cliente: não possui permissões administrativas.", 11f, false, COLOR_MUTED))
        }
        return card
    }

    private fun confirmarGeracaoToken(user: UsuarioLocalRepository.UsuarioAdmin) {
        AlertDialog.Builder(this)
            .setTitle("Gerar token para ${user.nome}?")
            .setMessage("O código será mostrado uma única vez e expira em 15 minutos. Só pode ser usado neste dispositivo enquanto o sistema estiver offline. Não compartilhe por canais públicos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Gerar token") { _, _ ->
                val currentCeo = SessaoRcar.atual(this)?.email.orEmpty()
                val token = repo.emitirTokenRecuperacao(currentCeo, user.email)
                if (token == null) {
                    Toast.makeText(this, "Somente um CEO autenticado pode emitir token para outra conta.", Toast.LENGTH_LONG).show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Token gerado • ${user.nome}")
                        .setMessage("Conta: ${user.email}\n\n$token\n\nVálido por 15 minutos, uso único. A pessoa deve abrir 'Esqueci minha senha' no mesmo aparelho e informar este token. Outro token invalida o anterior.")
                        .setNegativeButton("Fechar", null)
                        .setPositiveButton("Copiar código") { _, _ ->
                            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Token temporário RCarPremium", token))
                            Toast.makeText(this, "Token copiado. Evite compartilhar a área de transferência.", Toast.LENGTH_LONG).show()
                        }.show()
                }
            }.show()
    }

    private data class PermissionItem(val key: String, val title: String, val description: String)

    private fun permissionTile(email: String, item: PermissionItem): LinearLayout {
        val tile = verticalLayout().apply {
            setPadding(dp(9), dp(9), dp(9), dp(9))
            background = rounded(COLOR_SURFACE_ALT, 12, COLOR_BORDER)
        }
        val check = CheckBox(this).apply {
            text = item.title
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT)
            buttonTintList = ColorStateList.valueOf(COLOR_ORANGE)
            isChecked = repo.temPermissao(email, item.key)
        }
        check.setOnCheckedChangeListener { _, checked ->
            repo.definirPermissao(email, item.key, checked)
            Toast.makeText(this, if (checked) "Permissão concedida." else "Permissão removida.", Toast.LENGTH_SHORT).show()
        }
        tile.addView(check)
        addSpaceTo(tile, 3)
        tile.addView(textView(item.description, 10f, false, COLOR_MUTED))
        return tile
    }

    private fun updateOverviewStats() {
        // Atualizar contagens sem recriar o formulário em edição.
        // Os cartões estatísticos são os primeiros cartões logo após o cabeçalho.
        val users = repo.listarUsuarios()
        if (!::statsRow.isInitialized) return
        val cards = (0 until statsRow.childCount).mapNotNull { statsRow.getChildAt(it) as? LinearLayout }
        if (cards.size >= 3) {
            updateStatCard(cards[0], users.size.toString())
            updateStatCard(cards[1], users.count { it.ativo }.toString())
            updateStatCard(cards[2], users.count { !it.ativo }.toString())
        }
    }

    private fun updateStatCard(card: LinearLayout, value: String) {
        val valueView = card.getChildAt(2) as? TextView ?: return
        valueView.text = value
    }

    private fun inputField(hintText: String, type: Int): EditText = EditText(this).apply {
        hint = hintText
        inputType = type
        setSingleLine(true)
        textSize = 14f
        setTextColor(COLOR_TEXT)
        setHintTextColor(COLOR_MUTED)
        setPadding(dp(14), dp(5), dp(14), dp(5))
        background = rounded(COLOR_SURFACE_ALT, 12, COLOR_BORDER)
        minHeight = dp(52)
    }

    private fun cardLayout(): LinearLayout = verticalLayout().apply {
        setPadding(dp(15), dp(16), dp(15), dp(16))
        background = rounded(COLOR_SURFACE, 18, COLOR_BORDER)
    }

    private fun sectionEyebrow(value: String) = textView(value, 10f, true, COLOR_ORANGE)

    private fun textView(value: String, size: Float, bold: Boolean, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = true
    }

    private fun badge(value: String, backgroundColor: Int, textColor: Int): TextView = TextView(this).apply {
        text = value
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor)
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(6), dp(9), dp(6))
        background = rounded(backgroundColor, 30, backgroundColor)
    }

    private fun actionButton(title: String, backgroundColor: Int, textColor: Int): Button = Button(this).apply {
        text = title
        textSize = 11f
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor)
        background = rounded(backgroundColor, 13, if (backgroundColor == COLOR_ORANGE) COLOR_ORANGE else COLOR_BORDER)
        stateListAnimator = null
        elevation = 0f
        setPadding(dp(6), 0, dp(6), 0)
        minHeight = dp(42)
        minimumHeight = dp(42)
    }

    private fun addActionButton(title: String, backgroundColor: Int, textColor: Int, action: () -> Unit) {
        val button = actionButton(title, backgroundColor, textColor)
        button.setOnClickListener { action() }
        root.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
    }

    private fun verticalLayout() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun horizontalLayout() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), strokeColor)
    }

    private fun separator(): View = View(this).apply {
        setBackgroundColor(COLOR_BORDER)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun addSpace(heightDp: Int) = root.addView(spaceView(heightDp))

    private fun addSpaceTo(parent: LinearLayout, heightDp: Int) {
        parent.addView(spaceView(heightDp))
    }

    private fun spaceView(sizeDp: Int, horizontal: Boolean = false): View = View(this).apply {
        layoutParams = if (horizontal) {
            LinearLayout.LayoutParams(dp(sizeDp), dp(1))
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(sizeDp))
        }
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun roleLabel(role: String): String = when (role) {
        UsuarioLocalRepository.PAPEL_CEO -> "CEO"
        UsuarioLocalRepository.PAPEL_FUNCIONARIO -> "FUNCIONÁRIO"
        UsuarioLocalRepository.PAPEL_GERENTE -> "GERENTE"
        UsuarioLocalRepository.PAPEL_RH -> "RH"
        UsuarioLocalRepository.PAPEL_FINANCEIRO -> "FINANCEIRO"
        UsuarioLocalRepository.PAPEL_ESTOQUE -> "ESTOQUE"
        UsuarioLocalRepository.PAPEL_VENDAS -> "VENDAS"
        UsuarioLocalRepository.PAPEL_LOGISTICA -> "LOGÍSTICA"
        UsuarioLocalRepository.PAPEL_SUPORTE -> "SUPORTE"
        UsuarioLocalRepository.PAPEL_COMPRAS -> "COMPRAS"
        else -> "CLIENTE"
    }

    companion object {
        private const val FILTER_ALL = "TODAS"
        private const val FILTER_ACTIVE = "ATIVAS"
        private const val FILTER_BLOCKED = "BLOQUEADAS"

        private val COLOR_BACKGROUND = Color.rgb(10, 11, 13)
        private val COLOR_SURFACE = Color.rgb(24, 25, 28)
        private val COLOR_SURFACE_ALT = Color.rgb(34, 35, 39)
        private val COLOR_BORDER = Color.rgb(52, 53, 58)
        private val COLOR_TEXT = Color.rgb(247, 247, 249)
        private val COLOR_MUTED = Color.rgb(166, 168, 174)
        private val COLOR_ORANGE = Color.rgb(255, 106, 0)
        private val COLOR_ORANGE_DARK = Color.rgb(99, 43, 13)
        private val COLOR_ORANGE_TINT = Color.rgb(42, 27, 19)
        private val COLOR_ORANGE_LIGHT = Color.rgb(255, 157, 86)
        private val COLOR_GREEN = Color.rgb(107, 226, 155)
        private val COLOR_GREEN_BG = Color.rgb(22, 55, 39)
        private val COLOR_RED = Color.rgb(255, 115, 115)
        private val COLOR_RED_BG = Color.rgb(65, 28, 31)
    }
}
