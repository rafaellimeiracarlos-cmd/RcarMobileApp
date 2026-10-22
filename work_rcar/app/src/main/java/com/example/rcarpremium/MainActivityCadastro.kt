package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivityCadastro : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = Color.parseColor("#090A0B")
        window.navigationBarColor = Color.parseColor("#090A0B")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_main_cadastro)

        // Mantém o conteúdo dentro das áreas seguras do sistema.
        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. MAPEIA OS CAMPOS E ELEMENTOS DO XML
        val editNome = findViewById<EditText>(R.id.editNomeCadastro)
        val editEmail = findViewById<EditText>(R.id.editEmailCadastro)
        val editSenha = findViewById<EditText>(R.id.editSenhaCadastro)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarCadastro)
        val textVoltar = findViewById<TextView>(R.id.textVoltarLogin)
        val imgLogo = findViewById<ImageView>(R.id.img_logo)

        // Destaque visual no link para quem já tem uma conta.
        val textoLogin = getString(R.string.j_tem_uma_conta_fa_a_login)
        val textoDestacado = "Faça Login"
        val inicioDestaque = textoLogin.lastIndexOf(textoDestacado, ignoreCase = true)
        if (inicioDestaque >= 0) {
            val textoFormatado = SpannableString(textoLogin)
            textoFormatado.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF7A00")),
                inicioDestaque,
                inicioDestaque + textoDestacado.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textVoltar.text = textoFormatado
        }

        // 2. LÓGICA DE CADASTRO
        btnFinalizar.setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString().trim()

            if (nome.isEmpty()) {
                editNome.error = "Digite seu nome completo"
                editNome.requestFocus()
            } else if (email.isEmpty()) {
                editEmail.error = "Digite seu e-mail"
                editEmail.requestFocus()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.error = "Digite um e-mail válido"
                editEmail.requestFocus()
            } else if (senha.isEmpty()) {
                editSenha.error = "Crie uma senha"
                editSenha.requestFocus()
            } else if (senha.length < 8) {
                editSenha.error = "A senha precisa ter pelo menos 8 caracteres"
                editSenha.requestFocus()
            } else {
                val repositorio = UsuarioLocalRepository(this)
                val cadastrado = repositorio.cadastrar(nome, email, senha)

                if (cadastrado) {
                    Toast.makeText(
                        this,
                        "Cadastro de $nome realizado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Este e-mail já está cadastrado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // 3. NAVEGAÇÃO
        textVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        imgLogo.setOnClickListener {
            finish()
        }
    }
}
