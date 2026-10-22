# RCarPremium v2.4 — tela de detalhes de produto

## Interface atualizada
- Paleta carvão/preto com laranja RCarPremium.
- Logo horizontal recortada a partir da logo original do projeto no cabeçalho.
- Busca e banner de ofertas no topo.
- Imagem de produto, título e selo de categoria.
- Cartão de preço com desconto real quando houver oferta cadastrada.
- Informação de frete sem inventar cotação; orientação para consultar no checkout.
- Seletor de quantidade limitado ao estoque conhecido.
- Total selecionado e simulação de parcelamento.
- Abas funcionais de descrição, especificações e avaliações (avaliações vazias são explicitadas).
- Carrossel de produtos relacionados lido de AdminLocalStore, somente com produtos ativos, anúncios ativos e unidades em estoque.
- Acesso ao detalhe de produto relacionado ao tocar em um cartão.
- Botão Voltar ao topo após os produtos relacionados.
- Rodapé fixo com Adicionar, Comprar agora e navegação de Início/Categorias/Carrinho/Pedidos/Perfil.
- Preços promocionais aplicados ao carrinho quando o item tiver desconto configurado.
- O detalhe não recebe a sobreposição de chips genéricos de voltar da classe base para evitar cobrir os botões fixos.

## Nota
O estoque, ofertas, catálogo e carrinho continuam usando os repositórios locais existentes. Avaliações e imagens específicas por produto dependem de dados/recursos que não existem para todos os produtos; a tela usa a imagem automotiva existente como fallback. Não foi validada uma compilação Android completa neste ambiente.
