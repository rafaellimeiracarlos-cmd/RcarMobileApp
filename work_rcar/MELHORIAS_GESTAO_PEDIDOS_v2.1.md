# RCarPremium v2.1 — Gestão de Pedidos

## Tela de pedidos renovada
- Interface adaptativa para clientes e equipe administrativa, seguindo o tema preto e laranja.
- Indicadores, busca por pedido/cliente/produto e filtros por status.
- Cartões com identificador, data, itens, quantidade, valor e estado do pedido.
- Cliente visualiza somente os pedidos associados ao seu e-mail; notas internas nunca são exibidas nessa área.
- CEO pode editar notas internas e alterar o status dos pedidos.
- Funcionário só recebe acesso à gestão e às ações de edição quando o CEO habilita `gerenciar_pedidos`. A permissão é verificada na abertura da tela e novamente na camada de persistência.
- O checkout confirmado cria um pedido local, associado à conta cliente, com itens, total e forma de pagamento escolhida. Para evitar pedidos órfãos, a finalização exige uma conta cliente ativa.
- Dois pedidos iniciais marcados explicitamente como demonstração preservam o exemplo visto na versão anterior.

## Limitações
Os pedidos, notas e permissões são persistidos localmente neste dispositivo. Para produção, valide todas as regras também no servidor e sincronize os dados através de um backend autenticado. Os pagamentos permanecem em modo de demonstração.
