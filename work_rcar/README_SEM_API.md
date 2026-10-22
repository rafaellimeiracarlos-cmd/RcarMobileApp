# RCarPremium Mobile — modo local sem API externa

Esta versão não depende de uma API do Visual Studio para o fluxo básico de demonstração.

## Melhorias aplicadas

- O login do cliente usa o repositório local de usuários; foi removido o atalho fixo `teste@rcar.com / 1234`.
- O acesso interno consulta o mesmo repositório e verifica o perfil, em vez de aceitar credenciais separadas e iguais para todos.
- A conta de CEO existente é preservada e senhas antigas em texto puro são migradas para hashes PBKDF2 após login válido.
- Novos cadastros exigem senha com pelo menos oito caracteres e e-mail válido.
- O painel do funcionário oculta clientes, configurações, insights executivos, auditoria de sessões e relatórios gerenciais.
- O CEO pode criar, bloquear/reativar e excluir contas de clientes e funcionários e definir permissões por funcionário.
- O CEO tem insights de vendas diárias, ticket médio, estoque, alertas, usuários e acessos recentes.
- As sessões de clientes e funcionários registram início, saída e duração; o painel do CEO atualiza o contador de sessões abertas em tempo real enquanto está aberto.
- Ao confirmar uma compra de demonstração, o sistema valida a disponibilidade, reduz o estoque e registra o faturamento.
- Vendas e relatórios não adicionam mais um valor fictício ao total registrado.
- O cadastro de produtos rejeita preço inválido, estoque negativo e nomes duplicados.
- Arquivos de IDE, configurações locais e saídas de compilação foram excluídos do pacote organizado.

## Limitações

Acesso de demonstração: CEO principal `Rafaellimeiracarlos@gmail.com` / `006365Rafa@`; segundo CEO `renancarlos888@gmail.com` / `00636527`; funcionário `funcionario@rcarpremium.com` / `Funcionario@123`.

O aplicativo continua sendo um protótipo offline: os dados são locais, o pagamento é demonstrativo e não existe sincronização entre dispositivos. PBKDF2 melhora o armazenamento local de senhas, mas não substitui autenticação e autorização no servidor. Para produção, autenticação, perfis, estoque, pedidos e pagamentos devem ser validados por backend confiável.
