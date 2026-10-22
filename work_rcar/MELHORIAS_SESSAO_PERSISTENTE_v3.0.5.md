# RCarPremium 3.0.5 — sessão persistente para os três perfis

## Comportamento
- Clientes, funcionários e CEO permanecem conectados após navegar, sair da tela ou reabrir o app neste aparelho.
- O botão **Perfil** mostra o perfil correto, sem tentar entrar outra vez.
- A única saída voluntária é **Sair da conta** no Perfil, com confirmação. No painel interno o antigo botão **Voltar** foi alterado para **Meu perfil**.
- Conta administrativa não é mais apagada ao abrir por engano um perfil de cliente.
- A loja atualiza **LOGIN / PERFIL** ao retornar de outras telas.
- Categorias/Início são livres; Carrinho é acessível com conta ativa; Pedidos respeita permissões do funcionário.
- Checkout comercial permanece exclusivo de cliente; o CEO continua podendo simular compras, sem cobrança e sem alterar estoque.
- Carrinhos locais agora são separados por e-mail, migrando o carrinho legado uma vez quando aplicável.

## Testes manuais recomendados no Android Studio
1. Fazer login como cliente, fechar o app e reabrir. Abrir Perfil, Carrinho e Pedidos pela barra inferior: não deve solicitar login novamente.
2. Entrar como CEO, abrir Categorias, Carrinho e Perfil, voltar ao painel. O login deve continuar válido.
3. Testar funcionário com/sem permissão gerenciar_pedidos: sem permissão recebe explicação (não tela de login).
4. Sair pelo Perfil, verificar que os acessos restritos voltam a pedir login e o botão VOLTAR do Android não recupera telas autenticadas.
5. Testar alternância entre contas após saída explícita, conferindo o isolamento do carrinho.

## Limitações
Este repositório de identidade é **local** e serve para protótipo/offline. Não é autenticação de produção: sessões seguras entre aparelhos, proteção contra revogação e armazenamento de tokens exigem backend, HTTPS e gestão de sessão do servidor. Contas desativadas pelo CEO perdem acesso às funções protegidas, mesmo que exista informação de sessão antiga no aparelho.
