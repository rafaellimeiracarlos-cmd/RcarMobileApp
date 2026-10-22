package com.example.rcarpremium

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivityOfertas : RcarBaseActivity() {
    private val orange=Color.rgb(255,122,0)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val email=RcarLocalStore.currentEmail(this); if(email.isNullOrBlank() || !UsuarioLocalRepository(this).isCeo(email)){Toast.makeText(this,"Apenas o CEO pode gerenciar ofertas.",Toast.LENGTH_LONG).show();finish();return}; render() }
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(24));setBackgroundColor(Color.rgb(10,10,10))}; val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        list.addView(btn("←  VOLTAR",false).apply{setOnClickListener{finish()}})
        list.addView(txt("PRODUTOS EM OFERTA",23f,orange,true),lp(14));list.addView(txt("Defina descontos de 0% a 90%. O preço promocional será exibido no catálogo.",13f,Color.LTGRAY),lp(5))
        AdminLocalStore.products(this).filter{it.ativo && it.anuncioAtivo}.forEach{p->
            val percent=AdminLocalStore.offerPercent(this,p.nome); val price=AdminLocalStore.promotionalPrice(this,p)
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(0xFF1B1B1B.toInt(),0xFFFF7A00.toInt(),14)}
            card.addView(txt(p.nome,16f,Color.WHITE,true));card.addView(txt("Preço normal: ${money(p.preco)}",13f,Color.LTGRAY),lp(4));card.addView(txt(if(percent>0) "Oferta: $percent% OFF  •  ${money(price)}" else "Sem oferta ativa",14f,if(percent>0) orange else Color.LTGRAY,true),lp(4))
            card.addView(btn(if(percent>0) "EDITAR DESCONTO" else "CRIAR OFERTA",percent>0).apply{setOnClickListener{editar(p.nome,percent)}} ,lp(9));list.addView(card,lp(10))
        }
        scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun editar(nome:String,atual:Int){val input=EditText(this).apply{hint="Desconto em % (0 a 90)";inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText(atual.takeIf{it>0}?.toString().orEmpty());setTextColor(Color.WHITE);setHintTextColor(Color.GRAY)};AlertDialog.Builder(this).setTitle("Oferta: $nome").setMessage("Digite 0 para remover o desconto.").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Salvar"){_,_->val n=input.text.toString().toIntOrNull();if(n==null||n !in 0..90)Toast.makeText(this,"Use um desconto entre 0% e 90%.",Toast.LENGTH_SHORT).show() else {AdminLocalStore.setOfferPercent(this,nome,n);Toast.makeText(this,"Oferta atualizada.",Toast.LENGTH_SHORT).show();render()}}.show()}
    private fun txt(s:String,z:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=z;setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
    private fun btn(s:String,primary:Boolean)=Button(this).apply{text=s;textSize=13f;setTextColor(Color.WHITE);background=bg(if(primary)0xFF292929.toInt() else orange,orange,22)}
    private fun bg(c:Int,st:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),st)}
    private fun lp(t:Int=0)=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply{if(t>0)topMargin=dp(t)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun money(v:Double)=String.format(Locale("pt","BR"),"R$ %.2f",v)
}
