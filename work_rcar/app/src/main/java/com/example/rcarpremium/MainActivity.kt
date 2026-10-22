package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessaoRcar.atual(this) != null) {
            SessaoRcar.abrirDestino(this, intent.getStringExtra("DESTINO_APOS_LOGIN"), intent.getBooleanExtra("IR_CHECKOUT", false))
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Configuração de margens segura (Evita erros na linha 21)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. MAPEIA A LOGO DA TELA
        val imgLogo = findViewById<ImageView>(R.id.imgLogoTop)

        // 2. MAPEIA OS CAMPOS PADRÕES DO FORMULÁRIO (activity_main.xml)
        val editEmail = findViewById<EditText>(R.id.textEmail)
        val editSenha = findViewById<EditText>(R.id.textPassword)
        val btnEntrar = findViewById<Button>(R.id.btnEntrar)
        val btnCadastro = findViewById<TextView>(R.id.btnCadastro)
        val textEsqueceuSenha = findViewById<TextView>(R.id.textEsqSenha)

        // 3. MAPEIA OS NOVOS BOTÕES SOCIAIS COMO IMAGEBUTTON
        val btnGoogle = findViewById<ImageButton>(R.id.btnGoogle)
        val btnFacebook = findViewById<ImageButton>(R.id.btnFacebook)

        // 4. AÇÃO DO BOTÃO ENTRAR DA CAIXA BRANCA
        btnEntrar.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString().trim()

            if (email.isEmpty()) {
                editEmail.error = "Por favor, digite seu e-mail"
                editEmail.requestFocus()
            } else if (senha.isEmpty()) {
                editSenha.error = "Por favor, digite sua senha"
                editSenha.requestFocus()
            } else {
                val repositorio = UsuarioLocalRepository(this)
                val acessoValido = repositorio.autenticar(email, senha) &&
                    repositorio.papel(email) == UsuarioLocalRepository.PAPEL_CLIENTE

                if (acessoValido) {
                    if (repositorio.exigeTrocaSenha(email)) {
                        startActivity(Intent(this, MainActivityTrocarSenhaInicial::class.java)
                            .putExtra("CONTA_EMAIL", email))
                        finish()
                        return@setOnClickListener
                    }
                    Toast.makeText(this, "Acesso concedido!", Toast.LENGTH_SHORT).show()
                    RcarLocalStore.login(this, email)
                    SessaoLocalStore.iniciar(this, email, UsuarioLocalRepository.PAPEL_CLIENTE)
                    val destinoSolicitado = intent.getStringExtra("DESTINO_APOS_LOGIN")
                    val destino = when {
                        intent.getBooleanExtra("IR_CHECKOUT", false) || destinoSolicitado == "carrinho" -> MainActivityItemCarrinho::class.java
                        destinoSolicitado == "pedidos" -> MainActivityPedidos::class.java
                        else -> MainActivityHome::class.java
                    }
                    startActivity(Intent(this, destino))
                    finish()
                } else {
                    Toast.makeText(this, "E-mail ou senha incorretos.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. FUNCIONALIDADE DO BOTÃO GOOGLE
        btnGoogle.setOnClickListener {
            // Exibe um aviso rápido indicando que o botão funciona.
            // No futuro, aqui entrará a biblioteca oficial de autenticação Firebase/Google Auth.
            Toast.makeText(this, "Conectando ao Google...", Toast.LENGTH_SHORT).show()
        }

        // 6. FUNCIONALIDADE DO BOTÃO FACEBOOK
        btnFacebook.setOnClickListener {
            Toast.makeText(this, "Conectando ao Facebook...", Toast.LENGTH_SHORT).show()
        }

        // 7. SE O USUÁRIO CLICAR EM CADASTRE-SE DENTRO DO LOGIN
        btnCadastro.setOnClickListener {
            val intent = Intent(this, MainActivityCadastro::class.java)
            startActivity(intent)
        }

        // 8. SE O USUÁRIO CLICAR EM ESQUECI MINHA SENHA DENTRO DO LOGIN
        textEsqueceuSenha.setOnClickListener {
            val intent = Intent(this, MainActivityEsqueceuSenha::class.java)
            startActivity(intent)
        }

        // 9. AÇÃO DE CLIQUE NA LOGO (VOLTAR PARA A TELA DE COMPRAS)
        imgLogo.setOnClickListener {
            // Caso sua Activity de compras tenha outro nome (ex: HomeActivity), substitua aqui.
            val intent = Intent(this, MainActivitycomprasonline::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish() // Fecha a tela de login
        }
    }
}
