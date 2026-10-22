# Melhorias no cadastro de produtos — v1.8

## Interface
- Formulário dividido em informações, preços/rentabilidade e estoque/logística.
- Novo cabeçalho RCAR Premium, cartões escuros, contraste laranja e campos consistentes.
- Campos adicionais: SKU, marca, código de barras, descrição, custo, estoque mínimo, fornecedor, localização e status ativo.
- Prévia atualizada em tempo real para preço, margem bruta estimada e alerta de reposição.

## Comportamento
- Categorias predefinidas para padronizar o catálogo.
- Geração de SKU quando o campo estiver vazio.
- Validação de dados obrigatórios, custo, preço, estoque e código de barras.
- Persistência dos novos campos em armazenamento local com leitura compatível com produtos antigos.
- SKU e código de barras precisam ser únicos quando informados.
- Produtos inativos ficam fora do catálogo de cliente, mas permanecem no estoque administrativo.

## Limitação
O armazenamento continua local ao dispositivo. Para compartilhar o catálogo entre aparelhos, é necessário um backend central.
