package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton

class MainActivityHome : RcarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contaAtual = SessaoRcar.atual(this)
        if (contaAtual?.papel != UsuarioLocalRepository.PAPEL_CLIENTE) {
            // Não destruir uma sessão CEO/funcionário ao abrir o Perfil de cliente.
            if (SessaoRcar.interno(this)) {
                startActivity(Intent(this, MainActivityPerfilInterno::class.java))
            } else {
                startActivity(Intent(this, MainActivitycomprasonline::class.java))
            }
            finish()
            return
        }
        setContentView(R.layout.activity_main_home)

        BottomNav.setup(this, "perfil")

        // 1. Mapeamento dos elementos do Cabeçalho e Resumo
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        val txtNome = findViewById<TextView>(R.id.txtNome)
        val txtEmail = findViewById<TextView>(R.id.txtEmail)
        val txtQtdPedidos = findViewById<TextView>(R.id.txtQtdPedidos)
        val txtPontos = findViewById<TextView>(R.id.txtPontos)

        // 2. Mapeamento dos botões de navegação (CardViews)
        val btnDadosCadastrais = findViewById<CardView>(R.id.btnDadosCadastrais)
        val btnEnderecos = findViewById<CardView>(R.id.btnEnderecos)
        val btnMeusPedidos = findViewById<CardView>(R.id.btnMeusPedidos)
        val btnCarrinho = findViewById<CardView>(R.id.btnCarrinho)

        // 3. Mapeamento do Botão Sair
        val btnSair = findViewById<MaterialButton>(R.id.btnSair)
        findViewById<CardView>(R.id.btnConfiguracoes).setOnClickListener { startActivity(Intent(this, MainActivityConfiguracoes::class.java)) }
        findViewById<CardView>(R.id.btnSuporte).setOnClickListener { startActivity(Intent(this, MainActivitySuporte::class.java)) }

        // -------------------------------------------------------------
        // Configuração dos Eventos de Clique (Listeners)
        // -------------------------------------------------------------

        // Abrir ecrã de Dados Cadastrais
        btnDadosCadastrais.setOnClickListener {
            val intent = Intent(this, MainActivityDadosCadastrais::class.java)
            startActivity(intent)
        }

        // Abrir ecrã de Endereços
        btnEnderecos.setOnClickListener {
            startActivity(Intent(this, MainActivityEnderecosEntrega::class.java))
        }

        // Abrir ecrã de Meus Pedidos
        btnMeusPedidos.setOnClickListener {
            startActivity(Intent(this, MainActivityPedidos::class.java))
        }

        // Abrir ecrã do Carrinho
        btnCarrinho.setOnClickListener {
            startActivity(Intent(this, MainActivityItemCarrinho::class.java))
        }

        // Ação do Botão Sair
        btnSair.setOnClickListener { SessaoRcar.confirmarSaida(this) }

        // Atalhos adicionais do perfil: suporte e configurações ficam acessíveis pelo menu inferior/área de conta.
        val email = RcarLocalStore.currentEmail(this)
        txtEmail.text = email ?: "Cliente RCar Premium"
        txtNome.text = email?.let { UsuarioLocalRepository(this).nomeDoUsuario(it) } ?: "Cliente"
        txtQtdPedidos.text = "${RcarLocalStore.count(this)} itens no carrinho"
        txtPontos.text = "Atendimento RCar Premium"
    }
    override fun onResume() {
        super.onResume()
        val conta = SessaoRcar.atual(this) ?: return
        if (conta.papel != UsuarioLocalRepository.PAPEL_CLIENTE) return
        findViewById<TextView>(R.id.txtNome)?.text = conta.nome
        findViewById<TextView>(R.id.txtEmail)?.text = conta.email
        findViewById<TextView>(R.id.txtQtdPedidos)?.text = "${RcarLocalStore.count(this)} itens no carrinho"
    }

}
