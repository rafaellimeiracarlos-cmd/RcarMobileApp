package com.example.rcarpremium

/** Catálogo visual e regras de correspondência entre categorias e produtos. */
data class ProductCategory(
    val name: String,
    val icon: String,
    val description: String,
    val keywords: List<String>
)

object CategoryCatalog {
    val categories = listOf(
        ProductCategory("Shampoo Automotivo", "🧴", "Lavagem e cuidado da pintura", listOf("shampoo", "lavagem", "detergente automotivo")),
        ProductCategory("Cera e Cristalização", "✨", "Brilho, proteção e acabamento", listOf("cera", "cristal", "polimento", "cristalização", "cristalizacao")),
        ProductCategory("Higienização e Limpeza", "🧽", "Limpeza interna e conservação", listOf("higien", "limpeza", "limpador", "limpa", "desengraxante")),
        ProductCategory("Óleos e Lubrificantes", "🛢️", "Lubrificação e cuidado do motor", listOf("óleo", "oleo", "lubrificante", "graxa")),
        ProductCategory("Pneus e Rodas", "🛞", "Aderência, rodas e segurança", listOf("pneu", "roda", "calota")),
        ProductCategory("Freios", "🛑", "Pastilhas, discos e componentes", listOf("freio", "pastilha", "disco de freio", "pinça de freio")),
        ProductCategory("Suspensão", "⚙️", "Amortecedores e estabilidade", listOf("suspensão", "suspensao", "amortecedor", "mola helicoidal")),
        ProductCategory("Elétrica", "🔌", "Baterias, lâmpadas e conexões", listOf("elétrica", "eletrica", "bateria", "farol", "lâmpada", "lampada", "alternador", "fusível", "fusivel")),
        ProductCategory("Ferramentas", "🧰", "Itens para reparo e manutenção", listOf("ferramenta", "chave de", "soquete", "alicate", "macaco hidráulico")),
        ProductCategory("Acessórios", "🚘", "Detalhes que completam seu carro", listOf("acessório", "acessorio", "tapete", "capa", "suporte", "organizador"))
    )

    fun matchingProducts(categoryName: String, products: List<AdminProduto>): List<AdminProduto> {
        val category = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            ?: return products.filter { it.categoria.equals(categoryName, ignoreCase = true) }
        return products.filter { product ->
            product.categoria.equals(category.name, ignoreCase = true) ||
                category.keywords.any { keyword ->
                    product.nome.contains(keyword, ignoreCase = true) ||
                        product.categoria.contains(keyword, ignoreCase = true)
                }
        }
    }
}
