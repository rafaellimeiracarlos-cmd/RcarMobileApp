package com.example.rcarpremium
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class MainActivityConfiguracoes:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_configuracoes)
 findViewById<Switch>(R.id.swNotificacoes).isChecked=getPreferences(0).getBoolean("notif",true)
 findViewById<Switch>(R.id.swNotificacoes).setOnCheckedChangeListener{_,v->getPreferences(0).edit().putBoolean("notif",v).apply()}
 findViewById<TextView>(R.id.btnPrivacidade).setOnClickListener{startActivity(Intent(this,MainActivityTermos::class.java))}
 findViewById<TextView>(R.id.btnSobre).setOnClickListener{startActivity(Intent(this,MainActivitySobre::class.java))}
 findViewById<TextView>(R.id.btnAjuda).setOnClickListener{startActivity(Intent(this,MainActivitySuporte::class.java))}
 findViewById<TextView>(R.id.btnVoltar).setOnClickListener{finish()}}
}
