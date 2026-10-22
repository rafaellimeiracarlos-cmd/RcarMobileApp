package com.example.rcarpremium

import android.content.Intent

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** CEO mantém aplicações verificadas por referência técnica, nunca presumidas pela placa. */
class MainActivityCompatibilidadeCeo : RcarBaseActivity() {
    private lateinit var seletor: Spinner
    private lateinit var marca: EditText
    private lateinit var modelo: EditText
    private lateinit var motor: EditText
    private lateinit var anoDe: EditText
    private lateinit var anoAte: EditText
    private lateinit var lista: LinearLayout
    private lateinit var informativo: TextView
    private var produtos: List<AdminProduto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conta = SessaoRcar.atual(this)
        if (conta == null || !UsuarioLocalRepository(this).isCeo(conta.email)) {
            Toast.makeText(this, "Somente o CEO pode alterar compatibilidades.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        produtos = AdminLocalStore.products(this)
        val (frame, body) = MarketplaceUi.page(this, "COMPATIBILIDADE DE PEÇAS",
            "Vincule cada SKU a marca, modelo, motor e faixa de anos confirmados no catálogo técnico.",
            "⌂  PAINEL DO CEO", { finish() }, {
                startActivity(Intent(this, MainActivityPainelInterno::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })
        val card = MarketplaceUi.card(this)
        card.addView(MarketplaceUi.text(this, "PRODUTO DO ESTOQUE", 13f, MarketplaceUi.ORANGE, true))
        seletor = Spinner(this)
        seletor.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            if (produtos.isEmpty()) listOf("Cadastre um produto primeiro") else produtos.map { it.nome + " • " + it.sku })
        card.addView(seletor, MarketplaceUi.lp(this, 7))
        marca = input(card, "Marca", "Ex.: Volkswagen")
        modelo = input(card, "Modelo", "Ex.: Gol")
        motor = input(card, "Motor / versão", "Ex.: 1.0 MPI")
        anoDe = input(card, "Ano inicial", "Ex.: 2019", true)
        anoAte = input(card, "Ano final", "Ex.: 2022", true)
        card.addView(MarketplaceUi.button(this, "SALVAR COMPATIBILIDADE  ✓", true).apply {
            isEnabled = produtos.isNotEmpty()
            setOnClickListener { salvar() }
        }, MarketplaceUi.lp(this, 12))
        body.addView(card, MarketplaceUi.lp(this, 18))
        body.addView(MarketplaceUi.text(this,
            "Use somente aplicações verificadas pelo fabricante da peça, pelo número OEM ou por um catálogo técnico. Não marque uma peça como compatível por semelhança de nome.",
            12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 12))
        body.addView(MarketplaceUi.text(this, "APLICAÇÕES CADASTRADAS", 17f, Color.WHITE, true), MarketplaceUi.lp(this, 22))
        informativo = MarketplaceUi.text(this, "", 12f, MarketplaceUi.MUTED)
        body.addView(informativo, MarketplaceUi.lp(this, 6))
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(lista, MarketplaceUi.lp(this, 8))
        setContentView(frame)
        listar()
    }

    private fun input(card: LinearLayout, label: String, hintText: String, number: Boolean = false): EditText {
        card.addView(MarketplaceUi.text(this, label, 12f, MarketplaceUi.MUTED, true), MarketplaceUi.lp(this, 11))
        return EditText(this).apply {
            hint = hintText; setSingleLine(true); textSize = 15f
            setTextColor(Color.WHITE); setHintTextColor(MarketplaceUi.MUTED)
            if (number) inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(MarketplaceUi.dp(this@MainActivityCompatibilidadeCeo, 15), 0, MarketplaceUi.dp(this@MainActivityCompatibilidadeCeo, 15), 0)
            background = MarketplaceUi.shape(this@MainActivityCompatibilidadeCeo)
            card.addView(this, LinearLayout.LayoutParams(-1, MarketplaceUi.dp(this@MainActivityCompatibilidadeCeo, 48)).apply {
                topMargin = MarketplaceUi.dp(this@MainActivityCompatibilidadeCeo, 4)
            })
        }
    }

    private fun salvar() {
        val product = produtos.getOrNull(seletor.selectedItemPosition) ?: return
        val fitment = VehicleFitment(VehicleFitmentStore.productKey(product), marca.text.toString().trim(), modelo.text.toString().trim(),
            anoDe.text.toString().toIntOrNull() ?: 0, anoAte.text.toString().toIntOrNull() ?: 0, motor.text.toString().trim())
        try {
            VehicleFitmentStore.add(this, fitment)
            Toast.makeText(this, "Aplicação cadastrada para ${product.nome}", Toast.LENGTH_SHORT).show()
            listar()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun listar() {
        lista.removeAllViews()
        val all = VehicleFitmentStore.all(this)
        informativo.text = "${all.size} aplicação(ões) cadastrada(s) neste dispositivo"
        if (all.isEmpty()) {
            lista.addView(MarketplaceUi.text(this, "Nenhuma peça possui aplicação confirmada. Cadastre as primeiras acima.", 13f, MarketplaceUi.MUTED))
        }
        all.forEach { fit ->
            val product = produtos.firstOrNull { VehicleFitmentStore.productKey(it) == fit.productKey }
            val card = MarketplaceUi.card(this)
            card.addView(MarketplaceUi.text(this, product?.nome ?: "Produto removido", 15f, Color.WHITE, true))
            card.addView(MarketplaceUi.text(this, "${fit.brand} ${fit.model} • ${fit.fromYear}–${fit.toYear} • ${fit.engine}", 13f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 7))
            card.addView(MarketplaceUi.button(this, "REMOVER APLICAÇÃO").apply {
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivityCompatibilidadeCeo)
                        .setTitle("Remover compatibilidade?")
                        .setMessage("A peça deixará de aparecer nas buscas desse veículo.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Remover") { _, _ -> VehicleFitmentStore.remove(this@MainActivityCompatibilidadeCeo, fit); listar() }
                        .show()
                }
            }, MarketplaceUi.lp(this, 9))
            lista.addView(card, MarketplaceUi.lp(this, 8))
        }
    }
}
