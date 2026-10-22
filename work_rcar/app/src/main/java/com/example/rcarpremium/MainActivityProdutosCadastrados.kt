package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivityProdutosCadastrados : RcarBaseActivity() {
    private val orange = Color.rgb(255, 122, 0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = RcarLocalStore.currentEmail(this)
        val repo = UsuarioLocalRepository(this)
        if (email.isNullOrBlank() || !repo.isCeo(email)) { Toast.makeText(this, "Apenas o CEO pode editar o catálogo.", Toast.LENGTH_LONG).show(); finish(); return }
        render()
    }
    override fun onResume() { super.onResume(); if (RcarLocalStore.currentEmail(this)?.let { UsuarioLocalRepository(this).isCeo(it) } == true) render() }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(24)); setBackgroundColor(Color.rgb(10,10,10)) }
        val scroll = ScrollView(this); val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val back = button("←  VOLTAR", false).apply { setOnClickListener { finish() } }; content.addView(back, lp())
        content.addView(text("PRODUTOS CADASTRADOS", 23f, orange, true), lp(top=14))
        content.addView(text("Toque em editar para alterar preço, estoque, categoria ou descrição.", 13f, Color.LTGRAY), lp(top=5))
        val products = AdminLocalStore.products(this).filter { it.ativo }
        content.addView(text("${products.size} produtos ativos", 14f, Color.WHITE, true), lp(top=14))
        products.forEach { p ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14),dp(13),dp(14),dp(13)); background = bg(0xFF1B1B1B.toInt(), orange, 14) }
            card.addView(text(p.nome, 17f, Color.WHITE, true))
            card.addView(text("${p.categoria}  •  ${String.format(Locale("pt","BR"), "R$ %.2f", p.preco)}  •  Estoque: ${p.estoque}", 13f, Color.LTGRAY), lp(top=5))
            card.addView(text(if (p.anuncioAtivo) "ANÚNCIO ATIVO" else "ANÚNCIO PAUSADO", 11f, if (p.anuncioAtivo) 0xFF65D98A.toInt() else orange, true), lp(top=5))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val edit = button("✎  EDITAR", true).apply { setOnClickListener { startActivity(Intent(this@MainActivityProdutosCadastrados, MainActivityCadastroProduto::class.java).putExtra("EDITAR_PRODUTO_NOME", p.nome)) } }
            val offer = button("%  OFERTA", false).apply { setOnClickListener { startActivity(Intent(this@MainActivityProdutosCadastrados, MainActivityOfertas::class.java)) } }
            row.addView(edit, LinearLayout.LayoutParams(0, dp(44), 1f)); row.addView(offer, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin=dp(8) }); card.addView(row, lp(top=10))
            card.addView(button(if (p.anuncioAtivo) "Ⅱ  PAUSAR ANÚNCIO" else "▶  REATIVAR ANÚNCIO", false).apply {
                setOnClickListener {
                    AdminLocalStore.setAnuncioAtivo(this@MainActivityProdutosCadastrados, p.nome, !p.anuncioAtivo)
                    Toast.makeText(this@MainActivityProdutosCadastrados, if (p.anuncioAtivo) "Anúncio pausado." else "Anúncio reativado.", Toast.LENGTH_SHORT).show()
                    render()
                }
            }, lp(top=8))
            val users = UsuarioLocalRepository(this).listarUsuarios().filter { it.papel in UsuarioLocalRepository.PAPEIS_EQUIPE && it.ativo }
            card.addView(button(if (p.cadastradoPor.isBlank()) "♙  ATRIBUIR RESPONSÁVEL" else "♙  ALTERAR RESPONSÁVEL", false).apply {
                setOnClickListener {
                    if (users.isEmpty()) {
                        Toast.makeText(this@MainActivityProdutosCadastrados, "Cadastre/ative um funcionário em Logins e permissões primeiro.", Toast.LENGTH_LONG).show()
                    } else {
                        val labels = users.map { "${it.nome} (${it.email})" }.toTypedArray()
                        AlertDialog.Builder(this@MainActivityProdutosCadastrados)
                            .setTitle("Responsável por ${p.nome}")
                            .setItems(labels) { _, which ->
                                AdminLocalStore.definirResponsavel(this@MainActivityProdutosCadastrados, p.nome, users[which].email)
                                Toast.makeText(this@MainActivityProdutosCadastrados, "Responsável atualizado.", Toast.LENGTH_SHORT).show()
                                render()
                            }
                            .setNegativeButton("Cancelar", null).show()
                    }
                }
            }, lp(top=8))
            content.addView(card, lp(top=10))
        }
        content.addView(button("▣  IMPORTAR PRODUTOS POR NF-e / XML", false).apply {
            setOnClickListener { startActivity(Intent(this@MainActivityProdutosCadastrados, MainActivityImportarNota::class.java)) }
        }, lp(top=14))
        content.addView(button("＋  CADASTRAR NOVO PRODUTO", true).apply { setOnClickListener { startActivity(Intent(this@MainActivityProdutosCadastrados, MainActivityCadastroProduto::class.java)) } }, lp(top=14))
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }
    private fun text(s:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply { text=s; textSize=size; setTextColor(color); if(bold) typeface=Typeface.DEFAULT_BOLD }
    private fun button(s:String, primary:Boolean)=Button(this).apply { text=s; textSize=13f; setTextColor(Color.WHITE); background=bg(if(primary) orange else 0xFF242424.toInt(), if(primary) orange else 0xFF444444.toInt(), 22) }
    private fun bg(color:Int,stroke:Int,r:Int)=GradientDrawable().apply { setColor(color); cornerRadius=dp(r).toFloat(); setStroke(dp(1),stroke) }
    private fun lp(top:Int=0)=LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if(top>0) topMargin=dp(top) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
