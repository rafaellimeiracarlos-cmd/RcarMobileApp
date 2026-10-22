package com.example.rcarpremium

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.math.BigDecimal
import java.util.Locale

/** Importação revisada de mercadorias por NF-e/NFC-e XML. Nunca finaliza a entrada sem confirmação humana. */
class MainActivityImportarNota : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    private val white = Color.WHITE
    private val dim = Color.rgb(177, 184, 194)
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var urlField: EditText
    private lateinit var results: LinearLayout
    private var currentInvoice: NfeInvoice? = null
    private var xmlSelecionado: ByteArray? = null
    private val reviewRows = mutableListOf<ReviewRow>()

    private data class ReviewRow(
        val line: NfeLine,
        val check: CheckBox,
        val name: EditText,
        val category: EditText,
        val price: EditText,
        val quantity: EditText,
        var existing: AdminProduto?
    )

    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importDocument(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!authorized()) { finish(); return }
        drawScreen()
    }

    private fun authorized(): Boolean {
        val email = RcarLocalStore.currentEmail(this)
        val repo = UsuarioLocalRepository(this)
        if (email.isNullOrBlank() || !repo.usuarioAtivo(email) || !(repo.temPermissao(email, "cadastrar_produtos") || repo.temPermissao(email, "conferir_recebimento") || repo.temPermissao(email, "lancar_notas"))) {
            Toast.makeText(this, "Apenas CEO ou funcionário autorizado pode conferir notas.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun drawScreen() {
        val background = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0A0A0A.toInt()) }
        scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(40))
        }
        val logo = ImageView(this).apply { setImageResource(R.drawable.imglogo); scaleType = ImageView.ScaleType.FIT_CENTER }
        content.addView(logo, LinearLayout.LayoutParams(-1, dp(75)))
        content.addView(title("IMPORTAR NOTA FISCAL", 23f, orange, true), lp(10))
        content.addView(title("Leia o XML, faça a contagem física e envie ao CEO. O estoque só será atualizado após aprovação.", 13f, dim), lp(6))
        content.addView(action("← VOLTAR AO PAINEL", false) { finish() }, lp(12))

        val methods = card()
        methods.addView(title("ESCOLHA COMO IMPORTAR", 16f, white, true))
        methods.addView(action("📄  SELECIONAR XML OU DOCUMENTO", true) {
            pickDocument.launch(arrayOf("application/xml", "text/xml", "application/pdf", "text/plain", "application/octet-stream"))
        }, lp(12))
        methods.addView(action("▣  ESCANEAR QR CODE", false) { scanQrCode() }, lp(8))
        methods.addView(title("Link HTTPS direto do arquivo XML ou URL obtida no QR Code", 12f, dim), lp(12))
        urlField = entry("https://exemplo.com/nota.xml", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        methods.addView(urlField, lp(6))
        methods.addView(action("↓  IMPORTAR XML DO LINK", false) { importByLink() }, lp(8))
        methods.addView(title("PDF/DANFE e páginas de consulta não são XML. Se você tiver apenas o PDF ou um QR de consulta, obtenha o XML com o fornecedor ou um serviço fiscal autorizado.", 12f, dim), lp(10))
        content.addView(methods, lp(14))

        val history = card()
        history.addView(title("HISTÓRICO DE NOTAS", 15f, white, true))
        history.addView(title("Notas aprovadas e lançadas aparecem aqui. Conferências pendentes ou devolvidas ficam na área de Conferência de Peças.", 12f, dim), lp(4))
        history.addView(action("VER IMPORTAÇÕES REALIZADAS", false) { showHistory() }, lp(8))
        content.addView(history, lp(12))

        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results, lp(12))
        scroll.addView(content)
        background.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        // Sem botão flutuante de volta: a tela já possui um retorno próprio.
        setContentView(background)
    }

    private fun importDocument(uri: Uri) {
        val mime = contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val name = runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else ""
            }.orEmpty()
        }.getOrDefault("").lowercase(Locale.ROOT)
        if (mime == "application/pdf" || name.endsWith(".pdf")) {
            AlertDialog.Builder(this).setTitle("Documento PDF / DANFE")
                .setMessage("O PDF não contém necessariamente os dados estruturados do XML da NF-e. Para evitar preços e quantidades errados, esta versão importa automaticamente apenas o XML. Solicite o XML ao emissor e selecione o arquivo aqui.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        if (name.isNotBlank() && !name.endsWith(".xml") && mime !in setOf("text/xml", "application/xml", "text/plain", "application/octet-stream")) {
            showError("Selecione o XML da NF-e ou NFC-e. Este formato de arquivo não é suportado.")
            return
        }
        runCatching {
            contentResolver.openInputStream(uri)?.use { readLimited(it) } ?: error("Não foi possível abrir o documento")
        }.onSuccess { parseAndPreview(it) }.onFailure { showError(it.message ?: "Falha ao abrir o XML.") }
    }

    private fun importByLink() {
        val value = urlField.text.toString().trim()
        if (value.matches(Regex("\\d{44}"))) {
            explainPublicConsultation(value)
            return
        }
        if (!value.startsWith("https://", true)) {
            showError("Informe um link HTTPS direto para um arquivo XML, ou escaneie o QR Code.")
            return
        }
        val parsed = runCatching { URL(value) }.getOrNull()
        val host = parsed?.host.orEmpty().lowercase(Locale.ROOT)
        val forbidden = host == "localhost" || host.endsWith(".local") || host.matches(Regex("(?:127|10)\\.\\d+\\.\\d+\\.\\d+")) ||
            host.startsWith("192.168.") || host.matches(Regex("172\\.(?:1[6-9]|2[0-9]|3[01])\\..*")) || host == "[::1]"
        if (parsed == null || forbidden || host.isBlank() || parsed.protocol != "https" || parsed.userInfo != null) {
            showError("Use um link HTTPS público e válido para um XML de nota fiscal.")
            return
        }
        val buttonText = "Consultando documento XML..."
        results.removeAllViews()
        results.addView(title(buttonText, 14f, dim))
        Thread {
            val outcome = runCatching {
                val connection = parsed.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 9000; connection.readTimeout = 12000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/xml,text/xml,application/octet-stream")
                    require(connection.responseCode == 200) { "O servidor retornou HTTP ${connection.responseCode}." }
                    readLimited(connection.inputStream)
                } finally { connection.disconnect() }
            }
            runOnUiThread { outcome.onSuccess { parseAndPreview(it) }.onFailure { showError(it.message ?: "Falha ao acessar o link") } }
        }.start()
    }

    private fun scanQrCode() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128).build()
        GmsBarcodeScanning.getClient(this, options).startScan()
            .addOnSuccessListener { scanned ->
                val value = scanned.rawValue.orEmpty().trim()
                if (value.isBlank()) showError("O código escaneado está vazio.")
                else {
                    urlField.setText(value)
                    if (value.startsWith("https://", true)) {
                        AlertDialog.Builder(this).setTitle("Código lido")
                            .setMessage("O QR Code contém um endereço. Se for um XML direto, tente importar. Se for o portal de consulta da SEFAZ, abra a página e obtenha o XML do emissor.")
                            .setPositiveButton("Tentar importar") { _, _ -> importByLink() }
                            .setNeutralButton("Abrir consulta") { _, _ -> openBrowser(value) }
                            .setNegativeButton("Cancelar", null).show()
                    } else if (value.matches(Regex("\\d{44}"))) explainPublicConsultation(value)
                    else showError("QR lido. Para importar mercadorias, selecione o arquivo XML da nota.")
                }
            }
            .addOnFailureListener { showError("Não foi possível ativar o scanner. Confira se o Google Play Services está instalado; também pode colar o link ou escolher um arquivo XML.") }
    }

    private fun openBrowser(value: String) {
        val uri = Uri.parse(value)
        if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { showError("Não há navegador disponível.") }
        }
    }

    private fun explainPublicConsultation(key: String) {
        AlertDialog.Builder(this).setTitle("Chave de acesso reconhecida")
            .setMessage("A chave de 44 dígitos permite consultar a nota, mas não fornece automaticamente o XML completo. Para entrada no estoque, obtenha o arquivo XML com o fornecedor, por meio da SEFAZ ou de um serviço fiscal autorizado e selecione-o no app.")
            .setPositiveButton("Entendi", null).show()
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > NfeImportParser.MAX_BYTES) error("Arquivo maior que 4 MB. Importe uma nota de cada vez.")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun parseAndPreview(bytes: ByteArray) {
        runCatching { NfeImportParser.parse(bytes) }
            .onSuccess { invoice ->
                if (NfeImportStore.alreadyImported(this, invoice) || EstoqueConferenciaStore.hasInvoice(this, invoice.importId, invoice.hash)) {
                    showError("Esta NF-e já foi lançada ou está em conferência (pendente/devolvida).")
                } else { xmlSelecionado = bytes.copyOf(); renderInvoice(invoice) }
            }
            .onFailure { showError("Não foi possível interpretar o XML: ${it.message}") }
    }

    private fun renderInvoice(invoice: NfeInvoice) {
        currentInvoice = invoice
        reviewRows.clear()
        results.removeAllViews()
        val intro = card()
        intro.addView(title("NOTA IDENTIFICADA", 18f, orange, true))
        intro.addView(title("NF nº ${invoice.number.ifBlank { "não informado" }} • ${invoice.items.size} itens", 14f, white, true), lp(8))
        intro.addView(title("Fornecedor: ${invoice.supplier}", 13f, dim), lp(6))
        intro.addView(title("CNPJ emissor: ${invoice.supplierCnpj.ifBlank { "não identificado" }}", 12f, dim), lp(4))
        intro.addView(title("Situação do XML: ${if (invoice.authorizationStatus == "100" || invoice.authorizationStatus == "150") "protocolo indica autorização" else "não há protocolo de autorização no XML"}", 12f, orange), lp(8))
        intro.addView(title("ATENÇÃO: confira autenticidade/situação na SEFAZ. Esta importação não valida assinatura digital nem consulta cancelamentos. Produtos novos não serão publicados automaticamente.", 12f, dim), lp(8))
        results.addView(intro)

        invoice.items.forEach { line ->
            var found = NfeImportStore.findMatch(this, invoice, line)
            val wholeUnits = try { line.quantity.intValueExact().takeIf { it > 0 } } catch (_: ArithmeticException) { null }
            val countUnit = line.unit.uppercase(Locale.ROOT) in setOf("UN", "UND", "UNID", "PC", "PÇ", "PCS", "KIT", "JG")
            val box = card()
            val selected = CheckBox(this).apply {
                text = if (found == null) "IMPORTAR • PRODUTO NOVO" else "ATUALIZAR ESTOQUE DO PRODUTO EXISTENTE"
                setTextColor(white); buttonTintList = android.content.res.ColorStateList.valueOf(orange)
                isChecked = wholeUnits != null && countUnit
            }
            box.addView(selected)
            box.addView(title("${line.line}. ${line.description}", 16f, white, true), lp(6))
            box.addView(title("Cód. fornecedor: ${line.vendorCode.ifBlank { "—" }}  •  EAN: ${line.gtin.ifBlank { "—" }}", 12f, dim), lp(5))
            box.addView(title("NF: ${line.quantity.toPlainString()} ${line.unit}  •  Custo un.: ${money(line.unitCost.toDouble())}", 13f, orange, true), lp(5))
            val matchHint = title(if (found == null) "Produto novo; anúncio ficará pausado." else "Vinculado: ${found.nome}. Preço atual será mantido.", 12f, Color.rgb(104, 221, 160))
            box.addView(matchHint, lp(5))
            if (wholeUnits == null || !countUnit) box.addView(title("Unidade fiscal '${line.unit}': confira a conversão para unidades físicas. Ajuste a quantidade e marque a linha conscientemente.", 12f, orange), lp(5))
            box.addView(title("Nome no catálogo", 12f, dim), lp(10))
            val name = entry("Nome do produto").apply { setText(found?.nome ?: line.description); isEnabled = found == null }
            box.addView(name, lp(4))
            box.addView(title("Categoria", 12f, dim), lp(9))
            val category = entry("Ex.: Freios").apply {
                setText(found?.categoria ?: suggestCategory(line.description)); isEnabled = found == null
            }
            box.addView(category, lp(4))
            box.addView(title(if (found == null) "Preço de venda (sugestão: custo + 35%; pode alterar)" else "Preço atual de venda (preservado)", 12f, dim), lp(9))
            val price = entry("R$ 0,00", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL).apply {
                val suggested = found?.preco ?: (line.unitCost.toDouble() * 1.35)
                setText(String.format(Locale.US, "%.2f", suggested)); isEnabled = found == null
            }
            box.addView(price, lp(4))
            box.addView(title("Quantidade realmente conferida (somente após CEO aprovar)", 12f, dim), lp(9))
            val quantity = entry("Quantidade inteira", InputType.TYPE_CLASS_NUMBER).apply { setText(wholeUnits?.toString().orEmpty()) }
            box.addView(quantity, lp(4))
            val review = ReviewRow(line, selected, name, category, price, quantity, found)
            box.addView(action("VINCULAR A UM PRODUTO EXISTENTE / CRIAR NOVO", false) {
                val products = AdminLocalStore.products(this)
                val labels = (listOf("+ CRIAR PRODUTO NOVO") + products.map { it.nome }).toTypedArray()
                AlertDialog.Builder(this).setTitle("Destino da linha ${line.line}")
                    .setItems(labels) { _, which ->
                        val selectedProduct = if (which == 0) null else products[which - 1]
                        review.existing = selectedProduct
                        review.name.setText(selectedProduct?.nome ?: line.description)
                        review.name.isEnabled = selectedProduct == null
                        review.category.setText(selectedProduct?.categoria ?: suggestCategory(line.description))
                        review.category.isEnabled = selectedProduct == null
                        val selling = selectedProduct?.preco ?: line.unitCost.toDouble() * 1.35
                        review.price.setText(String.format(Locale.US, "%.2f", selling))
                        review.price.isEnabled = selectedProduct == null
                        review.check.text = if (selectedProduct == null) "IMPORTAR • PRODUTO NOVO" else "ATUALIZAR ESTOQUE DO PRODUTO EXISTENTE"
                        matchHint.text = if (selectedProduct == null) "Produto novo; anúncio ficará pausado." else "Vinculado: ${selectedProduct.nome}. Preço atual será mantido."
                    }.setNegativeButton("Cancelar", null).show()
            }, lp(10))
            results.addView(box, lp(12))
            reviewRows.add(review)
        }
        results.addView(action("✓ ENVIAR CONTAGEM PARA APROVAÇÃO DO CEO", true) { confirmImport() }, lp(18))
        results.addView(title("A contagem enviada não altera o estoque. O CEO poderá aprovar, corrigir ou devolver a conferência; a NF-e será registrada como importada só depois da aprovação.", 12f, dim), lp(9))
        scroll.post { scroll.smoothScrollTo(0, results.top) }
    }

    private fun confirmImport() {
        val invoice = currentInvoice ?: return
        if (!authorized()) return
        val selected = reviewRows.filter { it.check.isChecked }
        if (selected.isEmpty()) { showError("Selecione um ou mais itens para importar."); return }
        val choices = runCatching {
            reviewRows.map { row ->
                val price = row.price.text.toString().trim().replace(',', '.').toDoubleOrNull()
                    ?: if (row.check.isChecked) error("Informe o preço de venda da linha ${row.line.line}.") else 0.0
                val quantity = row.quantity.text.toString().trim().toIntOrNull()
                    ?: if (row.check.isChecked) error("Informe uma quantidade inteira para a linha ${row.line.line}.") else 0
                NfeChoice(row.line, row.check.isChecked, row.name.text.toString().trim(), price, quantity,
                    row.category.text.toString().trim(), row.existing?.nome)
            }
        }.getOrElse { showError(it.message.orEmpty()); return }
        val units = choices.filter { it.selected }.sumOf { it.quantity.toLong() }
        val newCount = choices.count { it.selected && it.existingOriginalName == null }
        val updateCount = choices.count { it.selected && it.existingOriginalName != null }
        val note = EditText(this).apply {
            hint = "Observações da contagem (opcional)"
            setTextColor(white); setHintTextColor(Color.GRAY); textSize = 14f
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Enviar contagem ao CEO?")
            .setMessage("${invoice.supplier}\nNF nº ${invoice.number}\n\nProdutos novos: $newCount\nProdutos existentes: $updateCount\nUnidades contadas: $units\n\nO estoque NÃO será modificado agora. Aguarde a análise do CEO.")
            .setView(note)
            .setNegativeButton("Revisar", null)
            .setPositiveButton("ENVIAR") { _, _ ->
                runCatching { EstoqueConferenciaStore.submitInvoice(this, invoice, choices, note.text.toString()) }
                    .onSuccess { submitted ->
                        val archived = runCatching {
                            ArquivoNotasStore.registrarEntrada(this, invoice, xmlSelecionado, submitted.id, RcarLocalStore.currentEmail(this).orEmpty())
                        }.isSuccess
                        xmlSelecionado = null
                        currentInvoice = null; reviewRows.clear(); results.removeAllViews()
                        results.addView(card().apply {
                            addView(title("✓ CONFERÊNCIA ENVIADA", 19f, Color.rgb(112, 224, 151), true))
                            addView(title("NF nº ${submitted.documento} • ${submitted.linhas.size} itens • $units unidades contadas", 14f, white), lp(8))
                            addView(title("Pendente de aprovação do CEO. Nenhum produto foi lançado no estoque. ${if (archived) "Nota registrada na Central de Notas." else "Não foi possível arquivar o XML: confira a Central de Notas."}", 12f, dim), lp(6))
                            addView(action("VER CONFERÊNCIAS", true) {
                                startActivity(Intent(this@MainActivityImportarNota, MainActivityConferenciaEstoque::class.java))
                            }, lp(9))
                        })
                    }
                    .onFailure { showError("Conferência não enviada: ${it.message}") }
            }.show()
    }

    private fun showHistory() {
        val entries = NfeImportStore.history(this)
        AlertDialog.Builder(this).setTitle("Histórico de entradas por NF-e")
            .setMessage(if (entries.isEmpty()) "Nenhuma nota importada neste aparelho." else entries.takeLast(30).joinToString("\n\n"))
            .setPositiveButton("Fechar", null).show()
    }

    private fun suggestCategory(name: String): String = when {
        Regex("freio|pastilha|disco", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Freios"
        Regex("óleo|oleo|lubrificante", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Óleos e Lubrificantes"
        Regex("filtro", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Filtros"
        Regex("amortec|suspens|mola", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Suspensão"
        Regex("pneu|roda", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Pneus e Rodas"
        else -> "Automotivo"
    }

    private fun money(value: Double) = String.format(Locale("pt", "BR"), "R$ %.2f", value)
    private fun showError(message: String) { AlertDialog.Builder(this).setTitle("Importação de nota").setMessage(message).setPositiveButton("OK", null).show() }
    private fun entry(hintText: String, type: Int = InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
        hint = hintText; inputType = type; textSize = 14f; setSingleLine(true)
        setTextColor(white); setHintTextColor(Color.GRAY)
        setPadding(dp(12), dp(10), dp(12), dp(10)); background = backgroundShape(0xFF161616.toInt(), 0xFF484848.toInt(), 12)
    }
    private fun title(label: String, size: Float, tint: Int, bold: Boolean = false) = TextView(this).apply {
        text = label; textSize = size; setTextColor(tint); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun action(label: String, primary: Boolean, listener: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false; setTextColor(white)
        background = backgroundShape(if (primary) orange else 0xFF242424.toInt(), orange, 20)
        setOnClickListener { listener() }
    }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(15))
        background = backgroundShape(0xFF171717.toInt(), 0xFF9E4908.toInt(), 18)
    }
    private fun backgroundShape(color: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
