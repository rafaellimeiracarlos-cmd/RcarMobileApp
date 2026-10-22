package com.example.rcarpremium

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class MainActivityPagamento : RcarBaseActivity() {
    private var enderecoView: TextView? = null
    private var freteView: TextView? = null
    private var enderecoButton: Button? = null

    override fun onResume() {
        super.onResume()
        if (enderecoView != null && freteView != null) atualizarResumoEntrega(enderecoView!!, freteView!!)
    }

    private fun atualizarResumoEntrega(enderecoView: TextView, freteView: TextView) {
        if (CompraTesteCeoStore.ativo(this)) {
            val salvo = CompraTesteCeoStore.endereco(this)
            enderecoView.text = salvo.ifBlank {
                "Nenhum endereço de teste cadastrado. Toque em Adicionar endereço para preencher a etapa de entrega da simulação."
            }
            enderecoButton?.text = if (salvo.isBlank()) "Adicionar endereço de teste" else "Editar endereço de teste"
            val frete = getSharedPreferences("rcarpremium_config", MODE_PRIVATE).getFloat("frete_padrao", 0f)
            freteView.text = String.format(java.util.Locale("pt", "BR"), "R$ %.2f (simulado)", frete)
            freteView.setTextColor(android.graphics.Color.rgb(255, 170, 58))
            this.enderecoView = enderecoView
            this.freteView = freteView
            return
        }
        val prefs = getSharedPreferences("rcarpremium_enderecos", MODE_PRIVATE)
        val raw = prefs.getString("lista", null)
        var enderecoTexto = "Nenhum endereço selecionado. Cadastre um endereço para entrega."
        if (!raw.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    if (item.optBoolean("padrao", false)) {
                        enderecoTexto = "${item.optString("rua")}, ${item.optString("numero")} - ${item.optString("bairro")}\n${item.optString("cidade")} - ${item.optString("estado")} | CEP ${item.optString("cep")}" 
                        break
                    }
                }
            } catch (_: Exception) { }
        }
        enderecoView.text = enderecoTexto
        enderecoButton?.text = if (enderecoTexto.startsWith("Nenhum endereço")) "Adicionar endereço de entrega" else "Selecionar / editar endereço"
        val config = getSharedPreferences("rcarpremium_config", MODE_PRIVATE)
        if (config.contains("frete_padrao")) {
            freteView.text = String.format(java.util.Locale("pt", "BR"), "R$ %.2f", config.getFloat("frete_padrao", 0f))
            freteView.setTextColor(android.graphics.Color.WHITE)
        } else {
            freteView.text = "Não configurado"
            freteView.setTextColor(android.graphics.Color.YELLOW)
        }
        this.enderecoView = enderecoView
        this.freteView = freteView
    }

    private fun temEnderecoPadrao(): Boolean {
        if (CompraTesteCeoStore.ativo(this)) return CompraTesteCeoStore.endereco(this).isNotBlank()
        val raw = getSharedPreferences("rcarpremium_enderecos", MODE_PRIVATE).getString("lista", null) ?: return false
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).any { arr.getJSONObject(it).optBoolean("padrao", false) }
        } catch (_: Exception) { false }
    }

    private fun mostrarQrPix() {
        if (CompraTesteCeoStore.ativo(this)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🧪 Pix simulado")
                .setMessage("Nesta compra de teste não é gerado QR Code bancário e nenhum Pix é enviado. Escolha CONFIRMAR SIMULAÇÃO para registrar apenas um pedido de teste.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        if (!getSharedPreferences("rcarpremium_config", MODE_PRIVATE).contains("frete_padrao")) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Frete ainda não configurado")
                .setMessage("O CEO precisa configurar o frete padrão em Painel do CEO → Configurar chave Pix da loja.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        if (!temEnderecoPadrao()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Endereço de entrega obrigatório")
                .setMessage("Cadastre e selecione um endereço antes de gerar o Pix do pedido.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cadastrar endereço") { _, _ -> startActivity(android.content.Intent(this, MainActivityEnderecosEntrega::class.java)) }
                .show()
            return
        }
        val prefs = getSharedPreferences("rcarpremium_config", MODE_PRIVATE)
        val key = prefs.getString("pix_key", null)?.trim().orEmpty()
        val name = prefs.getString("pix_name", "RCarPremium") ?: "RCarPremium"
        val city = prefs.getString("pix_city", "SAO PAULO") ?: "SAO PAULO"
        if (key.isBlank()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Configurar Pix da loja")
                .setMessage("O CEO precisa cadastrar a chave Pix em Painel do CEO → Configurar chave Pix da loja antes de gerar o QR Code.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        try {
            val subtotal = RcarLocalStore.cart(this).sumOf { it.preco * it.quantidade }
            val amount = subtotal + getSharedPreferences("rcarpremium_config", MODE_PRIVATE).getFloat("frete_padrao", 0f).toDouble()
            val payload = PixQrGenerator.payload(key, amount, name, city)
            val qr = android.widget.ImageView(this).apply {
                setImageBitmap(PixQrGenerator.bitmap(payload, 660))
                adjustViewBounds = true
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(android.graphics.Color.WHITE)
            }
            val box = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(dp(18), dp(12), dp(18), dp(8))
            }
            box.addView(android.widget.TextView(this).apply {
                text = "Pague ${String.format(java.util.Locale("pt", "BR"), "R$ %.2f", amount)} pelo app do seu banco."
                textSize = 16f; setTextColor(android.graphics.Color.WHITE); gravity = android.view.Gravity.CENTER
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            box.addView(qr, android.widget.LinearLayout.LayoutParams(dp(230), dp(230)).apply { topMargin = dp(12); bottomMargin = dp(12) })
            box.addView(android.widget.TextView(this).apply {
                text = "Pix copia e cola"; textSize = 13f; setTextColor(android.graphics.Color.rgb(255,122,0))
            })
            val code = android.widget.TextView(this).apply {
                text = payload; textSize = 10f; setTextColor(android.graphics.Color.LTGRAY); maxLines = 4; ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(5), 0, dp(5))
            }
            box.addView(code, android.widget.LinearLayout.LayoutParams(-1, -2))
            androidx.appcompat.app.AlertDialog.Builder(this).setTitle("QR Code Pix — RCarPremium").setView(box)
                .setNeutralButton("Copiar código", null).setPositiveButton("Fechar", null).create().also { dialog ->
                    dialog.setOnShowListener {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Pix RCarPremium", payload))
                            Toast.makeText(this, "Código Pix copiado.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível gerar o QR Code Pix. Confira a chave Pix e tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    /** Dados de entrega fictícios; não grava endereços reais de clientes. */
    private fun cadastrarEnderecoSimulado() {
        if (!CompraTesteCeoStore.ativo(this)) return
        val view = layoutInflater.inflate(R.layout.dialog_endereco_checkout_teste, null)
        val nome = view.findViewById<android.widget.EditText>(R.id.editNomeRecebedorTeste)
        val cep = view.findViewById<android.widget.EditText>(R.id.editCepTeste)
        val rua = view.findViewById<android.widget.EditText>(R.id.editRuaTeste)
        val numero = view.findViewById<android.widget.EditText>(R.id.editNumeroTeste)
        val bairro = view.findViewById<android.widget.EditText>(R.id.editBairroTeste)
        val cidade = view.findViewById<android.widget.EditText>(R.id.editCidadeTeste)
        val uf = view.findViewById<android.widget.EditText>(R.id.editUfTeste)
        val complemento = view.findViewById<android.widget.EditText>(R.id.editComplementoTeste)

        val salvo = CompraTesteCeoStore.endereco(this)
        if (salvo.isNotBlank()) {
            val linhas = salvo.replace(" (TESTE)", "").split("\n")
            if (linhas.isNotEmpty()) nome.setText(linhas[0].substringBefore(" •").trim())
            if (linhas.size > 1) {
                val l1 = linhas[1]
                rua.setText(l1.substringBefore(",").trim())
                numero.setText(l1.substringAfter(",", "").substringBefore(" - ").trim())
                bairro.setText(l1.substringAfter(" - ", "").trim())
            }
            if (linhas.size > 2) {
                val l2 = linhas[2]
                cidade.setText(l2.substringBefore(" - ").trim())
                uf.setText(l2.substringAfter(" - ", "").substringBefore(" | ").trim())
                cep.setText(l2.substringAfter("CEP ", "").filter { it.isDigit() })
            }
            if (linhas.size > 3) complemento.setText(linhas[3].removePrefix("Complemento: ").trim())
        }

        cep.filters = arrayOf(android.text.InputFilter.LengthFilter(8))
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar endereço", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.LTGRAY)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.rgb(255, 122, 0))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nomeRecebedor = nome.text.toString().trim().ifBlank { "Cliente de teste" }
                val codigo = cep.text.toString().trim()
                val logradouro = rua.text.toString().trim()
                val n = numero.text.toString().trim()
                val bairroTxt = bairro.text.toString().trim()
                val cidadeTxt = cidade.text.toString().trim()
                val ufTxt = uf.text.toString().trim().uppercase(java.util.Locale.getDefault())
                val compl = complemento.text.toString().trim()
                when {
                    codigo.length != 8 || !codigo.all { it.isDigit() } -> cep.error = "Informe 8 dígitos"
                    logradouro.isBlank() -> rua.error = "Informe a rua"
                    n.isBlank() -> numero.error = "Informe o número"
                    bairroTxt.isBlank() -> bairro.error = "Informe o bairro"
                    cidadeTxt.isBlank() -> cidade.error = "Informe a cidade"
                    ufTxt.length != 2 -> uf.error = "Use 2 letras"
                    else -> {
                        val endereco = buildString {
                            append(nomeRecebedor)
                            append(" • Compra de teste\n")
                            append("$logradouro, $n - $bairroTxt\n")
                            append("$cidadeTxt - $ufTxt | CEP $codigo")
                            if (compl.isNotBlank()) append("\nComplemento: $compl")
                            append(" (TESTE)")
                        }
                        CompraTesteCeoStore.salvarEndereco(this@MainActivityPagamento, endereco)
                        enderecoView?.let { enderecoTxt -> freteView?.let { freteTxt -> atualizarResumoEntrega(enderecoTxt, freteTxt) } }
                        Toast.makeText(this@MainActivityPagamento, "Endereço de teste salvo com sucesso.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_pagamento)

        // 1. Mapeamento dos Componentes
        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        val rgFormasPagamento = findViewById<RadioGroup>(R.id.rgFormasPagamento)
        val rbCartao = findViewById<RadioButton>(R.id.rbCartao)
        val layoutDadosCartao = findViewById<LinearLayout>(R.id.layoutDadosCartao)
        val btnConfirmarPagamento = findViewById<Button>(R.id.btnConfirmarPagamento)

        val txtTotalPagar = findViewById<TextView>(R.id.txtTotalPagar)
        val txtPrecoProdutos = findViewById<TextView>(R.id.txtPrecoProdutos)
        val txtProdutosQtd = findViewById<TextView>(R.id.txtProdutosQtd)
        val txtEndereco = findViewById<TextView>(R.id.txtEnderecoPagamento)
        val txtFrete = findViewById<TextView>(R.id.txtFretePagamento)
        enderecoButton = findViewById(R.id.btnSelecionarEnderecoPagamento)
        enderecoButton?.setOnClickListener {
            if (CompraTesteCeoStore.ativo(this)) cadastrarEnderecoSimulado()
            else startActivity(android.content.Intent(this, MainActivityEnderecosEntrega::class.java))
        }
        if (CompraTesteCeoStore.ativo(this)) {
            enderecoButton?.text = "Adicionar endereço de teste"
            btnConfirmarPagamento.text = "CONFIRMAR SIMULAÇÃO — SEM COBRANÇA"
            rbCartao.text = "Cartão (simulação sem dados bancários)"
            findViewById<RadioButton>(R.id.rbPix).text = "Pix de teste (sem gerar QR Code real)"
            findViewById<RadioButton>(R.id.rbBoleto).text = "Boleto de teste (não emitido)"
            val titulo = findViewById<LinearLayout>(R.id.headerPagamento)
            titulo.addView(TextView(this).apply {
                text = "🧪 TESTE CEO"
                setTextColor(android.graphics.Color.rgb(255, 145, 0))
                textSize = 11f
                setPadding(dp(8), 0, 0, 0)
            })
        }
        atualizarResumoEntrega(txtEndereco, txtFrete)

        // 2. Recebe os dados enviados via Intent
        val valorDouble = intent.getDoubleExtra("TOTAL_PEDIDO", -1.0)
        val valorString = intent.getStringExtra("TOTAL_PEDIDO")
        val qtdProdutos = intent.getIntExtra("QTD_PRODUTOS", 0)

        // Trata a formatação do valor total usando a extensão nativa de String do Kotlin
        val subtotal = RcarLocalStore.cart(this).sumOf { it.preco * it.quantidade }.takeIf { it > 0.0 }
            ?: if (valorDouble > 0.0) valorDouble else 0.0
        val configFrete = getSharedPreferences("rcarpremium_config", MODE_PRIVATE)
        val freteConfigurado = configFrete.contains("frete_padrao")
        val frete = configFrete.getFloat("frete_padrao", 0f).toDouble()
        val moeda = java.util.Locale("pt", "BR")
        val valorProdutos = String.format(moeda, "R$ %.2f", subtotal)
        val totalComFrete = subtotal + frete
        val valorFormatado = String.format(moeda, "R$ %.2f", totalComFrete)

        android.util.Log.d("PAGAMENTO_DEBUG", "Subtotal: $valorProdutos | Frete: $frete | Qtd: $qtdProdutos")

        txtTotalPagar?.text = valorFormatado
        txtPrecoProdutos?.text = valorProdutos
        txtProdutosQtd?.text = "Produtos ($qtdProdutos)"
        txtFrete?.text = if (CompraTesteCeoStore.ativo(this)) String.format(moeda, "R$ %.2f (simulado)", frete)
            else if (freteConfigurado) String.format(moeda, "R$ %.2f", frete) else "Não configurado"
        txtFrete?.setTextColor(if (CompraTesteCeoStore.ativo(this)) android.graphics.Color.rgb(255, 145, 0)
            else if (freteConfigurado) android.graphics.Color.WHITE else android.graphics.Color.YELLOW)

        // 3. Ação do Botão Voltar
        btnVoltar?.setOnClickListener {
            finish()
        }

        // 4. Exibir/Ocultar formulário de Cartão
        rgFormasPagamento?.setOnCheckedChangeListener { _, checkedId ->
            if (CompraTesteCeoStore.ativo(this)) {
                layoutDadosCartao?.visibility = View.GONE
                if (checkedId == R.id.rbPix) mostrarQrPix()
            } else if (checkedId == R.id.rbCartao) {
                layoutDadosCartao?.visibility = View.VISIBLE
            } else {
                layoutDadosCartao?.visibility = View.GONE
                if (checkedId == R.id.rbPix) mostrarQrPix()
            }
        }

        // 5. Confirmar Pagamento
        btnConfirmarPagamento?.setOnClickListener {
            val opcaoSelecionada = when (rgFormasPagamento?.checkedRadioButtonId) {
                R.id.rbPix -> "Pix"
                R.id.rbCartao -> "Cartão de Crédito"
                R.id.rbBoleto -> "Boleto"
                else -> "Desconhecida"
            }

            if (CompraTesteCeoStore.ativo(this)) {
                if (opcaoSelecionada == "Desconhecida") {
                    Toast.makeText(this, "Escolha uma forma de pagamento de teste.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!temEnderecoPadrao()) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Endereço fictício obrigatório")
                        .setMessage("Informe um endereço de teste para verificar a etapa de entrega.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Preencher") { _, _ -> cadastrarEnderecoSimulado() }
                        .show()
                    return@setOnClickListener
                }
                if (CompraTesteCeoStore.carrinho(this).isEmpty()) {
                    Toast.makeText(this, "Adicione um produto para simular a compra.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val freteTeste = getSharedPreferences("rcarpremium_config", MODE_PRIVATE).getFloat("frete_padrao", 0f).toDouble()
                val pedido = CompraTesteCeoStore.concluir(this, opcaoSelecionada, freteTeste)
                if (pedido == null) {
                    Toast.makeText(this, "Não foi possível concluir a simulação.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🧪 Compra simulada concluída")
                    .setMessage("Pedido ${pedido.id} registrado SOMENTE no histórico de testes.\n\nForma: ${pedido.pagamento} (simulada)\nValor: ${String.format(java.util.Locale("pt", "BR"), "R$ %.2f", pedido.total)}\n\nNenhuma cobrança, Pix, baixa de estoque ou venda real foi realizada.")
                    .setCancelable(false)
                    .setNegativeButton("Ver testes") { _, _ ->
                        startActivity(android.content.Intent(this, MainActivityHistoricoTesteCeo::class.java))
                        finish()
                    }
                    .setPositiveButton("Voltar ao painel") { _, _ ->
                        CompraTesteCeoStore.encerrar(this)
                        startActivity(android.content.Intent(this, MainActivityPainelInterno::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }.show()
                return@setOnClickListener
            }
            if (opcaoSelecionada == "Desconhecida") {
                Toast.makeText(this, "Selecione uma forma de pagamento.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!getSharedPreferences("rcarpremium_config", MODE_PRIVATE).contains("frete_padrao")) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Frete ainda não configurado")
                    .setMessage("O CEO precisa configurar o valor padrão do frete no Painel do CEO → Configurar chave Pix da loja. O total ainda não pode ser finalizado.")
                    .setPositiveButton("Entendi", null).show()
                return@setOnClickListener
            }
            if (!temEnderecoPadrao()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Endereço de entrega obrigatório")
                    .setMessage("Cadastre e selecione um endereço antes de finalizar o pedido.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Cadastrar endereço") { _, _ -> startActivity(android.content.Intent(this, MainActivityEnderecosEntrega::class.java)) }
                    .show()
                return@setOnClickListener
            }
            if (opcaoSelecionada == "Pix") {
                mostrarQrPix()
                return@setOnClickListener
            }
            if (opcaoSelecionada == "Cartão de Crédito") {
                val numero = findViewById<android.widget.EditText>(R.id.editNumeroCartao).text.toString().filter { it.isDigit() }
                val validade = findViewById<android.widget.EditText>(R.id.editValidade).text.toString().trim()
                val cvc = findViewById<android.widget.EditText>(R.id.editCVC).text.toString().trim()
                val titular = findViewById<android.widget.EditText>(R.id.editNomeTitular).text.toString().trim()
                if (numero.length < 12 || validade.isEmpty() || cvc.length < 3 || titular.isEmpty()) {
                    Toast.makeText(this, "Confira os dados do cartão.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val emailComprador = RcarLocalStore.currentEmail(this)
            if (emailComprador.isNullOrBlank()) {
                Toast.makeText(this, "Entre com uma conta de cliente antes de finalizar a compra.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!SessaoRcar.cliente(this)) {
                Toast.makeText(this, "A finalização da compra exige uma conta de cliente ativa.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val itensPedido = RcarLocalStore.cart(this)
            val subtotalPedido = itensPedido.sumOf { it.preco * it.quantidade }
            val totalPedido = subtotalPedido + getSharedPreferences("rcarpremium_config", MODE_PRIVATE).getFloat("frete_padrao", 0f).toDouble()
            btnConfirmarPagamento?.isEnabled = false
            val erroVenda = AdminLocalStore.finalizarVenda(this, itensPedido)
            if (erroVenda != null) {
                btnConfirmarPagamento?.isEnabled = true
                Toast.makeText(this, erroVenda, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Cria o pedido persistente para aparecer em Meus Pedidos e no painel interno.
            PedidoLocalStore.registrarCompra(
                context = this,
                email = emailComprador,
                itens = itensPedido,
                total = totalPedido,
                formaPagamento = opcaoSelecionada
            )
            RcarLocalStore.clearCart(this)
            Toast.makeText(this, "Pedido registrado em modo de demonstração via $opcaoSelecionada.", Toast.LENGTH_LONG).show()
            btnConfirmarPagamento?.postDelayed({ finish() }, 1200)
        }
    }
}
