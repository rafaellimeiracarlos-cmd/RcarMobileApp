package com.example.rcarpremium

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Notas em pasta PRIVADA do app. Entrada de fornecedor e saída de pedido nunca se confundem. */
data class NotaArquivo(
    val id: String, val tipo: String, val numero: String, val chave: String,
    val fornecedor: String, val lancadoPor: String, val lancadoEm: Long,
    val conferenciaId: String, val pedidoId: String, val arquivo: String,
    val mime: String, val sha256: String, val lidaPeloCeoEm: Long = 0
)

object ArquivoNotasStore {
    const val ENTRADA = "ENTRADA"
    const val SAIDA = "SAIDA"
    private const val PREF = "rcar_arquivo_notas_v1"
    private const val KEY = "documentos"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun requireTeam(ctx: Context): String {
        val email = SessaoRcar.atual(ctx)?.email ?: error("Entre na conta para consultar notas.")
        val repo = UsuarioLocalRepository(ctx)
        require(repo.usuarioAtivo(email) && (repo.isCeo(email) || repo.temPermissao(email, "lancar_notas") ||
            repo.temPermissao(email, "conferir_recebimento"))) { "Sem permissão para lançamento de notas." }
        return email
    }
    private fun requireCeo(ctx: Context): String {
        val email = SessaoRcar.atual(ctx)?.email ?: error("Sessão inválida")
        require(UsuarioLocalRepository(ctx).isCeo(email) && UsuarioLocalRepository(ctx).usuarioAtivo(email)) {
            "Apenas o CEO pode vincular a nota de saída ao cliente."
        }
        return email
    }

