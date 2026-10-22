# RCarPremium — gestão administrativa adicionada

## Funcionalidades
- Painel de insights do CEO: vendas de hoje, acumulado registrado, valor estimado do estoque, quantidade de produtos, alertas de estoque baixo, usuários e sessões abertas.
- Gestão de usuários exclusiva do CEO: criar contas de funcionário/cliente, bloquear/reativar e excluir contas não-CEO.
- Permissões individuais para funcionários: cadastro de produtos, gerenciamento de estoque, consulta de vendas e gerenciamento de pedidos (a permissão de pedidos já fica registrada para integração com a tela de pedidos).
- Histórico de acesso: perfil, e-mail, data/hora de entrada, saída e duração. A tela de sessões e os insights são restritos ao CEO.
- Vendas registradas agora incluem data/hora para cálculo de vendas do dia, quantidade de vendas, ticket médio e histórico recente.
- A confirmação de compra valida o estoque, reduz as quantidades vendidas e só registra a venda quando há disponibilidade suficiente.
- A tela de sessões atualiza a duração dos acessos ativos a cada segundo enquanto estiver aberta.
- Foram incluídas as duas contas CEO solicitadas anteriormente.
- Reforço de verificação de permissão ao abrir cadastro de produtos, estoque e vendas, não apenas ocultação dos botões.

## Como testar
1. Entrar como CEO usando as credenciais de demonstração já documentadas no projeto.
2. Abrir **Logins e permissões**, criar um funcionário e marcar as funções necessárias.
3. Sair e entrar com o funcionário; somente as funções permitidas devem aparecer e ser aceitas.
4. Voltar ao CEO e abrir **Insights executivos** e **Sessões e tempo de acesso**.

## Limitações importantes
Este projeto continua usando armazenamento local do Android. Os dados e sessões são por dispositivo/instalação e não sincronizam entre aparelhos. Para produção, implemente backend com autenticação real, autorização validada no servidor, banco de dados central, trilha de auditoria protegida e políticas de privacidade. O registro de vendas do dia só considera vendas gravadas pelo método `AdminLocalStore.registerSale`; confira o fluxo de pagamento para garantir que ele seja chamado após uma venda confirmada. O contador de sessão é atualizado em tempo real apenas enquanto a tela de auditoria está aberta no dispositivo; não é um cronômetro remoto. O fechamento inesperado do aplicativo pode deixar uma sessão registrada como ativa até que seja tratada. As credenciais de demonstração estão no README e devem ser trocadas antes de qualquer uso real.
