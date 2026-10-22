package com.example.rcarpremium

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivityItemCarrinho : RcarBaseActivity() {

    private lateinit var txtValorTotal: TextView
    private lateinit var listaProdutos: MutableList<ProdutoItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_item_carrinho)

        // 1. MAPEIA OS ELEMENTOS DA INTERFACE
        val imgVoltarOuLogo = findViewById<ImageView>(R.id.BottomVolt)
        txtValorTotal = findViewById(R.id.txtValorTotal)
        if (CompraTesteCeoStore.ativo(this)) {
            findViewById<android.widget.LinearLayout>(R.id.headerCarrinho).addView(TextView(this).apply {
                text = "  🧪 TESTE CEO"
                setTextColor(android.graphics.Color.rgb(255, 122, 0))
                textSize = 12f
            })
            findViewById<Button>(R.id.btnFinalizarCompra).text = "Simular checkout"
        }
        val btnFinalizarCompra = findViewById<Button>(R.id.btnFinalizarCompra)
        val recyclerCarrinho = findViewById<RecyclerView>(R.id.recyclerCarrinho)
        val btnContinuarComprando = findViewById<Button>(R.id.btnContinuarComprando)

        // 2. AÇÃO DO BOTÃO CONTINUAR COMPRANDO
        btnContinuarComprando?.setOnClickListener {
            finish()
        }

        // 3. DADOS DE EXEMPLO
        listaProdutos = RcarLocalStore.cart(this)

        // 4. CONFIGURA O RECYCLERVIEW
        recyclerCarrinho.layoutManager = LinearLayoutManager(this)

        val adapter = CarrinhoAdapter(listaProdutos) {
            atualizarValorTotal()
        }
        recyclerCarrinho.adapter = adapter

        // 5. CALCULA O VALOR INICIAL
        atualizarValorTotal()

        // 6. AÇÃO DE VOLTAR AO CLICAR NA LOGO
        imgVoltarOuLogo?.setOnClickListener {
            val intent = Intent(this, MainActivitycomprasonline::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // 7. AÇÃO DO BOTÃO FINALIZAR COMPRA
        btnFinalizarCompra?.setOnClickListener {
            if (listaProdutos.isEmpty()) {
                android.widget.Toast.makeText(this, "Seu carrinho está vazio.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val conta = SessaoRcar.atual(this)
            if (!CompraTesteCeoStore.ativo(this) && conta != null && conta.papel != UsuarioLocalRepository.PAPEL_CLIENTE) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Compra para clientes")
                    .setMessage("Você está conectado como ${conta.papel}. O checkout comercial exige conta de cliente. Para simular compras, o CEO pode ativar o modo de teste no painel, sem sair desta conta.")
                    .setPositiveButton("Entendi", null).show()
                return@setOnClickListener
            }
            if (!CompraTesteCeoStore.ativo(this) && conta == null) {
                androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Entre para finalizar")
                    .setMessage("Faça login para informar endereço de entrega e pagamento.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Fazer login") { _, _ -> startActivity(Intent(this, MainActivity::class.java).putExtra("IR_CHECKOUT", true)) }
                    .show()
                return@setOnClickListener
            }
            val valorTotal = listaProdutos.sumOf { it.preco * it.quantidade }
            val quantidadeTotal = listaProdutos.sumOf { it.quantidade }
            if (!CompraTesteCeoStore.ativo(this)) AnalyticsLocalStore.markCheckoutStarted(this, listaProdutos)

            val intent = Intent(this, MainActivityPagamento::class.java).apply {
                putExtra("TOTAL_PEDIDO", "R$ %.2f".format(valorTotal))
                putExtra("QTD_PRODUTOS", quantidadeTotal)
            }
            startActivity(intent)
        }
    }

    // Função para recalcular o valor total do carrinho
    private fun atualizarValorTotal() {
        RcarLocalStore.saveCart(this, listaProdutos)
        val valorTotal = listaProdutos.sumOf { it.preco * it.quantidade }
        txtValorTotal.text = "R$ %.2f".format(valorTotal)
    }
}
