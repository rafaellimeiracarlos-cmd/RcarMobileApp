package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Recuperação offline com token gerado por CEO no mesmo dispositivo. */
class MainActivityEsqueceuSenha : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val orange = Color.rgb(255, 122, 0)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(9, 10, 12)) }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(35), dp(22), dp(30))
        }
        scroll.addView(form)
        setContentView(scroll)
        fun label(value: String, size: Float, color: Int, bold: Boolean = false) {
            form.addView(TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(4), 0, dp(8))
            })
        }
        label("RCARPREMIUM • RECUPERAÇÃO", 13f, orange, true)
        label("Redefinir senha", 28f, Color.WHITE, true)
        label("Solicite um token ao CEO. Digite o código e escolha sua nova senha. O código só funciona no mesmo aparelho em que foi gerado, enquanto o sistema usa armazenamento local.", 14f, Color.LTGRAY)
        fun field(hintText: String, type: Int): EditText = EditText(this).apply {
            hint = hintText
            inputType = type
            RcarDialogUi.styleInput(this)
            form.addView(this, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        }
        val email = field("E-mail da conta", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        email.setText(intent.getStringExtra("CONTA_EMAIL") ?: "")
        val token = field("Token de recuperação (32 caracteres)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        val newPass = field("Nova senha", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val confirm = field("Confirmar nova senha", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        label("Nova senha: 10+ caracteres, maiúscula, minúscula, número e símbolo. Token válido por 15 minutos e uma utilização.", 12f, Color.LTGRAY)
        val reset = Button(this).apply {
            text = "REDEFINIR SENHA"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
        }
        form.addView(reset, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        reset.setOnClickListener {
            if (newPass.text.toString() != confirm.text.toString()) {
                confirm.error = "As senhas não coincidem"
                return@setOnClickListener
            }
            if (UsuarioLocalRepository(this).redefinirSenhaComToken(
                    email.text.toString(), token.text.toString(), newPass.text.toString())) {
                Toast.makeText(this, "Senha redefinida. Faça login novamente.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Token inválido, expirado, esgotado ou senha fora dos requisitos.", Toast.LENGTH_LONG).show()
            }
        }
        val back = Button(this).apply {
            text = "Voltar para o login"
            isAllCaps = false
            setTextColor(Color.WHITE)
        }
        form.addView(back, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        back.setOnClickListener { finish() }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
