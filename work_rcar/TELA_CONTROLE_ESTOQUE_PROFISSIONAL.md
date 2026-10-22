# Tela de Controle de Estoque — RCarPremium

A tela `MainActivityEstoque.kt` foi redesenhada em Kotlin nativo para aproximar a interface da referência visual em `design/controle_estoque_referencia.png`.

## Alterações
- Identidade visual preta e laranja, alinhada à RCarPremium.
- Cabeçalho com a logo existente `R.drawable.rcarlogo`.
- Resumo do inventário com quantidade de produtos, unidades, alertas de estoque baixo e valor estimado.
- Cartões de produtos com preço, categoria, estoque atual e controles para diminuir/aumentar uma unidade.
- Aviso visual para produtos abaixo do estoque mínimo cadastrado.
- Barra inferior flutuante com Início, Estoque, Vendas e Mais.
- Verificação de permissão `gerenciar_estoque` mantida.

## Arquivo principal
`app/src/main/java/com/example/rcarpremium/MainActivityEstoque.kt`

A imagem é apenas uma referência visual; a interface real é construída nativamente pelo código Kotlin para manter os botões funcionais e os dados ligados ao armazenamento local do aplicativo.
