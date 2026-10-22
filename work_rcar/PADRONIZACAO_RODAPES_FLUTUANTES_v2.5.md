# RCarPremium v2.5 — Rodapés flutuantes

## O que mudou

- `bottom_nav.xml`: o menu inferior compartilhado agora tem 68dp, laterais livres, borda laranja, fundo preto e seleção destacada.
- O mesmo menu é utilizado nas telas: loja, categorias, lista de produtos, pedidos, perfil, dados cadastrais, endereços e detalhes do produto.
- `activity_main_ver_produtos.xml`: rodapé de compra e navegação flutuantes, sem duplicação da barra compartilhada.
- `activity_main_item_carrinho.xml`: resumo total e ações em cartão flutuante.
- `activity_main_pagamento.xml`: botão de pagamento em cartão flutuante.
- `activity_termos.xml`: botão de aceitar termos em rodapé flutuante.
- `activity_main.xml`: faixa discreta inferior flutuante no login.
- `MainActivitySessoesCeo.kt`: rodapé de ações de auditoria agrupado em cartão com cantos arredondados.
- `RcarBaseActivity.kt`: botão de voltar inferior é deslocado para evitar o rodapé de navegação, pagamento e carrinho.

As telas administrativas que não tinham menu inferior continuam usando sua navegação existente; nenhum menu de loja foi imposto às telas restritas do CEO.

## Como testar

1. Abra a pasta `work_rcar` no Android Studio.
2. Aguarde a sincronização do Gradle e execute `Build > Make Project`.
3. Confira as telas Loja, Categorias, Produtos, Carrinho, Pedidos, Perfil, Pagamento, Termos e Sessões CEO.
4. Teste a rolagem, o botão voltar e a navegação em um aparelho estreito (ex.: 360dp de largura).

> Os layouts XML e o pacote ZIP foram validados automaticamente; a compilação completa exige o Android SDK/Gradle disponíveis no seu computador.
