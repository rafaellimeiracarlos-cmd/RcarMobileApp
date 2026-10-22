package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivityAcessoInterno : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessaoRcar.atual(this) != null) {
            SessaoRcar.abrirDestino(this)
            return
        }
        setContentView(R.layout.activity_acesso_interno)

        val papel = intent.getStringExtra(EXTRA_PAPEL) ?: UsuarioLocalRepository.PAPEL_FUNCIONARIO
        val isCeo = papel == UsuarioLocalRepository.PAPEL_CEO
        val titulo = findViewById<TextView>(R.id.txtTituloAcesso)
        val subtitulo = findViewById<TextView>(R.id.txtSubtituloAcesso)
        val botaoEntrar = findViewById<Button>(R.id.btnEntrarInterno)
        val usuario = findViewById<EditText>(R.id.editUsuarioInterno)
        val senha = findViewById<EditText>(R.id.editSenhaInterno)

        titulo.text = if (isCeo) "Login do CEO" else "Acesso da equipe"
        subtitulo.text = if (isCeo) "Acesso total ao sistema" else "RH, Financeiro, Gerência, Estoque, Vendas e outros perfis"
        botaoEntrar.text = "ENTRAR"

        botaoEntrar.setOnClickListener {
            val email = usuario.text.toString().trim()
            val password = senha.text.toString()
            when {
                email.isBlank() -> usuario.error = "Digite seu e-mail"
                password.isBlank() -> senha.error = "Digite sua senha"
                else -> {
                    val papelEsperado = if (isCeo) UsuarioLocalRepository.PAPEL_CEO
                        else UsuarioLocalRepository.PAPEL_FUNCIONARIO
                    val repo = UsuarioLocalRepository(this)
                    val valido = repo.autenticar(email, password) && (repo.papel(email) == papelEsperado ||
                        (!isCeo && repo.papel(email) in UsuarioLocalRepository.PAPEIS_EQUIPE))
                    if (valido) {
                        if (repo.exigeTrocaSenha(email)) {
                            startActivity(Intent(this, MainActivityTrocarSenhaInicial::class.java)
                                .putExtra("CONTA_EMAIL", email))
                            finish()
                            return@setOnClickListener
                        }
                        RcarLocalStore.login(this, email)
                        SessaoLocalStore.iniciar(this, email, repo.papel(email))
                        startActivity(Intent(this, MainActivityPainelInterno::class.java).apply {
                            putExtra(EXTRA_PAPEL, papelEsperado)
                        })
                        finish()
                    } else {
                        Toast.makeText(this, "E-mail, senha ou perfil incorreto.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        findViewById<android.widget.TextView>(R.id.btnRecuperarSenhaInterno).setOnClickListener {
            startActivity(Intent(this, MainActivityEsqueceuSenha::class.java).putExtra("CONTA_EMAIL", usuario.text.toString().trim()))
        }
        findViewById<ImageView>(R.id.btnVoltarInterno).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.imgLogoInterno).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_PAPEL = "PAPEL"
    }
}
