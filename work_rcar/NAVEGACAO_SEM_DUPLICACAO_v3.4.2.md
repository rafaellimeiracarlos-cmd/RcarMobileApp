# RCarPremium v3.4.2 — revisão de navegação

## Objetivo
Remover botões **Voltar** duplicados que ficavam sobre cabeçalhos, cartões de produtos e menus flutuantes.

## Alterações
- `RcarBaseActivity.kt`: retirada a sobreposição automática de dois botões "Voltar" em todas as telas. O gesto/botão padrão Voltar do Android continua funcionando.
- `MarketplaceUi.kt`: apenas uma seta de retorno no canto superior esquerdo, logo RCarPremium centralizada e rodapé flutuante com ação de destino (por exemplo, Início da Loja), sem um segundo "Voltar".
- Telas do Marketplace de localização, assistente e lojas: o rodapé abre **Início da Loja** em vez de duplicar o retorno ao Marketplace.
- Área de compatibilidade do CEO: o rodapé abre o **Painel Interno**; a seta do cabeçalho retorna à tela anterior.
- Retirados botões inferiores "Voltar" redundantes em: Produtos Cadastrados, Meus Produtos (funcionário), Ofertas, Solicitações de Estoque, Margem de Lucro, Insights do CEO e Sessões do CEO.
- Não foram removidos os controles **Voltar ao topo**, navegar para Início, atualizar histórico, nem botões de compra ou de formulário.

## Teste manual recomendado
1. Abra o Marketplace: deve existir somente uma seta no cabeçalho, sem botões sobre os anúncios; rodapé flutuante com **Início da Loja**.
2. Role até o final dos produtos: nenhum botão "Voltar" deve cobrir cartões ou o rodapé.
3. Abra Assistente de Compra, Lojas Próximas e Buscar pela Placa: compare o retorno da seta e o atalho para a loja.
4. Acesse Painel CEO: Ofertas, Produtos Cadastrados, Solicitações, Insights, Margens e Sessões: apenas o retorno necessário.
5. Teste o gesto Android de voltar, a navegação inferior, carrinho, pedidos e checkout.

## Validação disponível
- Verificação estática: 75 XMLs bem formados e remoção confirmada do código de overlay global.
- Compilação Android integral: **não executada**, pois o Gradle 9.4.1 depende de download de `services.gradle.org`, inacessível no ambiente utilizado.

Abra a pasta `work_rcar` desta versão no Android Studio e execute **Build > Rebuild Project** antes de testar no emulador.
