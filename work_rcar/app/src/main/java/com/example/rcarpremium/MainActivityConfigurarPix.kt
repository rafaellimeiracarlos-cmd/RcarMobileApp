package com.example.rcarpremium

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivityConfigurarPix : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email=RcarLocalStore.currentEmail(this)
        if(email.isNullOrBlank() || !UsuarioLocalRepository(this).isCeo(email)){Toast.makeText(this,"Apenas o CEO pode configurar o Pix da loja.",Toast.LENGTH_LONG).show();finish();return}
        val prefs=getSharedPreferences("rcarpremium_config",MODE_PRIVATE)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(24));setBackgroundColor(Color.rgb(10,10,10))}
        val scroll=ScrollView(this);val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        form.addView(TextView(this).apply{text="CONFIGURAR PIX DA LOJA";textSize=22f;setTextColor(Color.rgb(255,122,0));setTypeface(null,1)})
        form.addView(TextView(this).apply{text="Informe a chave Pix que pertence à empresa. Esses dados serão usados para montar o QR Code de pagamento.";textSize=14f;setTextColor(Color.LTGRAY)},lp(8))
        val key=field("Chave Pix (CPF/CNPJ, e-mail, telefone ou chave aleatória)",prefs.getString("pix_key","") ?: "")
        val merchant=field("Nome do recebedor (até 25 caracteres)",prefs.getString("pix_name","RCarPremium") ?: "RCarPremium")
        val city=field("Cidade do recebedor (até 15 caracteres)",prefs.getString("pix_city","SAO PAULO") ?: "SAO PAULO")
        val frete=field("Frete padrão da loja em reais (ex.: 14,90)",prefs.getString("frete_padrao_texto","") ?: "")
        frete.inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        form.addView(key,lp(16));form.addView(merchant,lp(10));form.addView(city,lp(10));form.addView(frete,lp(10))
        form.addView(TextView(this).apply{text="Configure também o frete padrão que será somado ao pedido. O QR é gerado localmente; o app não confirma automaticamente o recebimento do pagamento.";textSize=12f;setTextColor(Color.YELLOW)},lp(12))

        // Previsão própria da loja, separada da chave Pix e de cotações de transportadora.
        form.addView(TextView(this).apply {
            text="PRAZO DE ENTREGA INFORMADO PELA LOJA"
            textSize=16f
            setTypeface(null,1)
            setTextColor(Color.rgb(255,122,0))
        }, lp(20))
        val prazo=field("Prazo máximo estimado em dias úteis (1 a 60)",
            prefs.getInt("prazo_entrega_dias_uteis", 0).takeIf { it > 0 }?.toString() ?: "")
        prazo.inputType=InputType.TYPE_CLASS_NUMBER
        form.addView(prazo,lp(10))
        form.addView(TextView(this).apply {
            text="Essa é uma previsão manual informada pela loja. A consulta de CEP identifica a região, mas não gera cotação automática ou prazo por transportadora."
            textSize=12f
            setTextColor(Color.LTGRAY)
        },lp(8))
        form.addView(Button(this).apply {
            text="SALVAR PRAZO DE ENTREGA"
            setTextColor(Color.WHITE)
            background=rounded(Color.rgb(255,122,0))
            setOnClickListener {
                val dias=prazo.text.toString().trim().toIntOrNull()
                if(dias==null || dias !in 1..60){
                    Toast.makeText(this@MainActivityConfigurarPix,"Informe um prazo válido de 1 a 60 dias úteis.",Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putInt("prazo_entrega_dias_uteis",dias).apply()
                    Toast.makeText(this@MainActivityConfigurarPix,"Prazo informativo salvo: até $dias dias úteis.",Toast.LENGTH_SHORT).show()
                }
            }
        },lp(10))
        form.addView(Button(this).apply{text="SALVAR CONFIGURAÇÃO PIX";setTextColor(Color.WHITE);background=rounded(Color.rgb(255,122,0));setOnClickListener{val k=key.text.toString().trim();val n=merchant.text.toString().trim().take(25);val c=city.text.toString().trim().uppercase().take(15);val freightText=frete.text.toString().trim();val freightRaw=if(freightText.contains(",")) freightText.replace(".","").replace(",",".") else freightText;val freight=freightRaw.toDoubleOrNull();if(k.isBlank()||n.isBlank()||c.isBlank()||freight==null||freight<0){Toast.makeText(this@MainActivityConfigurarPix,"Preencha chave, nome, cidade e um frete válido.",Toast.LENGTH_LONG).show()}else{prefs.edit().putString("pix_key",k).putString("pix_name",n).putString("pix_city",c).putFloat("frete_padrao",freight.toFloat()).putString("frete_padrao_texto",frete.text.toString().trim()).apply();Toast.makeText(this@MainActivityConfigurarPix,"Pix e frete padrão salvos.",Toast.LENGTH_SHORT).show();finish()}}},lp(18))
        form.addView(Button(this).apply{text="VOLTAR";setTextColor(Color.WHITE);background=rounded(Color.rgb(36,36,36));setOnClickListener{finish()}},lp(10))
        scroll.addView(form);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun field(h:String,v:String)=EditText(this).apply{hint=h;setText(v);setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.rgb(30,30,30))}
    private fun rounded(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(12).toFloat();setStroke(dp(1),Color.rgb(255,122,0))}
    private fun lp(t:Int=0)=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply{if(t>0)topMargin=dp(t)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
