package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.Normalizer
import java.util.Locale

/** Assistente: recomendação local funcional e opção de IA remota autenticada no backend. */
class MainActivityAssistenteCompra : RcarBaseActivity() {
    private lateinit var answer: android.widget.TextView
    private lateinit var prompt: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (frame, body) = MarketplaceUi.page(this, "ASSISTENTE RCAR",
            "Encontre peças no catálogo. IA remota disponível depois de conectar um servidor seguro.",
            "⌂  INÍCIO DA LOJA", { finish() }, {
                startActivity(Intent(this, MainActivitycomprasonline::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })
        body.addView(MarketplaceUi.text(this, "Como podemos ajudar?", 17f, Color.WHITE, true), MarketplaceUi.lp(this, 18))
        prompt = EditText(this).apply {
            hint = "Ex.: preciso de filtro de óleo para meu carro"; minLines=2; maxLines=4
            textSize=15f; setTextColor(Color.WHITE); setHintTextColor(MarketplaceUi.MUTED)
            background=MarketplaceUi.shape(this@MainActivityAssistenteCompra)
            setPadding(20, 18, 20, 18)
        }
        body.addView(prompt, MarketplaceUi.lp(this, 12))
        body.addView(MarketplaceUi.button(this, "BUSCAR RECOMENDAÇÕES LOCAIS", true).apply {
            setOnClickListener { recomendar() }
        }, MarketplaceUi.lp(this, 12))
        body.addView(MarketplaceUi.button(this, "CONECTAR À IA DO SERVIDOR").apply { setOnClickListener { autenticarRemoto() } }, MarketplaceUi.lp(this, 8))
        body.addView(MarketplaceUi.button(this, "PERGUNTAR À IA REMOTA").apply {
            setOnClickListener {
                val q=prompt.text.toString().trim()
                if(q.length < 5) { prompt.error="Descreva sua dúvida."; return@setOnClickListener }
                answer.text="Consultando servidor..."
                MarketplaceRemoteApi.chat(this@MainActivityAssistenteCompra, q) { result ->
                    answer.text = result.getOrElse { "Serviço de IA indisponível: ${it.message}" }
                }
            }
        }, MarketplaceUi.lp(this, 8))
        answer = MarketplaceUi.text(this, "Escolha o modo local para ver sugestões imediatamente. Para respostas de IA, o CEO configura o backend HTTPS e você entra com uma conta do servidor.", 14f, MarketplaceUi.MUTED)
        body.addView(MarketplaceUi.card(this).apply { addView(answer) }, MarketplaceUi.lp(this, 18))
        body.addView(MarketplaceUi.text(this, "Atenção: confirme compatibilidade da peça pelo modelo, ano e motorização do veículo antes da compra. Sugestões não garantem compatibilidade.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 14))
        setContentView(frame)
    }
    private fun normalize(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    private fun recomendar() {
        val q = normalize(prompt.text.toString().trim())
        if(q.length < 3) { prompt.error="Descreva a peça ou problema."; return }
        val tokens = q.split(" ", ",", ".", "-", "/").filter { it.length > 2 && it !in listOf("para", "com", "uma", "que", "preciso", "quero", "meu", "minha", "carro") }
        val ranked = MarketplaceStore.produtosDisponiveis(this).map { (product, seller) ->
            val hay = normalize("${product.nome} ${product.categoria} ${product.marca} ${product.descricao}")
            Triple(product, seller, tokens.count { hay.contains(it) })
        }.filter { it.third > 0 }.sortedByDescending { it.third }.take(5)
        answer.text = if (ranked.isEmpty()) {
            "Não encontrei correspondências no catálogo local. Tente informar o tipo da peça ou procure nas categorias."
        } else buildString {
            append("Sugestões por palavras-chave (não são IA):\n\n")
            ranked.forEach { (p, seller, _) -> append("• ${p.nome}\n  ${seller.nome} • ${p.categoria}\n\n") }
            append("Acesse o marketplace para ver preços, estoque e comprar.")
        }
    }
    private fun autenticarRemoto() {
        if(MarketplaceStore.urlBackend(this).isBlank()) {
            Toast.makeText(this, "O CEO precisa configurar o servidor HTTPS em Gerenciar lojas.", Toast.LENGTH_LONG).show(); return
        }
        val form=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(30, 14, 30, 10); background = RcarDialogUi.panel(this@MainActivityAssistenteCompra) }
        val email=EditText(this).apply { hint="E-mail no servidor"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; RcarDialogUi.styleInput(this) }
        val password=EditText(this).apply { hint="Senha do servidor"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; RcarDialogUi.styleInput(this) }
        form.addView(email)
        form.addView(password, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })
        val dialog = AlertDialog.Builder(this).setTitle("Entrar na IA do servidor").setMessage("Conta do backend (não é o login local do aplicativo).")
            .setView(RcarDialogUi.wrapPanel(this, form)).setNegativeButton("Cancelar",null).setPositiveButton("Entrar", null).create()
        dialog.setOnShowListener {
            RcarDialogUi.styleDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                answer.text="Autenticando..."
                dialog.dismiss()
                MarketplaceRemoteApi.login(this, email.text.toString().trim(), password.text.toString()) { result ->
                    answer.text=result.getOrElse { "Falha no login remoto: ${it.message}" }
                }
            }
        }
        dialog.show()
    }
}
