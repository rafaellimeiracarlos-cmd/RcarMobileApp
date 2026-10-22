package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Consulta de testes, totalmente separado de vendas e pedidos reais. */
class MainActivityHistoricoTesteCeo : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!CompraTesteCeoStore.autorizado(this)) { finish(); return }
        mostrar()
    }

    private fun mostrar() {
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(18), px(18), px(18), px(32))
            setBackgroundColor(Color.rgb(9, 10, 12))
        }
        val title = TextView(this).apply {
            text = "PEDIDOS DE TESTE DO CEO"
            textSize = 23f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(255, 122, 0))
        }
        container.addView(title)
        container.addView(TextView(this).apply {
            text = "SIMULAÇÃO — sem cobrança, baixa no estoque ou faturamento."
            textSize = 13f; setTextColor(Color.LTGRAY)
            setPadding(0, px(8), 0, px(18))
        })
        val pedidos = CompraTesteCeoStore.historico(this)
        if (pedidos.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma compra de teste registrada. Abra o Modo de Teste no Painel do CEO para começar."
                textSize = 15f; setTextColor(Color.WHITE)
                setPadding(0, px(20), 0, px(20))
            })
        }
        pedidos.forEach { pedido ->
            val data = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale("pt", "BR")).format(Date(pedido.criadoEm))
            container.addView(TextView(this).apply {
                text = "${pedido.id} • $data\n${pedido.itens}\n${String.format(Locale("pt", "BR"), "R$ %.2f", pedido.total)} — ${pedido.pagamento} (simulado)\nEntrega teste: ${pedido.endereco}"
                setTextColor(Color.WHITE); textSize = 13f
                setPadding(px(14), px(14), px(14), px(14))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(24, 25, 27)); cornerRadius = px(14).toFloat()
                    setStroke(px(1), Color.rgb(255, 122, 0))
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(12) })
        }
        if (pedidos.isNotEmpty()) {
            container.addView(Button(this).apply {
                text = "Limpar histórico de testes"
                setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(72, 72, 72))
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivityHistoricoTesteCeo)
                        .setTitle("Apagar somente testes?")
                        .setMessage("Isto apaga apenas o histórico de simulações. Vendas reais não serão alteradas.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Apagar") { _, _ -> CompraTesteCeoStore.limparHistorico(this@MainActivityHistoricoTesteCeo); mostrar() }
                        .show()
                }
            })
        }
        container.addView(Button(this).apply {
            text = "Voltar ao painel do CEO"
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(255, 122, 0))
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        })
        setContentView(ScrollView(this).apply { addView(container) })
    }
}
