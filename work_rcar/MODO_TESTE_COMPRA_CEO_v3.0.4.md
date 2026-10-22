# RCarPremium — Modo de Teste de Compra do CEO (v3.0.4)

## Como acessar

1. Faça login na conta **CEO**.
2. Entre em **Painel do CEO → 🧪 Simular compra • Modo CEO**.
3. Leia o aviso e toque em **Iniciar teste**.
4. Escolha produtos pela loja, categorias ou lista. Adicione ao carrinho ou toque em **Comprar agora**.
5. No carrinho de teste, toque em **Simular checkout**.
6. Na etapa de pagamento, informe um **endereço fictício**, selecione Pix, Cartão ou Boleto e toque em **Confirmar simulação — sem cobrança**.
7. Visualize a confirmação e consulte o **Histórico de compras de teste** no painel CEO (ou pelo botão *Pedidos* da navegação em modo de teste).
8. Saia usando **SAIR/CEO** no topo da loja, o botão **Voltar ao painel** ao concluir, ou **Encerrar modo de teste ativo** no painel.

## Isolamento da simulação

- Só pode ser iniciado por conta CEO ativa autenticada.
- O CEO **continua CEO**; suas permissões não são alteradas.
- **Carrinho de teste separado**, sem afetar compras reais.
- A simulação **não gera Pix, código bancário, transação de cartão, boleto ou ordem de entrega**.
- A simulação **não baixa estoque**, **não registra venda ou receita real**, **não altera margem de lucro** e **não cria pedido no `PedidoLocalStore`**.
- Testes possuem identificador `TESTE-...`, endereço fictício e histórico separado no dispositivo.
- Novo login/logout encerra a sessão de teste; o histórico permanece para consulta e pode ser apagado no painel.
- O frete mostrado é um valor simulado com base no frete padrão cadastrado, ou R$ 0,00 se não configurado. Não representa cotação.

## Limitações

- Dados de teste ficam em SharedPreferences do aparelho; não sincronizam com backend.
- O checkout real continua com as restrições atuais e exige conta de cliente ativa.
- **Não utilizar** este modo para validar liquidação bancária, geração de QR Pix de produção, transportadoras ou integrações reais.
- Compilação no Android Studio e teste no emulador ainda necessários para confirmar a execução.

## Arquivos principais

- `CompraTesteCeoStore.kt` — sessão, carrinho, endereço e histórico de simulações.
- `MainActivityHistoricoTesteCeo.kt` — consulta e exclusão do histórico exclusivo de testes.
- `MainActivityPainelInterno.kt`, `activity_painel_interno.xml` — acessos no painel do CEO.
- `RcarLocalStore.kt`, `AdminLocalStore.kt`, `PedidoLocalStore.kt` — guardas e separação de dados.
- `MainActivitycomprasonline.kt`, `MainActivityVerProdutos.kt`, `MainActivityListaProdutos.kt`, `BottomNav.kt` — navegação de teste.
- `MainActivityItemCarrinho.kt`, `MainActivityPagamento.kt` — fluxo de compra simulada.
