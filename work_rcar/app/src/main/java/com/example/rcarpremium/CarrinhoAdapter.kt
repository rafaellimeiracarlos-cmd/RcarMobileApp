package com.example.rcarpremium

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarrinhoAdapter(
    private val listaProdutos: MutableList<ProdutoItem>,
    private val onQuantidadeAlterada: () -> Unit
) : RecyclerView.Adapter<CarrinhoAdapter.CarrinhoViewHolder>() {

    class CarrinhoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeProduto)
        val txtPreco: TextView = itemView.findViewById(R.id.txtPrecoProduto)
        val txtQtd: TextView = itemView.findViewById(R.id.txtQtdProduto)
        val btnDiminuir: Button = itemView.findViewById(R.id.btnDiminuir)
        val btnAumentar: Button = itemView.findViewById(R.id.btnAumentar)
        val btnRemover: ImageView = itemView.findViewById(R.id.btnRemoverProduto) // Mapeia o ícone/botão de remover
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrinhoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carrinho, parent, false)
        return CarrinhoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarrinhoViewHolder, position: Int) {
        val produto = listaProdutos[position]

        val precoTotalItem = produto.preco * produto.quantidade

        holder.txtNome.text = produto.nome
        holder.txtPreco.text = String.format("R$ %.2f", precoTotalItem)
        holder.txtQtd.text = produto.quantidade.toString()

        // Botão de Aumentar (+)
        holder.btnAumentar.setOnClickListener {
            produto.quantidade++
            notifyItemChanged(holder.adapterPosition)
            onQuantidadeAlterada()
        }

        // Botão de Diminuir (-)
        holder.btnDiminuir.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (produto.quantidade > 1) {
                produto.quantidade--
                notifyItemChanged(currentPosition)
                onQuantidadeAlterada()
            } else {
                // Remove o item se a quantidade chegar a 0
                removerItem(currentPosition)
            }
        }

        // Botão de Remover (Lixeira)
        holder.btnRemover.setOnClickListener {
            removerItem(holder.adapterPosition)
        }
    }

    // Função interna para tratar a remoção segura do item
    private fun removerItem(position: Int) {
        if (position != RecyclerView.NO_POSITION && position < listaProdutos.size) {
            listaProdutos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, listaProdutos.size)
            onQuantidadeAlterada() // Atualiza o valor total no rodapé
        }
    }

    override fun getItemCount(): Int = listaProdutos.size
}
