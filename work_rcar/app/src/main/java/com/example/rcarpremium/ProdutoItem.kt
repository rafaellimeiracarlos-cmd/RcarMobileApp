package com.example.rcarpremium

data class ProdutoItem(
    val nome: String,
    val preco: Double,
    var quantidade: Int,
    val imagemResId: Int
)
