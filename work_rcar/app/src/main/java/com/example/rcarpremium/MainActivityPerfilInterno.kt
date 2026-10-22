package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Perfil de CEO e funcionário. A saída voluntária ocorre somente aqui. */
class MainActivityPerfilInterno : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conta = SessaoRcar.atual(this)
        if (conta == null) {
            startActivity(Intent(this, MainActivitycomprasonline::class.java))
            finish()
            return
        }
        if (conta.papel == UsuarioLocalRepository.PAPEL_CLIENTE) {
            SessaoRcar.abrirDestino(this)
            return
        }
        val laranja = Color.rgb(255, 122, 0)
        window.statusBarColor = Color.BLACK
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(9, 10, 12)) }
        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(35), dp(22), dp(38))
        }
        scroll.addView(corpo)
        setContentView(scroll)
        fun titulo(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) {
            corpo.addView(TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), dp(6), dp(4), dp(8))
            })
        }
        titulo("RCarPremium  •  MINHA CONTA", 14f, laranja, true)
        titulo("Meu perfil", 28f, Color.WHITE, true)
        titulo(if (conta.papel == UsuarioLocalRepository.PAPEL_CEO) "CEO • Acesso administrativo" else "${conta.papel} • Acesso conforme permissões", 15f, Color.LTGRAY)
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.rgb(23, 24, 27)); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), laranja)
            }
            setPadding(dp(17), dp(18), dp(17), dp(18))
        }
        fun linha(texto: String) = info.addView(TextView(this).apply {
            text = texto; setTextColor(Color.WHITE); textSize = 15f
            setPadding(0, dp(5), 0, dp(5))
        })
        linha("Nome: ${conta.nome}")
        linha("E-mail: ${conta.email}")
        linha("Perfil: ${conta.papel}")
        linha("● Conectado neste dispositivo")
        corpo.addView(info, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(20) })
        fun botao(rotulo: String, destaque: Boolean = false, acao: () -> Unit) {
            corpo.addView(Button(this).apply {
                text = rotulo; setAllCaps(false); textSize = 15f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(if (destaque) laranja else Color.rgb(37, 38, 41))
                    cornerRadius = dp(17).toFloat()
                    setStroke(dp(1), if (destaque) laranja else Color.rgb(65, 65, 65))
                }
                setOnClickListener { acao() }
            }, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(12) })
        }
        botao("← Voltar ao painel") { startActivity(Intent(this, MainActivityPainelInterno::class.java)); finish() }
        botao("⌂ Explorar loja") { startActivity(Intent(this, MainActivitycomprasonline::class.java)) }
        botao("▦ Explorar categorias") { startActivity(Intent(this, MainActivityCategorias::class.java)) }
        botao("🛒 Meu carrinho") { startActivity(Intent(this, MainActivityItemCarrinho::class.java)) }
        if (conta.papel == UsuarioLocalRepository.PAPEL_CEO || UsuarioLocalRepository(this).temPermissao(conta.email, "gerenciar_pedidos")) {
            botao("▤ Gestão de pedidos") { startActivity(Intent(this, MainActivityPedidos::class.java)) }
        }
        botao("Sair da conta", true) { SessaoRcar.confirmarSaida(this) }
        titulo("Sua conta permanece conectada mesmo após fechar o aplicativo. Para encerrar, use Sair da conta acima.", 12f, Color.LTGRAY)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
