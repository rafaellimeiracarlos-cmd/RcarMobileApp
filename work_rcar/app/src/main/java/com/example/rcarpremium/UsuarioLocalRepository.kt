package com.example.rcarpremium

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Repositório local de usuários para modo demonstração/offline.
 * Senhas novas são armazenadas com PBKDF2; senhas antigas em texto puro são
 * migradas no primeiro login válido. Para produção, use autenticação no backend.
 */
class UsuarioLocalRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Migra uma única vez as contas de CEO criadas por versões anteriores.
        // Isso corrige contas que ficaram salvas como CLIENTE ou com uma senha antiga.
        migrarCredenciaisIniciaisCeoUmaVez()
        cadastrarAdminInicial()
        cadastrarSegundoAdminInicial()
        cadastrarFuncionarioInicial()
    }

    /**
     * Migração de compatibilidade para o modo demonstração local.
     * Na primeira execução desta versão, restaura as duas contas CEO predefinidas.
     * A marca de migração impede redefinir as senhas a cada abertura do aplicativo.
     */
    private fun migrarCredenciaisIniciaisCeoUmaVez() {
        val versaoMigracao = prefs.getInt(KEY_MIGRACAO_CEO, 0)
        if (versaoMigracao >= 1) return

        gravarContaCeoPadrao(
            email = CEO_EMAIL,
            nome = "Rafael Limeira Carlos (CEO)",
            senha = CEO_PASSWORD
        )
        gravarContaCeoPadrao(
            email = CEO_EMAIL_SECUNDARIO,
            nome = "Renan Carlos (CEO)",
            senha = CEO_PASSWORD_SECUNDARIO
        )
        prefs.edit().putInt(KEY_MIGRACAO_CEO, 1).apply()
    }

    private fun gravarContaCeoPadrao(email: String, nome: String, senha: String) {
        val normalized = normalizarEmail(email)
        prefs.edit()
            .putString(keyNome(normalized), nome)
            .putString(keyEmail(normalized), normalized)
            .putString(keySenha(normalized), hashSenha(senha))
            .putString(keyPapel(normalized), PAPEL_CEO)
            .putBoolean(keyAtivo(normalized), true)
            .putLong(keyCriadoEm(normalized), prefs.getLong(keyCriadoEm(normalized), System.currentTimeMillis()))
            .apply()
    }

    private fun cadastrarAdminInicial() = cadastrarContaCeoSeAusente(CEO_EMAIL, "Rafael Limeira Carlos (CEO)", CEO_PASSWORD)

    private fun cadastrarSegundoAdminInicial() = cadastrarContaCeoSeAusente(CEO_EMAIL_SECUNDARIO, "Renan Carlos (CEO)", CEO_PASSWORD_SECUNDARIO)

    /** Cria contas de demonstração somente quando ainda não existem; não sobrescreve senhas já cadastradas. */
    private fun cadastrarContaCeoSeAusente(email: String, nome: String, senha: String) {
        if (!prefs.contains(keySenha(email))) {
            prefs.edit()
                .putString(keyNome(email), nome)
                .putString(keyEmail(email), email)
                .putString(keySenha(email), hashSenha(senha))
                .putString(keyPapel(email), PAPEL_CEO)
                .putBoolean(keyAtivo(email), true)
                .putLong(keyCriadoEm(email), prefs.getLong(keyCriadoEm(email), System.currentTimeMillis()))
                .apply()
        } else if (!prefs.contains(keyPapel(email))) {
            prefs.edit().putString(keyPapel(email), PAPEL_CEO).apply()
        }
    }

    private fun cadastrarFuncionarioInicial() {
        val email = FUNCIONARIO_EMAIL
        if (!prefs.contains(keySenha(email))) {
            prefs.edit()
                .putString(keyNome(email), "Funcionário de demonstração")
                .putString(keyEmail(email), email)
                .putString(keySenha(email), hashSenha(FUNCIONARIO_PASSWORD))
                .putString(keyPapel(email), PAPEL_FUNCIONARIO)
                .apply()
        } else if (!prefs.contains(keyPapel(email))) {
            prefs.edit().putString(keyPapel(email), PAPEL_FUNCIONARIO).apply()
        }
    }

    /** Cadastro público: usuários criados pela tela de cadastro sempre são clientes. */
    fun cadastrar(nome: String, email: String, senha: String): Boolean {
        val emailNormalizado = normalizarEmail(email)
        if (nome.trim().isEmpty() || !emailValido(emailNormalizado) || senha.length < 8 ||
            prefs.contains(keySenha(emailNormalizado))) return false

        prefs.edit()
            .putString(keyNome(emailNormalizado), nome.trim())
            .putString(keyEmail(emailNormalizado), emailNormalizado)
            .putString(keySenha(emailNormalizado), hashSenha(senha))
            .putString(keyPapel(emailNormalizado), PAPEL_CLIENTE)
            .putLong(keyCriadoEm(emailNormalizado), System.currentTimeMillis())
            .apply()
        return true
    }

    fun autenticar(email: String, senha: String): Boolean {
        val emailNormalizado = normalizarEmail(email)
        val senhaArmazenada = prefs.getString(keySenha(emailNormalizado), null) ?: return false
        if (!usuarioAtivo(emailNormalizado)) return false
        if (verificarSenha(senha, senhaArmazenada)) {
            // Migra registros antigos que guardavam a senha em texto puro.
            if (!senhaArmazenada.startsWith(PASSWORD_PREFIX)) {
                prefs.edit().putString(keySenha(emailNormalizado), hashSenha(senha)).apply()
            }
            return true
        }
        return false
    }

    fun autenticarInterno(email: String, senha: String, papelEsperado: String): Boolean {
        val emailNormalizado = normalizarEmail(email)
        if (!autenticar(emailNormalizado, senha)) return false
        return papel(emailNormalizado) == papelEsperado
    }

    fun existeUsuario(email: String): Boolean = prefs.contains(keySenha(normalizarEmail(email)))

    fun nomeDoUsuario(email: String): String? = prefs.getString(keyNome(normalizarEmail(email)), null)

    fun papel(email: String): String = prefs.getString(keyPapel(normalizarEmail(email)), PAPEL_CLIENTE)
        ?: PAPEL_CLIENTE

    fun isCeo(email: String): Boolean = papel(email) == PAPEL_CEO


    data class UsuarioAdmin(val nome: String, val email: String, val papel: String, val ativo: Boolean)

    data class ClienteDetalhes(
        val nome: String,
        val email: String,
        val papel: String,
        val ativo: Boolean,
        val cpf: String,
        val rg: String,
        val telefone: String,
        val endereco: String,
        val numero: String,
        val bairro: String,
        val cidade: String,
        val estado: String,
        val cep: String,
        val complemento: String,
        val observacoes: String,
        val criadoEm: Long
    )

    fun listarUsuarios(): List<UsuarioAdmin> {
        val emails = prefs.all.keys.filter { it.startsWith("usuario_email_") }
            .mapNotNull { prefs.getString(it, null) }.distinct()
        return emails.map { email ->
            UsuarioAdmin(
                nome = nomeDoUsuario(email) ?: email,
                email = email,
                papel = papel(email),
                ativo = prefs.getBoolean(keyAtivo(email), true)
            )
        }.sortedWith(compareBy<UsuarioAdmin> { it.papel }.thenBy { it.nome.lowercase() })
    }

    /** Criado somente a partir da área interna do CEO. */
    fun criarUsuarioPeloCeo(nome: String, email: String, senha: String, papel: String): Boolean {
        val normalized = normalizarEmail(email)
        if (nome.isBlank() || !emailValido(normalized) || senha.length < 8 ||
            existeUsuario(normalized) || papel !in PAPEIS_CRIAVEIS) return false
        prefs.edit()
            .putString(keyNome(normalized), nome.trim())
            .putString(keyEmail(normalized), normalized)
            .putString(keySenha(normalized), hashSenha(senha))
            .putString(keyPapel(normalized), papel)
            .putBoolean(keyAtivo(normalized), true)
            .putBoolean(keyTrocaSenha(normalized), true)
            .putLong(keyCriadoEm(normalized), prefs.getLong(keyCriadoEm(normalized), System.currentTimeMillis()))
            .apply()
        return true
    }

    fun usuarioAtivo(email: String): Boolean = prefs.getBoolean(keyAtivo(normalizarEmail(email)), true)

    fun definirAtivo(email: String, ativo: Boolean): Boolean {
        val normalized = normalizarEmail(email)
        if (papel(normalized) == PAPEL_CEO || !existeUsuario(normalized)) return false
        prefs.edit().putBoolean(keyAtivo(normalized), ativo).apply()
        return true
    }

    fun definirPermissao(email: String, permissao: String, permitido: Boolean) {
        val normalized = normalizarEmail(email)
        require(papel(normalized) in PAPEIS_EQUIPE) { "Permissões individuais são apenas para contas de equipe." }
        require(permissao in PERMISSOES) { "Permissão desconhecida." }
        prefs.edit().putBoolean(keyPermissao(normalized, permissao), permitido).apply()
    }

    fun temPermissao(email: String?, permissao: String): Boolean {
        if (email.isNullOrBlank()) return false
        if (isCeo(email)) return true
        if (papel(email) !in PAPEIS_EQUIPE || !usuarioAtivo(email) || exigeTrocaSenha(email)) return false
        return prefs.getBoolean(keyPermissao(email, permissao), false)
    }

    fun excluirUsuario(email: String): Boolean {
        val normalized = normalizarEmail(email)
        if (papel(normalized) == PAPEL_CEO || !existeUsuario(normalized)) return false
        val editor = prefs.edit()
        listOf(
            keyNome(normalized), keyEmail(normalized), keySenha(normalized), keyPapel(normalized), keyAtivo(normalized),
            keyCpf(normalized), keyRg(normalized), keyTelefone(normalized), keyEndereco(normalized), keyNumero(normalized),
            keyBairro(normalized), keyCidade(normalized), keyEstado(normalized), keyCep(normalized), keyComplemento(normalized),
            keyObservacoes(normalized), keyCriadoEm(normalized), keyTrocaSenha(normalized),
            keyTokenHash(normalized), keyTokenExpira(normalized), keyTokenTentativas(normalized)
        ).forEach { editor.remove(it) }
        PERMISSOES.forEach { editor.remove(keyPermissao(normalized, it)) }
        editor.apply()
        return true
    }

    fun listarClientesDetalhados(): List<ClienteDetalhes> = listarUsuarios()
        .filter { it.papel == PAPEL_CLIENTE }
        .mapNotNull { obterDetalhesCliente(it.email) }
        .sortedBy { it.nome.lowercase() }

    fun obterDetalhesCliente(email: String): ClienteDetalhes? {
        val normalized = normalizarEmail(email)
        if (!existeUsuario(normalized)) return null
        garantirPerfilBasico(normalized)
        return ClienteDetalhes(
            nome = nomeDoUsuario(normalized) ?: normalized,
            email = normalized,
            papel = papel(normalized),
            ativo = usuarioAtivo(normalized),
            cpf = prefs.getString(keyCpf(normalized), "") ?: "",
            rg = prefs.getString(keyRg(normalized), "") ?: "",
            telefone = prefs.getString(keyTelefone(normalized), "") ?: "",
            endereco = prefs.getString(keyEndereco(normalized), "") ?: "",
            numero = prefs.getString(keyNumero(normalized), "") ?: "",
            bairro = prefs.getString(keyBairro(normalized), "") ?: "",
            cidade = prefs.getString(keyCidade(normalized), "") ?: "",
            estado = prefs.getString(keyEstado(normalized), "") ?: "",
            cep = prefs.getString(keyCep(normalized), "") ?: "",
            complemento = prefs.getString(keyComplemento(normalized), "") ?: "",
            observacoes = prefs.getString(keyObservacoes(normalized), "") ?: "",
            criadoEm = prefs.getLong(keyCriadoEm(normalized), 0L)
        )
    }

    fun salvarDetalhesCliente(
        email: String,
        cpf: String,
        rg: String,
        telefone: String,
        endereco: String,
        numero: String,
        bairro: String,
        cidade: String,
        estado: String,
        cep: String,
        complemento: String,
        observacoes: String
    ): Boolean {
        val normalized = normalizarEmail(email)
        if (!existeUsuario(normalized)) return false
        garantirPerfilBasico(normalized)
        prefs.edit()
            .putString(keyCpf(normalized), cpf.trim())
            .putString(keyRg(normalized), rg.trim())
            .putString(keyTelefone(normalized), telefone.trim())
            .putString(keyEndereco(normalized), endereco.trim())
            .putString(keyNumero(normalized), numero.trim())
            .putString(keyBairro(normalized), bairro.trim())
            .putString(keyCidade(normalized), cidade.trim())
            .putString(keyEstado(normalized), estado.trim().uppercase())
            .putString(keyCep(normalized), cep.trim())
            .putString(keyComplemento(normalized), complemento.trim())
            .putString(keyObservacoes(normalized), observacoes.trim())
            .apply()
        return true
    }

    private fun garantirPerfilBasico(email: String) {
        val editor = prefs.edit()
        if (!prefs.contains(keyCriadoEm(email))) editor.putLong(keyCriadoEm(email), System.currentTimeMillis())
        if (!prefs.contains(keyCpf(email))) editor.putString(keyCpf(email), "")
        if (!prefs.contains(keyRg(email))) editor.putString(keyRg(email), "")
        if (!prefs.contains(keyTelefone(email))) editor.putString(keyTelefone(email), "")
        if (!prefs.contains(keyEndereco(email))) editor.putString(keyEndereco(email), "")
        if (!prefs.contains(keyNumero(email))) editor.putString(keyNumero(email), "")
        if (!prefs.contains(keyBairro(email))) editor.putString(keyBairro(email), "")
        if (!prefs.contains(keyCidade(email))) editor.putString(keyCidade(email), "")
        if (!prefs.contains(keyEstado(email))) editor.putString(keyEstado(email), "")
        if (!prefs.contains(keyCep(email))) editor.putString(keyCep(email), "")
        if (!prefs.contains(keyComplemento(email))) editor.putString(keyComplemento(email), "")
        if (!prefs.contains(keyObservacoes(email))) editor.putString(keyObservacoes(email), "")
        editor.apply()
    }

    /** Contas criadas pelo CEO precisam substituir a senha inicial antes de iniciar sessão. */
    fun exigeTrocaSenha(email: String): Boolean =
        prefs.getBoolean(keyTrocaSenha(normalizarEmail(email)), false)

    fun trocarSenhaInicial(email: String, senhaAtual: String, novaSenha: String): Boolean {
        val normalized = normalizarEmail(email)
        if (!exigeTrocaSenha(normalized) || !autenticar(normalized, senhaAtual) ||
            !senhaForte(novaSenha) || senhaAtual == novaSenha) return false
        return prefs.edit()
            .putString(keySenha(normalized), hashSenha(novaSenha))
            .putBoolean(keyTrocaSenha(normalized), false)
            .remove(keyTokenHash(normalized)).remove(keyTokenExpira(normalized))
            .remove(keyTokenTentativas(normalized)).commit()
    }

    /** Apenas o CEO logado no próprio aparelho pode emitir tokens de recuperação. */
    fun emitirTokenRecuperacao(emailCeo: String, emailAlvo: String): String? {
        val ceo = normalizarEmail(emailCeo)
        val alvo = normalizarEmail(emailAlvo)
        if (SessaoRcar.atual(appContext)?.email != ceo ||
            !existeUsuario(ceo) || !usuarioAtivo(ceo) || !isCeo(ceo) ||
            exigeTrocaSenha(ceo) || !existeUsuario(alvo) || isCeo(alvo)) return null
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02X".format(it.toInt() and 255) }
        val hash = hashToken(alvo, token)
        val expiration = System.currentTimeMillis() + 15L * 60 * 1000
        val saved = prefs.edit().putString(keyTokenHash(alvo), hash)
            .putLong(keyTokenExpira(alvo), expiration)
            .putInt(keyTokenTentativas(alvo), 0).commit()
        return if (saved) token.chunked(4).joinToString("-") else null
    }

    /** Token de 128 bits: utilização única, no máximo cinco tentativas, validade de 15 minutos. */
    fun redefinirSenhaComToken(email: String, token: String, novaSenha: String): Boolean {
        val normalized = normalizarEmail(email)
        val cleanToken = token.filter { it.isLetterOrDigit() }.uppercase(java.util.Locale.ROOT)
        if (!existeUsuario(normalized) || isCeo(normalized) || !senhaForte(novaSenha) ||
            cleanToken.length != 32) return false
        val savedHash = prefs.getString(keyTokenHash(normalized), null) ?: return false
        val expiration = prefs.getLong(keyTokenExpira(normalized), 0L)
        val attempts = prefs.getInt(keyTokenTentativas(normalized), 0)
        if (System.currentTimeMillis() >= expiration || attempts >= 5) {
            invalidarToken(normalized)
            return false
        }
        if (!MessageDigest.isEqual(savedHash.toByteArray(Charsets.UTF_8),
                hashToken(normalized, cleanToken).toByteArray(Charsets.UTF_8))) {
            prefs.edit().putInt(keyTokenTentativas(normalized), attempts + 1).apply()
            if (attempts + 1 >= 5) invalidarToken(normalized)
            return false
        }
        return prefs.edit().putString(keySenha(normalized), hashSenha(novaSenha))
            .putBoolean(keyTrocaSenha(normalized), false)
            .remove(keyTokenHash(normalized)).remove(keyTokenExpira(normalized))
            .remove(keyTokenTentativas(normalized)).commit()
    }

    private fun invalidarToken(email: String) {
        prefs.edit().remove(keyTokenHash(email)).remove(keyTokenExpira(email))
            .remove(keyTokenTentativas(email)).apply()
    }

    private fun hashToken(email: String, token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((email + ":" + token).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun senhaForte(senha: String): Boolean =
        senha.length >= 10 && senha.any { it.isUpperCase() } &&
        senha.any { it.isLowerCase() } && senha.any { it.isDigit() } &&
        senha.any { !it.isLetterOrDigit() }

    private fun verificarSenha(senha: String, armazenada: String): Boolean {
        if (!armazenada.startsWith(PASSWORD_PREFIX)) return armazenada == senha
        return try {
            val partes = armazenada.split('$')
            if (partes.size != 4) return false
            val iterations = partes[1].toInt()
            val salt = Base64.decode(partes[2], Base64.NO_WRAP)
            val hashSalvo = Base64.decode(partes[3], Base64.NO_WRAP)
            MessageDigest.isEqual(hashSalvo, derivar(senha, salt, iterations, hashSalvo.size))
        } catch (_: Exception) {
            false
        }
    }

    private fun hashSenha(senha: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derivar(senha, salt, ITERATIONS, HASH_BYTES)
        return listOf(PASSWORD_PREFIX, ITERATIONS.toString(), Base64.encodeToString(salt, Base64.NO_WRAP), Base64.encodeToString(hash, Base64.NO_WRAP)).joinToString("\$")
    }

    private fun derivar(senha: String, salt: ByteArray, iterations: Int, tamanho: Int): ByteArray {
        val spec = PBEKeySpec(senha.toCharArray(), salt, iterations, tamanho * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun normalizarEmail(email: String) = email.trim().lowercase()
    private fun emailValido(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun keyNome(email: String) = "usuario_nome_$email"
    private fun keyEmail(email: String) = "usuario_email_$email"
    private fun keySenha(email: String) = "usuario_senha_$email"
    private fun keyPapel(email: String) = "usuario_papel_$email"
    private fun keyAtivo(email: String) = "usuario_ativo_$email"
    private fun keyCpf(email: String) = "usuario_cpf_$email"
    private fun keyRg(email: String) = "usuario_rg_$email"
    private fun keyTelefone(email: String) = "usuario_telefone_$email"
    private fun keyEndereco(email: String) = "usuario_endereco_$email"
    private fun keyNumero(email: String) = "usuario_numero_$email"
    private fun keyBairro(email: String) = "usuario_bairro_$email"
    private fun keyCidade(email: String) = "usuario_cidade_$email"
    private fun keyEstado(email: String) = "usuario_estado_$email"
    private fun keyCep(email: String) = "usuario_cep_$email"
    private fun keyComplemento(email: String) = "usuario_complemento_$email"
    private fun keyObservacoes(email: String) = "usuario_observacoes_$email"
    private fun keyCriadoEm(email: String) = "usuario_criado_em_$email"
    private fun keyTrocaSenha(email: String) = "usuario_troca_senha_$email"
    private fun keyTokenHash(email: String) = "usuario_token_hash_$email"
    private fun keyTokenExpira(email: String) = "usuario_token_expira_$email"
    private fun keyTokenTentativas(email: String) = "usuario_token_tentativas_$email"
    private fun keyPermissao(email: String, permissao: String) = "permissao_${permissao}_$email"

    companion object {
        private const val PREFS_NAME = "rcarpremium_usuarios"
        private const val KEY_MIGRACAO_CEO = "migracao_credenciais_ceo_v1"
        private const val PASSWORD_PREFIX = "pbkdf2"
        private const val ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val HASH_BYTES = 32

        const val PAPEL_CEO = "CEO"
        const val PAPEL_FUNCIONARIO = "FUNCIONARIO"
        const val PAPEL_GERENTE = "GERENTE"
        const val PAPEL_CLIENTE = "CLIENTE"
        const val PAPEL_RH = "RH"
        const val PAPEL_FINANCEIRO = "FINANCEIRO"
        const val PAPEL_ESTOQUE = "ESTOQUE"
        const val PAPEL_VENDAS = "VENDAS"
        const val PAPEL_LOGISTICA = "LOGISTICA"
        const val PAPEL_SUPORTE = "SUPORTE"
        const val PAPEL_COMPRAS = "COMPRAS"
        val PAPEIS_EQUIPE = setOf(
            PAPEL_FUNCIONARIO, PAPEL_GERENTE, PAPEL_RH, PAPEL_FINANCEIRO,
            PAPEL_ESTOQUE, PAPEL_VENDAS, PAPEL_LOGISTICA, PAPEL_SUPORTE, PAPEL_COMPRAS
        )
        val PAPEIS_CRIAVEIS = PAPEIS_EQUIPE + PAPEL_CLIENTE
        val PERMISSOES = listOf(
            "cadastrar_produtos",
            "gerenciar_estoque",
            "conferir_recebimento",
            "lancar_notas",
            "ver_vendas",
            "gerenciar_pedidos",
            "pausar_anuncios",
            "registrar_vendas",
            "solicitar_reposicao_estoque"
        )

        const val CEO_EMAIL = "rafaellimeiracarlos@gmail.com"
        const val CEO_PASSWORD = "006365Rafa@"
        const val CEO_EMAIL_SECUNDARIO = "renancarlos888@gmail.com"
        const val CEO_PASSWORD_SECUNDARIO = "00636527"
        const val FUNCIONARIO_EMAIL = "funcionario@rcarpremium.com"
        const val FUNCIONARIO_PASSWORD = "Funcionario@123"
    }
}
