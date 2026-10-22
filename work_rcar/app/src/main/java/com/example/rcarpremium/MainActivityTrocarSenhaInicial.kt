package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Etapa obrigatória, antes da criação da sessão, para contas provisórias feitas pelo CEO. */
class MainActivityTrocarSenhaInicial : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("CONTA_EMAIL")?.trim().orEmpty()
        val repo = UsuarioLocalRepository(this)
        if (!repo.existeUsuario(email) || !repo.exigeTrocaSenha(email)) {
            finish()
            return
        }
        val orange = Color.rgb(255, 122, 0)
        val bg = Color.rgb(10, 11, 13)
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(38), dp(22), dp(26))
        }
        scroll.addView(content)
        setContentView(scroll)
        fun title(value: String, size: Float, color: Int, bold: Boolean = false) {
            content.addView(TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(8))
            })
        }
        title("RCARPREMIUM  •  SEGURANÇA", 13f, orange, true)
        title("Crie sua senha definitiva", 27f, Color.WHITE, true)
        title("Esta conta foi criada pelo CEO com senha provisória. Altere a senha antes de acessar qualquer área.", 14f, Color.LTGRAY)
        title(email, 14f, orange, true)
        fun field(hint: String): EditText = EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            RcarDialogUi.styleInput(this)
            content.addView(this, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(14) })
        }
        val old = field("Senha provisória recebida do CEO")
        val next = field("Nova senha")
        val repeat = field("Confirmar nova senha")
        title("Use no mínimo 10 caracteres, incluindo maiúscula, minúscula, número e símbolo.", 12f, Color.LTGRAY)
        val confirm = Button(this).apply {
            text = "ALTERAR SENHA E ENTRAR"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
        }
        content.addView(confirm, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18) })
        confirm.setOnClickListener {
            val antiga = old.text.toString()
            val nova = next.text.toString()
            if (nova != repeat.text.toString()) { repeat.error = "As senhas não coincidem"; return@setOnClickListener }
            if (!repo.trocarSenhaInicial(email, antiga, nova)) {
                Toast.makeText(this, "Confira a senha provisória e a segurança da nova senha.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Só concede sessão após autenticar com a senha definitiva.
            if (!repo.autenticar(email, nova)) { finish(); return@setOnClickListener }
            RcarLocalStore.login(this, email)
            SessaoLocalStore.iniciar(this, email, repo.papel(email))
            val destination = if (repo.papel(email) == UsuarioLocalRepository.PAPEL_CLIENTE)
                MainActivityHome::class.java else MainActivityPainelInterno::class.java
            startActivity(Intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            Toast.makeText(this, "Senha alterada com sucesso.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
