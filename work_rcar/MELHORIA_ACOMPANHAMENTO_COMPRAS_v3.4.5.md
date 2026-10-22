# RCarPremium v3.4.5 — Acompanhe sua compra e compre novamente

## O que mudou

- Nova tela nativa de acompanhamento, seguindo a identidade preta e laranja e a logo original.
- Cabeçalho limpo, com apenas um retorno aos pedidos (ícone no canto superior).
- Cartão de status e total do pedido, etapas de acompanhamento com indicação visual da etapa atual.
- Transportadora, código de rastreio, previsão, última localização e mapa **apenas quando houver dados reais** informados.
- Seção **Produtos do pedido**: cada item tem nome, quantidade, valor registrado na compra e botão **Ver produto comprado**.
- **Ver produto comprado** abre detalhes preservados do pedido; quando ainda existe anúncio no catálogo, também permite abrir o anúncio atual.
- CTA flutuante **Comprar novamente**: compara os itens do pedido com o catálogo/estoque atual, confirma preço/quantidade, adiciona ao carrinho e abre a tela do carrinho. Não efetua cobrança.
- Se algum item não estiver disponível, a recompra completa é bloqueada sem alterar o carrinho; o cliente pode buscar alternativas no catálogo.
- Para compras novas, os itens são armazenados individualmente no pedido (nome, quantidade e preço no momento da compra), além do resumo legado.
- Pedidos antigos continuam visíveis pelo resumo salvo, mas não possuem preço unitário histórico se não foi registrado na época.
- A tela mantém a NF-e vinculada ao pedido com acesso sujeito às permissões existentes.

## Arquivos principais

- `app/src/main/java/com/example/rcarpremium/MainActivityAcompanharPedido.kt`
- `app/src/main/java/com/example/rcarpremium/PedidoLocalStore.kt`
- `app/build.gradle.kts` (versão 3.4.5)

## Como verificar no Android Studio

1. Abra a pasta `work_rcar` no Android Studio.
2. Execute `Build > Rebuild Project`.
3. Entre com uma conta de cliente ativa e acesse **Pedidos**.
4. Abra **Acompanhar entrega** em um pedido.
5. Use **Ver produto comprado** e confira os dados históricos; se houver anúncio atual, abra-o.
6. Toque **Comprar novamente**: confirme que os itens entram no carrinho com preços e estoque atuais.
7. Teste produto sem estoque, pausado ou removido; nenhum item deve ser colocado no carrinho automaticamente.
8. Confira que CEO/funcionário continuam com permissão administrativa e que a recompra comercial exige cliente (exceto simulação do CEO quando ativada).

## Limitações

- Este projeto usa armazenamento local; dados de pedidos e localizações não são sincronizados entre aparelhos.
- As fotos existentes são imagens ilustrativas do catálogo, não um arquivo histórico de fotos dos itens vendidos.
- Pedidos antigos não armazenavam preços por item nem SKUs: são exibidos com as informações disponíveis; a recompra só é possível quando há correspondência segura no catálogo atual.
- Checkout bancário e rastreio ao vivo dependem de serviços externos e backend para uso comercial.
- A compilação Android completa requer SDK e Gradle configurados na máquina do usuário.
