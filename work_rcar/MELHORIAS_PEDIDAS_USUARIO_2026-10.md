# Melhorias aplicadas — RCarPremium

- Criada tela administrativa de produtos cadastrados com ação de edição.
- Formulário de produto agora carrega os dados existentes e salva atualizações.
- Criado painel de ofertas do CEO com desconto percentual de 0% a 90%, persistido localmente.
- Catálogo passa a exibir preço promocional e a tela de ofertas lista apenas produtos com desconto.
- Painel interno ganhou atalhos para produtos cadastrados/edição, prévia da loja e ofertas.
- Prévia abre a loja real para navegar pelo catálogo.
- Adicionado atalho flutuante “↑ TOPO” na tela principal da loja.
- Carrinho, pedidos e perfil exigem login de cliente pela navegação inferior; tentativas de compra sem login oferecem acesso ao login.
- Depois do login, a navegação pode retornar ao carrinho/pedidos/perfil conforme o destino solicitado.
- Adicionado resumo de endereço de entrega no checkout e exigência de endereço padrão antes de finalizar.
- Adicionados botões flutuantes de voltar no topo e na parte inferior das telas via atividade base.

## Limitações importantes

- O armazenamento é local no dispositivo, conforme a arquitetura atual; não há sincronização entre dispositivos.
- O valor do frete não pode ser calculado de forma real sem integração com transportadora/serviço de frete. A tela indica que a entrega deve ser consultada.
- Um QR Code Pix válido exige uma chave Pix da loja e integração de cobrança para gerar o payload/QR correto. O aplicativo não simula confirmação de pagamento nem gera QR falso; a finalização Pix fica bloqueada até essa configuração.
- A compilação não foi validada neste ambiente por falta de Android SDK configurado.
