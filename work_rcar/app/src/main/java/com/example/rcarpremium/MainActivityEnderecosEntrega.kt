package com.example.rcarpremium

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONArray
import org.json.JSONObject

class MainActivityEnderecosEntrega : RcarBaseActivity() {
    private val prefsName = "rcarpremium_enderecos"
    private lateinit var container: LinearLayout
    private lateinit var principal: TextView
    private var enderecos = mutableListOf<Endereco>()

    data class Endereco(var titulo: String, var rua: String, var numero: String, var bairro: String, var cidade: String, var estado: String, var cep: String, var complemento: String, var padrao: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enderecos_entrega)
        BottomNav.setup(this, "perfil")
        container = findViewById(R.id.containerEnderecos)
        principal = findViewById(R.id.txtPrincipalEndereco)
        load()
        render()
        findViewById<TextView>(R.id.btnBackEnderecos).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNovoEndereco).setOnClickListener { showForm(null) }
        findViewById<TextView>(R.id.btnEditarPrincipal).setOnClickListener { enderecos.firstOrNull { it.padrao }?.let { showForm(enderecos.indexOf(it)) } }
        findViewById<TextView>(R.id.btnUsarPrincipal).setOnClickListener { Toast.makeText(this, "Endereço principal selecionado.", Toast.LENGTH_SHORT).show() }
    }

    private fun load() {
        val raw = getSharedPreferences(prefsName, MODE_PRIVATE).getString("lista", null)
        if (raw.isNullOrBlank()) {
            enderecos = mutableListOf()
            save()
            return
        }
        try {
            val arr = JSONArray(raw)
            enderecos = MutableList(arr.length()) { i -> val o=arr.getJSONObject(i); Endereco(o.getString("titulo"),o.getString("rua"),o.getString("numero"),o.getString("bairro"),o.getString("cidade"),o.getString("estado"),o.getString("cep"),o.optString("complemento"),o.optBoolean("padrao")) }.toMutableList()
        } catch (_: Exception) { enderecos = mutableListOf() }
    }

    private fun save() {
        val arr = JSONArray()
        enderecos.forEach { e -> arr.put(JSONObject().apply { put("titulo",e.titulo);put("rua",e.rua);put("numero",e.numero);put("bairro",e.bairro);put("cidade",e.cidade);put("estado",e.estado);put("cep",e.cep);put("complemento",e.complemento);put("padrao",e.padrao) }) }
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString("lista", arr.toString()).apply()
    }

    private fun render() {
        container.removeAllViews()
        val principalEndereco = enderecos.firstOrNull { it.padrao } ?: enderecos.firstOrNull()
        if (principalEndereco != null) {
            principal.text = format(principalEndereco)
            principalEndereco.padrao = true
        } else principal.text = "Nenhum endereço cadastrado"
        enderecos.forEachIndexed { index, e ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_endereco, container, false)
            view.findViewById<TextView>(R.id.txtTituloEndereco).text = e.titulo + if (e.padrao) "  •  PADRÃO" else ""
            view.findViewById<TextView>(R.id.txtDetalheEndereco).text = format(e)
            view.findViewById<TextView>(R.id.btnEditarEndereco).setOnClickListener { showForm(index) }
            view.findViewById<TextView>(R.id.btnExcluirEndereco).setOnClickListener { confirmDelete(index) }
            view.findViewById<TextView>(R.id.btnUsarEndereco).setOnClickListener { setDefault(index) }
            view.findViewById<TextView>(R.id.btnMenuEndereco).setOnClickListener { showMenu(index, view) }
            container.addView(view)
        }
    }

    private fun format(e: Endereco) = "${e.rua}, ${e.numero} - ${e.bairro}\n${e.cidade} - ${e.estado} | CEP ${e.cep}" + if (e.complemento.isNotBlank()) "\n${e.complemento}" else ""

    private fun setDefault(index: Int) { enderecos.forEachIndexed { i, e -> e.padrao = i == index }; save(); render(); Toast.makeText(this,"Endereço padrão atualizado.",Toast.LENGTH_SHORT).show() }

    private fun showMenu(index: Int, anchor: android.view.View) {
        val popup = PopupMenu(this, anchor); popup.menu.add("Definir como padrão"); popup.menu.add("Editar"); popup.menu.add("Excluir")
        popup.setOnMenuItemClickListener { when(it.title.toString()) { "Definir como padrão" -> setDefault(index); "Editar" -> showForm(index); "Excluir" -> confirmDelete(index) }; true }; popup.show()
    }

    private fun confirmDelete(index: Int) {
        if (enderecos.size <= 1) { Toast.makeText(this,"Mantenha pelo menos um endereço cadastrado.",Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("Excluir endereço?").setMessage("Esse endereço será removido do seu cadastro.").setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ -> val wasDefault=enderecos[index].padrao; enderecos.removeAt(index); if(wasDefault) enderecos.first().padrao=true; save(); render() }.show()
    }

    private fun showForm(editIndex: Int?) {
        val box = LinearLayout(this); box.orientation=LinearLayout.VERTICAL; box.setPadding(24,4,24,0)
        val fields = listOf("Nome do endereço (Casa, Trabalho...)" to "", "Rua" to "", "Número" to "", "Bairro" to "", "Cidade" to "", "Estado (UF)" to "", "CEP" to "", "Complemento" to "")
        val inputs = fields.map { (hint,_) -> EditText(this).apply { this.hint=hint; setSingleLine(true); setPadding(16,8,16,8); box.addView(this, LinearLayout.LayoutParams(-1,58).apply{bottomMargin=6}) } }
        editIndex?.let { val e=enderecos[it]; val values=listOf(e.titulo,e.rua,e.numero,e.bairro,e.cidade,e.estado,e.cep,e.complemento); inputs.forEachIndexed { i,v -> v.setText(values[i]) } }
        AlertDialog.Builder(this).setTitle(if(editIndex==null)"Novo endereço" else "Editar endereço").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar") { _, _ ->
            if(inputs[0].text.isBlank()||inputs[1].text.isBlank()||inputs[2].text.isBlank()||inputs[4].text.isBlank()||inputs[5].text.isBlank()||inputs[6].text.isBlank()){ Toast.makeText(this,"Preencha os campos obrigatórios.",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            val e=Endereco(inputs[0].text.toString(),inputs[1].text.toString(),inputs[2].text.toString(),inputs[3].text.toString(),inputs[4].text.toString(),inputs[5].text.toString().uppercase(),inputs[6].text.toString(),inputs[7].text.toString(), editIndex==null && enderecos.isEmpty())
            if(editIndex==null){ if(enderecos.isEmpty()) e.padrao=true; enderecos.add(e) } else { e.padrao=enderecos[editIndex].padrao; enderecos[editIndex]=e }
            save(); render(); Toast.makeText(this,"Endereço salvo!",Toast.LENGTH_SHORT).show()
        }.show()
    }
}
