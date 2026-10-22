package com.example.rcarpremium

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

internal object MarketplaceUi {
    const val BG = 0xFF0B0C0F.toInt()
    const val CARD = 0xFF191B20.toInt()
    const val ORANGE = 0xFFFF7900.toInt()
    const val MUTED = 0xFFAEB4BD.toInt()
    fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).toInt()
    fun shape(context: Context, color: Int = CARD, border: Int = 0xFF353940.toInt(), radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(context, radius).toFloat(); setStroke(dp(context, 1), border)
    }
    fun text(context: Context, value: String, size: Float = 14f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    fun button(context: Context, label: String, primary: Boolean = false) = Button(context).apply {
        text = label; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE)
        background = shape(context, if (primary) ORANGE else 0xFF25272C.toInt(), if (primary) ORANGE else 0xFF494B50.toInt(), 22)
    }
    fun lp(context: Context, top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        if (top > 0) topMargin = dp(context, top)
    }
    fun card(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(context, 15), dp(context, 15), dp(context, 15), dp(context, 15))
        background = shape(context)
    }
    /** Barra inferior flutuante comum às novas telas do marketplace. */
    fun page(context: Context, title: String, subtitle: String, bottomLabel: String = "⌂  INÍCIO DA LOJA", back: () -> Unit, home: () -> Unit): Pair<FrameLayout, LinearLayout> {
        val frame = FrameLayout(context).apply { setBackgroundColor(BG) }
        val scroll = ScrollView(context).apply {
            clipToPadding = false; setPadding(0, 0, 0, dp(context, 108))
        }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 22), dp(context, 18), dp(context, 12))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Retorno no topo esquerdo, como nos demais fluxos do Android.
        val backBtn = button(context, "‹").apply {
            textSize = 28f
            contentDescription = "Voltar para a tela anterior"
            setOnClickListener { back() }
        }
        header.addView(backBtn, LinearLayout.LayoutParams(dp(context, 44), dp(context, 46)))
        // Espaçadores equilibram a logo central sem adicionar controles extras.
        header.addView(ImageView(context).apply {
            setImageResource(R.drawable.imglogo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "RCarPremium"
        }, LinearLayout.LayoutParams(0, dp(context, 53), 1f).apply {
            leftMargin = dp(context, 8)
            rightMargin = dp(context, 8)
        })
        header.addView(View(context), LinearLayout.LayoutParams(dp(context, 44), dp(context, 46)))
        body.addView(header, lp(context))
        body.addView(text(context, title, 24f, ORANGE, true), lp(context, 14))
        body.addView(text(context, subtitle, 13f, MUTED), lp(context, 6))
        scroll.addView(body); frame.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
            elevation = dp(context, 16).toFloat(); background = shape(context, 0xF9141519.toInt(), ORANGE, 24)
        }
        // Rodapé: somente ação de destino. "Voltar" já existe no cabeçalho.
        val footerHome = button(context, bottomLabel, true).apply { setOnClickListener { home() } }
        footer.addView(footerHome, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)
        ))
        frame.addView(footer, FrameLayout.LayoutParams(-1, dp(context, 62), Gravity.BOTTOM).apply {
            leftMargin=dp(context, 14); rightMargin=dp(context, 14); bottomMargin=dp(context, 16)
        })
        return frame to body
    }
}
