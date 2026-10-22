package com.example.rcarpremium
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class MainActivitySuporte:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_suporte)
 findViewById<Button>(R.id.btnWhats).setOnClickListener{Toast.makeText(this,"WhatsApp de atendimento: (11) 98765-4321",Toast.LENGTH_LONG).show()}
 findViewById<Button>(R.id.btnChat).setOnClickListener{Toast.makeText(this,"Chat online iniciado. Um atendente responderá em breve.",Toast.LENGTH_SHORT).show()}
 findViewById<Button>(R.id.btnEmail).setOnClickListener{Toast.makeText(this,"E-mail: atendimento@rcarpremium.com",Toast.LENGTH_LONG).show()}
 findViewById<TextView>(R.id.btnVoltar).setOnClickListener{finish()}}
}
