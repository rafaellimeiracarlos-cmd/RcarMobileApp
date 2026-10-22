package com.example.rcarpremium

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog

/** Sessão local persistente para o protótipo offline.
 * Não é um token seguro de backend; autenticação remota deve ser feita no servidor.
 * Navegar, fechar ou reabrir o app não encerra a conta.
 */
object SessaoRcar {
    data class Conta(val email: String, val nome: String, val papel: String)

    fun atual(context: Context): Conta? {
        val email = RcarLocalStore.currentEmail(context)?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: return null
        val repo = UsuarioLocalRepository(context)
        if (!repo.existeUsuario(email) || !repo.usuarioAtivo(email) || repo.exigeTrocaSenha(email)) return null
        return Conta(email, repo.nomeDoUsuario(email) ?: email, repo.papel(email))
    }

    fun cliente(context: Context) = atual(context)?.papel == UsuarioLocalRepository.PAPEL_CLIENTE
    fun interno(context: Context) = atual(context)?.papel?.let {
        it == UsuarioLocalRepository.PAPEL_CEO || it in UsuarioLocalRepository.PAPEIS_EQUIPE
    } == true

    fun telaPerfil(context: Context): Class<out Activity> =
        if (interno(context)) MainActivityPerfilInterno::class.java else MainActivityHome::class.java

    fun abrirPerfil(activity: Activity) {
        activity.startActivity(Intent(activity, telaPerfil(activity)))
    }

    fun abrirDestino(activity: Activity, destino: String? = null, checkout: Boolean = false) {
        val conta = atual(activity) ?: return
        val tela = when {
            conta.papel != UsuarioLocalRepository.PAPEL_CLIENTE -> MainActivityPainelInterno::class.java
            checkout || destino == "carrinho" -> MainActivityItemCarrinho::class.java
            destino == "pedidos" -> MainActivityPedidos::class.java
            destino == "inicio" -> MainActivitycomprasonline::class.java
            else -> MainActivityHome::class.java
        }
        activity.startActivity(Intent(activity, tela).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        activity.finish()
    }

    /** Único fluxo de saída voluntária, chamado dos perfis de cliente/funcionário/CEO. */
    fun confirmarSaida(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Sair da conta?")
            .setMessage("Você continuará conectado até escolher Sair da conta. Deseja encerrar esta sessão agora?")
            .setNegativeButton("Continuar conectado", null)
            .setPositiveButton("Sair da conta") { _, _ ->
                SessaoLocalStore.encerrar(activity, RcarLocalStore.currentEmail(activity))
                RcarLocalStore.logout(activity)
                activity.startActivity(Intent(activity, MainActivitycomprasonline::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                activity.finish()
            }.show()
    }
}
