package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import java.util.Locale

/** Busca Mercosul/antiga. Sem provedor autorizado, requer confirmação manual do veículo. */
class MainActivityBuscaPlaca : RcarBaseActivity() {
    private lateinit var placa: EditText
    private lateinit var marca: EditText
    private lateinit var modelo: EditText
    private lateinit var ano: EditText
    private lateinit var motor: EditText
    private lateinit var remember: CheckBox
    private lateinit var status: TextView
    private lateinit var resultados: LinearLayout
    private var origem = "manual"
    private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (frame, body) = MarketplaceUi.page(this, "BUSCA PELA PLACA",
            "Encontre peças do carro certo, com compatibilidade cadastrada e confirmada.",
            "⌂  INÍCIO DA LOJA", { finish() }, {
                startActivity(Intent(this, MainActivitycomprasonline::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })

        val plateCard = MarketplaceUi.card(this)
        plateCard.addView(MarketplaceUi.text(this, "PLACA MERCOSUL OU ANTIGA", 13f, MarketplaceUi.ORANGE, true))
        plateCard.addView(MarketplaceUi.text(this, "Ex.: ABC1D23 ou ABC1234", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 6))
        placa = entry("Digite a placa", 20f).apply {
            filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(8))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            letterSpacing = 0.12f
        }
        plateCard.addView(placa, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this, 56)).apply { topMargin = MarketplaceUi.dp(this@MainActivityBuscaPlaca, 12) })
        plateCard.addView(MarketplaceUi.button(this, "CONSULTAR PLACA  →", true).apply {
            setOnClickListener { consultarPlaca() }
        }, MarketplaceUi.lp(this, 12))
        plateCard.addView(MarketplaceUi.button(this, "Conectar consulta autorizada", false).apply {
            setOnClickListener { conectarServidor() }
        }, MarketplaceUi.lp(this, 5))
        plateCard.addView(MarketplaceUi.text(this,
            "A placa não revela marca, modelo ou motor por si só. Consulta automática exige um serviço autorizado; nenhum dado de proprietário é solicitado.",
            12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 9))
        body.addView(plateCard, MarketplaceUi.lp(this, 16))

        status = MarketplaceUi.text(this, "Informe a placa e consulte ou confirme os dados do veículo abaixo.", 13f, MarketplaceUi.MUTED)
        body.addView(status, MarketplaceUi.lp(this, 12))

        val vehicleCard = MarketplaceUi.card(this)
        vehicleCard.addView(MarketplaceUi.text(this, "CONFIRMAR O VEÍCULO", 17f, Color.WHITE, true))
        vehicleCard.addView(MarketplaceUi.text(this, "Informe marca, modelo, ano e motorização. Se vierem de um provedor, confira antes de buscar.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 6))
        marca = field(vehicleCard, "Marca", "Ex.: Volkswagen")
        modelo = field(vehicleCard, "Modelo", "Ex.: Gol")
        ano = field(vehicleCard, "Ano do veículo", "Ex.: 2020").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        motor = field(vehicleCard, "Motor / versão", "Ex.: 1.0 MPI")
        remember = CheckBox(this).apply {
            text = "Lembrar este veículo neste aparelho (opcional)"
            setTextColor(Color.WHITE)
            textSize = 12f
            buttonTintList = android.content.res.ColorStateList.valueOf(MarketplaceUi.ORANGE)
        }
        vehicleCard.addView(remember, MarketplaceUi.lp(this, 9))
        vehicleCard.addView(MarketplaceUi.button(this, "BUSCAR PEÇAS COMPATÍVEIS", true).apply {
            setOnClickListener { mostrarPecas() }
        }, MarketplaceUi.lp(this, 6))
        vehicleCard.addView(MarketplaceUi.button(this, "Esquecer placa salva neste aparelho").apply {
            setOnClickListener {
                val p = PlateRules.normalize(placa.text.toString())
                if (!PlateRules.isValid(p)) { placa.error = "Informe uma placa válida"; return@setOnClickListener }
                VehicleFitmentStore.forget(this@MainActivityBuscaPlaca, p)
                Toast.makeText(this@MainActivityBuscaPlaca, "Vínculo local da placa removido.", Toast.LENGTH_SHORT).show()
            }
        }, MarketplaceUi.lp(this, 5))
        body.addView(vehicleCard, MarketplaceUi.lp(this, 14))

        body.addView(MarketplaceUi.text(this, "PEÇAS PARA O SEU CARRO", 18f, Color.WHITE, true), MarketplaceUi.lp(this, 20))
        resultados = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(resultados, MarketplaceUi.lp(this, 7))
        resultados.addView(MarketplaceUi.text(this,
            "Confirme os dados do carro para ver apenas as peças com compatibilidade cadastrada.", 13f, MarketplaceUi.MUTED))
        body.addView(MarketplaceUi.button(this, "VER TODO O CATÁLOGO (SEM FILTRO)").apply {
            setOnClickListener { startActivity(Intent(this@MainActivityBuscaPlaca, MainActivityListaProdutos::class.java)) }
        }, MarketplaceUi.lp(this, 14))
        body.addView(MarketplaceUi.text(this,
            "A compatibilidade é informada pelo catálogo RCarPremium, não validada por órgão de trânsito. Confira chassi, ano/modelo e código da peça antes de comprar.",
            12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 10))
        setContentView(frame)
    }

    private fun entry(hintText: String, size: Float = 15f) = EditText(this).apply {
        hint = hintText; textSize = size; setTextColor(Color.WHITE)
        setHintTextColor(MarketplaceUi.MUTED); setSingleLine(true)
        setPadding(MarketplaceUi.dp(this@MainActivityBuscaPlaca, 15), 0, MarketplaceUi.dp(this@MainActivityBuscaPlaca, 15), 0)
        background = MarketplaceUi.shape(this@MainActivityBuscaPlaca)
    }
    private fun field(card: LinearLayout, caption: String, placeholder: String): EditText {
        card.addView(MarketplaceUi.text(this, caption, 12f, MarketplaceUi.MUTED, true), MarketplaceUi.lp(this, 11))
        return entry(placeholder).also { card.addView(it, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this, 49)).apply { topMargin = MarketplaceUi.dp(this@MainActivityBuscaPlaca, 5) }) }
    }

    private fun consultarPlaca() {
        val p = PlateRules.normalize(placa.text.toString())
        if (!PlateRules.isValid(p)) {
            placa.error = "Formato inválido. Use ABC1D23 (Mercosul) ou ABC1234."
            return
        }
        placa.setText(p)
        VehicleFitmentStore.remembered(this, p)?.let { vehicle ->
            preencher(vehicle)
            origem = "cadastro local informado pelo usuário"
            status.text = "✓ Veículo salvo encontrado (${PlateRules.patternName(p)}). Dados declarados, não verificados oficialmente."
            mostrarPecas(); return
        }
        status.text = "Consultando serviço autorizado, se configurado..."
        MarketplaceRemoteApi.vehicleByPlate(this, p) { response ->
            response.onSuccess { vehicle ->
                preencher(vehicle)
                origem = "provedor autorizado (confirme os dados)"
                status.text = "✓ Veículo retornado pelo serviço conectado. Confira a versão e o motor antes de continuar."
                mostrarPecas()
            }.onFailure { error ->
                origem = "manual"
                status.text = "Consulta automática indisponível: ${error.message?.take(150) ?: "sem retorno"}. Informe o veículo manualmente para continuar."
            }
        }
    }

    private fun conectarServidor() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MarketplaceUi.dp(this@MainActivityBuscaPlaca, 18), 0, MarketplaceUi.dp(this@MainActivityBuscaPlaca, 18), 0)
        }
        val email = entry("E-mail da conta no servidor")
        email.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val password = entry("Senha do servidor")
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        content.addView(email, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this, 50)))
        content.addView(password, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this, 50)).apply { topMargin = MarketplaceUi.dp(this@MainActivityBuscaPlaca, 10) })
        AlertDialog.Builder(this).setTitle("Conectar consulta de veículo")
            .setMessage("Exige servidor HTTPS RCarPremium e provedor de placas autorizado. Esta conta é do backend, distinta do login local.")
            .setView(content).setNegativeButton("Cancelar", null)
            .setPositiveButton("Conectar") { _, _ ->
                MarketplaceRemoteApi.login(this, email.text.toString().trim(), password.text.toString()) { answer ->
                    status.text = answer.fold({ "Servidor conectado. Agora informe a placa e consulte." },
                        { "Não foi possível conectar: ${it.message}" })
                }
            }.show()
    }

    private fun preencher(vehicle: VehicleSpec) {
        marca.setText(vehicle.brand); modelo.setText(vehicle.model)
        ano.setText(vehicle.year.toString()); motor.setText(vehicle.engine)
    }

    private fun mostrarPecas() {
        val vehicle = VehicleSpec(marca.text.toString().trim(), modelo.text.toString().trim(),
            ano.text.toString().trim().toIntOrNull() ?: 0, motor.text.toString().trim())
        if (!vehicle.isValid()) {
            Toast.makeText(this, "Informe marca, modelo, ano e motor válidos.", Toast.LENGTH_LONG).show()
            return
        }
        val p = PlateRules.normalize(placa.text.toString())
        if (remember.isChecked && PlateRules.isValid(p)) {
            try { VehicleFitmentStore.remember(this, p, vehicle) }
            catch (e: IllegalStateException) { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
        }
        resultados.removeAllViews()
        val products = VehicleFitmentStore.compatible(this, vehicle)
        status.text = "Veículo selecionado: ${vehicle.description()}  •  Dados: $origem"
        if (products.isEmpty()) {
            resultados.addView(MarketplaceUi.card(this).apply {
                addView(MarketplaceUi.text(this@MainActivityBuscaPlaca, "Nenhuma peça com compatibilidade confirmada no catálogo local.", 15f, Color.WHITE, true))
                addView(MarketplaceUi.text(this@MainActivityBuscaPlaca, "Peça ao CEO para cadastrar a aplicação desse modelo ou explore o catálogo sem filtro, conferindo a compatibilidade antes da compra.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this@MainActivityBuscaPlaca, 7))
            })
            return
        }
        resultados.addView(MarketplaceUi.text(this, "${products.size} produto(s) com aplicação cadastrada para ${vehicle.description()}", 13f, MarketplaceUi.ORANGE, true))
        products.forEach { product ->
            val card = MarketplaceUi.card(this)
            card.addView(MarketplaceUi.text(this, product.nome, 17f, Color.WHITE, true))
            card.addView(MarketplaceUi.text(this, product.categoria + " • " + product.marca, 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 6))
            card.addView(MarketplaceUi.text(this, moeda.format(AdminLocalStore.promotionalPrice(this, product)), 19f, MarketplaceUi.ORANGE, true), MarketplaceUi.lp(this, 8))
            card.addView(MarketplaceUi.button(this, "VER DETALHES DA PEÇA  →", true).apply {
                setOnClickListener {
                    startActivity(Intent(this@MainActivityBuscaPlaca, MainActivityVerProdutos::class.java).apply {
                        putExtra("PRODUTO_NOME", product.nome)
                        putExtra("PRODUTO_PRECO", AdminLocalStore.promotionalPrice(this@MainActivityBuscaPlaca, product))
                    })
                }
            }, MarketplaceUi.lp(this, 10))
            resultados.addView(card, MarketplaceUi.lp(this, 8))
        }
    }
}
