package com.example.rcarpremium

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivitySobre : RcarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre)

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }
}