    @Synchronized fun listar(ctx: Context): List<NotaArquivo> = try {
        val array = JSONArray(prefs(ctx).getString(KEY, "[]") ?: "[]")
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { o ->
            NotaArquivo(o.optString("id"), o.optString("tipo"), o.optString("numero"), o.optString("chave"),
                o.optString("fornecedor"), o.optString("lancadoPor"), o.optLong("lancadoEm"),
                o.optString("conferenciaId"), o.optString("pedidoId"), o.optString("arquivo"),
                o.optString("mime"), o.optString("sha256"), o.optLong("lidaPeloCeoEm"))
        }}.sortedByDescending { it.lancadoEm }
    } catch (_: Exception) { emptyList() }

    private fun persist(ctx: Context, docs: List<NotaArquivo>) {
        val array = JSONArray()
        docs.forEach { n -> array.put(JSONObject().apply {
            put("id", n.id); put("tipo", n.tipo); put("numero", n.numero); put("chave", n.chave)
            put("fornecedor", n.fornecedor); put("lancadoPor", n.lancadoPor); put("lancadoEm", n.lancadoEm)
            put("conferenciaId", n.conferenciaId); put("pedidoId", n.pedidoId); put("arquivo", n.arquivo)
            put("mime", n.mime); put("sha256", n.sha256); put("lidaPeloCeoEm", n.lidaPeloCeoEm)
        })}
        check(prefs(ctx).edit().putString(KEY, array.toString()).commit()) { "Falha ao salvar índice do arquivo." }
    }

    private fun armazenar(ctx: Context, tipo: String, content: ByteArray, extension: String): String {
        require(content.isNotEmpty() && content.size <= NfeImportParser.MAX_BYTES) { "Documento inválido ou maior que 4 MB" }
        val now = Calendar.getInstance()
        val folder = File(ctx.filesDir, "notas_fiscais/${tipo.lowercase(Locale.ROOT)}/${now.get(Calendar.YEAR)}/${(now.get(Calendar.MONTH)+1).toString().padStart(2, '0')}")
        check(folder.isDirectory || folder.mkdirs()) { "Não foi possível criar pasta de notas." }
        val target = File(folder, UUID.randomUUID().toString() + ".$extension")
        target.outputStream().use { it.write(content) }
        return target.relativeTo(ctx.filesDir).invariantSeparatorsPath
    }

    @Synchronized fun registrarEntrada(ctx: Context, nota: NfeInvoice, xml: ByteArray?, conferenciaId: String, funcionario: String): NotaArquivo {
        val actor = requireTeam(ctx)
        require(actor.equals(funcionario, true)) { "Responsável divergente" }
        val conferencia = EstoqueConferenciaStore.list(ctx).firstOrNull { it.id == conferenciaId }
            ?: error("Conferência não localizada")
        require(conferencia.notaId == nota.importId && conferencia.notaHash == nota.hash) { "Documento divergente da conferência" }
        val existente = listar(ctx).firstOrNull { it.tipo == ENTRADA && it.conferenciaId == conferenciaId }
        if (existente != null) return existente
        val path = if (xml == null) "" else armazenar(ctx, ENTRADA, xml, "xml")
        val entry = NotaArquivo(UUID.randomUUID().toString(), ENTRADA, nota.number, nota.key,
            nota.supplier, actor, System.currentTimeMillis(), conferenciaId, "", path,
            if (xml == null) "" else "application/xml", xml?.let(::sha) ?: nota.hash)
        persist(ctx, listar(ctx) + entry)
        return entry
    }

    @Synchronized fun anexarNotaPedido(ctx: Context, pedidoId: String, uri: Uri, numero: String, chave: String): NotaArquivo {
        val actor = requireCeo(ctx)
        require(pedidoId.isNotBlank() && PedidoLocalStore.listar(ctx).any { it.id == pedidoId }) { "Pedido não encontrado." }
        val name = ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else ""
        }.orEmpty().lowercase(Locale.ROOT)
        val mime = ctx.contentResolver.getType(uri).orEmpty()
        val pdf = name.endsWith(".pdf") || mime == "application/pdf"
        val xml = name.endsWith(".xml") || mime in listOf("application/xml", "text/xml")
        require(pdf || xml) { "Selecione um documento PDF/DANFE ou XML." }
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) { val n = input.read(buffer); if (n < 0) break; require(out.size() + n <= NfeImportParser.MAX_BYTES) { "Documento acima de 4 MB" }; out.write(buffer, 0, n) }
            out.toByteArray()
        } ?: error("Falha ao ler documento")
        require(if (pdf) bytes.size >= 5 && bytes.copyOfRange(0,5).toString(Charsets.US_ASCII) == "%PDF-" else bytes.toString(Charsets.UTF_8).trimStart().startsWith("<")) { "Formato real do arquivo não corresponde à extensão." }
        if (chave.isNotBlank()) require(chave.matches(Regex("[0-9]{44}"))) { "Chave da NF-e deve ter 44 dígitos" }
        val old = listar(ctx)
        require(old.none { it.tipo == SAIDA && it.pedidoId == pedidoId && it.sha256 == sha(bytes) }) { "Esta nota já foi anexada ao pedido." }
        val file = armazenar(ctx, SAIDA, bytes, if (pdf) "pdf" else "xml")
        val row = NotaArquivo(UUID.randomUUID().toString(), SAIDA, numero.trim().take(40), chave, "RCarPremium",
            actor, System.currentTimeMillis(), "", pedidoId, file,
            if (pdf) "application/pdf" else "application/xml", sha(bytes))
        persist(ctx, old + row)
        return row
    }

    fun paraPedido(ctx: Context, pedido: PedidoLocal): List<NotaArquivo> {
        val conta = SessaoRcar.atual(ctx) ?: return emptyList()
        val repo = UsuarioLocalRepository(ctx)
        if (!repo.isCeo(conta.email) && !repo.temPermissao(conta.email, "gerenciar_pedidos") &&
            !pedido.clienteEmail.equals(conta.email, true)) return emptyList()
        return listar(ctx).filter { it.tipo == SAIDA && it.pedidoId == pedido.id }
    }

    @Synchronized fun marcarLida(ctx: Context, id: String) {
        requireCeo(ctx)
        val old = listar(ctx)
        persist(ctx, old.map { if (it.id == id) it.copy(lidaPeloCeoEm = System.currentTimeMillis()) else it })
    }

    fun abrir(ctx: Context, row: NotaArquivo) {
        val conta = SessaoRcar.atual(ctx) ?: error("Entre na sua conta")
        val allowed = if (row.tipo == ENTRADA) {
            val r = UsuarioLocalRepository(ctx)
            r.isCeo(conta.email) || r.temPermissao(conta.email, "lancar_notas") || r.temPermissao(conta.email, "conferir_recebimento")
        } else {
            val pedido = PedidoLocalStore.listar(ctx).firstOrNull { it.id == row.pedidoId }
                ?: error("Pedido não encontrado")
            paraPedido(ctx, pedido).any { it.id == row.id }
        }
        require(allowed) { "Sem acesso a esta nota" }
        require(row.arquivo.isNotBlank()) { "XML não foi armazenado; somente os metadados estão disponíveis." }
        val f = File(ctx.filesDir, row.arquivo)
        require(f.isFile && f.canonicalPath.startsWith(File(ctx.filesDir, "notas_fiscais").canonicalPath + File.separator)) { "Documento inválido." }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.notas", f)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, row.mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(view, "Abrir nota fiscal"))
    }
}
