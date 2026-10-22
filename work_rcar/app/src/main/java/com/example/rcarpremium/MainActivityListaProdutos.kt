package com.example.rcarpremium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivityListaProdutos : RcarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_produtos)

        val categoria = intent.getStringExtra("CATEGORIA")
        val busca = intent.getStringExtra("BUSCA_PRODUTO")?.trim().orEmpty()
        val modoOfertas = intent.getBooleanExtra("MODO_OFERTAS", false)

        val titulo = findViewById<TextView>(R.id.txtTituloLista)
        val subtitulo = findViewById<TextView>(R.id.txtSubtituloLista)
        val container = findViewById<LinearLayout>(R.id.listaProdutos)

        titulo.text = when {
            modoOfertas -> "Ofertas da semana"
            busca.isNotBlank() -> "Resultados da busca"
            else -> categoria ?: "Todos os produtos"
        }
        subtitulo.text = when {
            modoOfertas -> "Produtos em destaque • confira preço e disponibilidade"
            busca.isNotBlank() -> "Resultados para: $busca"
            else -> "Selecione um produto para ver os detalhes"
        }

        if (CompraTesteCeoStore.ativo(this)) {
            subtitulo.text = "🧪 MODO TESTE CEO — ${subtitulo.text}"
            subtitulo.setTextColor(Color.rgb(255, 156, 35))
        }

        findViewById<TextView>(R.id.btnVoltarLista).setOnClickListener { finish() }

        val todos = AdminLocalStore.products(this).filter { it.ativo && it.anuncioAtivo }
        val categoriaConhecida = CategoryCatalog.categories.any {
            it.name.equals(categoria, ignoreCase = true)
        }
        val filtrados = when {
            modoOfertas -> todos.filter { AdminLocalStore.offerPercent(this, it.nome) > 0 }
            busca.isNotBlank() -> todos.filter {
                it.nome.contains(busca, ignoreCase = true) ||
                    it.categoria.contains(busca, ignoreCase = true)
            }
            categoriaConhecida -> CategoryCatalog.matchingProducts(categoria.orEmpty(), todos)
            else -> todos.filter {
                categoria.isNullOrBlank() ||
                    categoria == "Todos" ||
                    it.categoria.equals(categoria, ignoreCase = true) ||
                    categoria.contains(it.categoria, ignoreCase = true) ||
                    it.nome.contains(categoria, ignoreCase = true)
            }
        }

        // Uma categoria vazia deve mostrar o estado vazio, nunca produtos de outra categoria.
        val lista = if (filtrados.isEmpty() && busca.isBlank() && !modoOfertas && !categoriaConhecida) todos else filtrados

        if (lista.isEmpty()) {
            container.addView(
                TextView(this).apply {
                    text = "Nenhum produto encontrado.\nTente outra palavra ou explore as categorias."
                    textSize = 15f
                    setTextColor(Color.LTGRAY)
                    setPadding(18, 26, 18, 26)
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            return
        }

        lista.forEach { produto ->
            val card = layoutInflater.inflate(R.layout.item_produto_catalogo, container, false)
            card.findViewById<TextView>(R.id.itemNome).text = produto.nome
            card.findViewById<TextView>(R.id.itemCategoria).text =
                "${produto.categoria} • ${produto.estoque} em estoque"
            val desconto = AdminLocalStore.offerPercent(this, produto.nome)
            val precoExibido = AdminLocalStore.promotionalPrice(this, produto)
            card.findViewById<TextView>(R.id.itemPreco).text = if (desconto > 0)
                String.format(Locale("pt", "BR"), "OFERTA %d%% OFF  •  R$ %.2f", desconto, precoExibido)
            else String.format(Locale("pt", "BR"), "R$ %.2f", produto.preco)
            card.setOnClickListener {
                startActivity(Intent(this, MainActivityVerProdutos::class.java).apply {
                    putExtra("PRODUTO_NOME", produto.nome)
                    putExtra("PRODUTO_PRECO", precoExibido)
                })
            }
            container.addView(
                card,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
    }
}
