package com.example.rcarpremium

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Navegação pública e privada: reconhece cliente, funcionário e CEO já conectados. */
object BottomNav {
    fun setup(activity: Activity, selected: String) {
        val ids = listOf(R.id.navInicio, R.id.navCategorias, R.id.navCarrinho, R.id.navPedidos, R.id.navPerfil)
        val keys = listOf("inicio", "categorias", "carrinho", "pedidos", "perfil")
        ids.forEachIndexed { index, id ->
            activity.findViewById<TextView>(id)?.apply {
                setTextColor(if (keys[index] == selected) Color.rgb(255, 122, 0) else Color.WHITE)
                setBackgroundResource(if (keys[index] == selected) R.drawable.bg_bottom_nav_selected else android.R.color.transparent)
                setOnClickListener { abrir(activity, keys[index]) }
            }
        }
    }

    fun abrir(activity: Activity, key: String) {
        if (CompraTesteCeoStore.ativo(activity)) {
            val tela = when (key) {
                "pedidos" -> MainActivityHistoricoTesteCeo::class.java
                "perfil" -> MainActivityPerfilInterno::class.java
                "carrinho" -> MainActivityItemCarrinho::class.java
                "categorias" -> MainActivityCategorias::class.java
                else -> MainActivitycomprasonline::class.java
            }
            activity.startActivity(Intent(activity, tela))
            return
        }
        if (key == "inicio" || key == "categorias") {
            val tela = if (key == "inicio") MainActivitycomprasonline::class.java else MainActivityCategorias::class.java
            activity.startActivity(Intent(activity, tela).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            return
        }
        val conta = SessaoRcar.atual(activity)
        if (conta == null) {
            AlertDialog.Builder(activity)
                .setTitle("Entre na sua conta")
                .setMessage("Faça login para acessar ${when (key) { "perfil" -> "seu perfil"; "pedidos" -> "seus pedidos"; else -> "seu carrinho" }}.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Fazer login") { _, _ ->
                    activity.startActivity(Intent(activity, MainActivity::class.java).putExtra("DESTINO_APOS_LOGIN", key))
                }.show()
            return
        }
        when (key) {
            "perfil" -> SessaoRcar.abrirPerfil(activity)
            "carrinho" -> activity.startActivity(Intent(activity, MainActivityItemCarrinho::class.java))
            "pedidos" -> {
                val repo = UsuarioLocalRepository(activity)
                if (conta.papel in UsuarioLocalRepository.PAPEIS_EQUIPE && !repo.temPermissao(conta.email, "gerenciar_pedidos")) {
                    AlertDialog.Builder(activity).setTitle("Permissão necessária")
                        .setMessage("Você continua conectado. O CEO precisa autorizar o gerenciamento de pedidos para o seu perfil de funcionário.")
                        .setPositiveButton("Entendi", null).show()
                } else activity.startActivity(Intent(activity, MainActivityPedidos::class.java))
            }
        }
    }
}
