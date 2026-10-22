package com.example.rcarpremium

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivityDadosCadastrais : RcarBaseActivity() {
    private val prefsName = "rcarpremium_dados"
    private lateinit var nome: EditText
    private lateinit var email: EditText
    private lateinit var nascimento: EditText
    private lateinit var cpf: EditText
    private lateinit var telefone: EditText
    private lateinit var telefone2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dados_cadastrais)
        BottomNav.setup(this, "perfil")

        nome = findViewById(R.id.editDadosNome)
        email = findViewById(R.id.editDadosEmail)
        nascimento = findViewById(R.id.editDadosNascimento)
        cpf = findViewById(R.id.editDadosCpf)
        telefone = findViewById(R.id.editDadosTelefone)
        telefone2 = findViewById(R.id.editDadosTelefone2)

        val sessionEmail = RcarLocalStore.currentEmail(this) ?: ""
        val repo = UsuarioLocalRepository(this)
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        nome.setText(prefs.getString("nome_$sessionEmail", repo.nomeDoUsuario(sessionEmail) ?: ""))
        email.setText(sessionEmail)
        nascimento.setText(prefs.getString("nascimento_$sessionEmail", ""))
        cpf.setText(prefs.getString("cpf_$sessionEmail", ""))
        telefone.setText(prefs.getString("telefone_$sessionEmail", ""))
        telefone2.setText(prefs.getString("telefone2_$sessionEmail", ""))
        findViewById<TextView>(R.id.txtDadosResumoNome).text = nome.text.toString().ifBlank { "Cliente RCAR Premium" }
        findViewById<TextView>(R.id.txtDadosResumoEmail).text = email.text.toString()

        nome.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) findViewById<TextView>(R.id.txtDadosResumoNome).text = nome.text.toString().ifBlank { "Cliente RCAR Premium" } }
        findViewById<MaterialButton>(R.id.btnSalvarDados).setOnClickListener { salvar(sessionEmail) }
        findViewById<TextView>(R.id.btnVoltarDados).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnBackDados).setOnClickListener { finish() }
    }

    private fun salvar(sessionEmail: String) {
        if (nome.text.toString().trim().length < 3) { nome.error = "Informe seu nome completo"; nome.requestFocus(); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()) { email.error = "Informe um e-mail válido"; email.requestFocus(); return }
        val p = getSharedPreferences(prefsName, MODE_PRIVATE)
        p.edit()
            .putString("nome_$sessionEmail", nome.text.toString().trim())
            .putString("nascimento_$sessionEmail", nascimento.text.toString().trim())
            .putString("cpf_$sessionEmail", cpf.text.toString().trim())
            .putString("telefone_$sessionEmail", telefone.text.toString().trim())
            .putString("telefone2_$sessionEmail", telefone2.text.toString().trim())
            .apply()
        findViewById<TextView>(R.id.txtDadosResumoNome).text = nome.text.toString()
        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
    }
}
