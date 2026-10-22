# RCarPremium v2.3 — Fluxo de equipe e gestão financeira

## Funcionário
- Abra `Painel do Funcionário` > `Meus produtos / ações`.
- Só são listados produtos cujo campo `cadastradoPor` corresponde ao e-mail autenticado. Produtos de versões antigas sem responsável podem ser atribuídos a um funcionário pelo CEO em `Produtos cadastrados / editar`.
- `Editar produto` requer a permissão `cadastrar_produtos`.
- `Registrar venda` requer `registrar_vendas`: valida estoque, baixa quantidade e grava faturamento, produto, custo, quantidade e responsável.
- `Solicitar pausa do anúncio` requer `pausar_anuncios`. O anúncio não é pausado no envio; o CEO deve aprovar na central.
- `Solicitar reposição` requer `solicitar_reposicao_estoque`. O CEO aprova a compra e, quando chega, marca como recebida para acrescentar as unidades ao estoque.

## CEO
- Em `Logins e permissões`, conceda individualmente as permissões de anúncios, vendas e reposição.
- Em `Solicitações da equipe / estoque baixo`, aprove/rejeite pausas e compras e registre recebimentos. A tela apresenta produtos com estoque menor ou igual ao mínimo cadastrado.
- Em `Margem de lucro e vendas`, veja faturamento agregado, vendas detalhadas, custo dos produtos vendidos quando informado e estimativa de lucro/margem.
- Em `Produtos cadastrados / editar`, o CEO pode pausar ou reativar diretamente os anúncios e atribuir/transferir a responsabilidade de um produto a um funcionário ativo.

## Regras e limites
- Pausar anúncio é independente de desativar o produto: o produto permanece no estoque, mas deixa de ser listado para clientes.
- A margem é bruta e estimada; vendas antigas sem linhas detalhadas ou produtos sem custo cadastrado não permitem calcular lucro real.
- O armazenamento atual usa SharedPreferences/JSON local. Solicitações e vendas aparecem para o CEO somente na mesma instalação do app/dispositivo. Para uso em aparelhos separados, publicar pedidos e permissões em backend/Firebase/API, implementar autenticação server-side e sincronização.
- Esta versão não integra fornecedor, transportadora ou banco: uma compra aprovada não é uma ordem enviada automaticamente ao fornecedor, e o registro de venda manual não processa pagamento.
