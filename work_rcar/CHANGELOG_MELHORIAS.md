# Registro de melhorias

## Organização

- Documentação principal adicionada em `README.md`, com instruções de abertura, mapa das áreas e limitações.
- `.gitignore` atualizado para excluir arquivos de IDE, configuração local e saídas de compilação.
- Arquivos gerados e configurações específicas do computador removidos do pacote de entrega.
- `AdminLocalStore.kt`, `MainActivityCadastroProduto.kt`, `MainActivityVendas.kt` e `MainActivityRelatorios.kt` formatados para facilitar manutenção.

## Login e permissões

- Login do cliente passou a usar o repositório de usuários, removendo a conta de teste embutida no formulário.
- Acesso interno valida credenciais e perfil pelo repositório comum.
- Contas de demonstração separadas para CEO e funcionário.
- Novas senhas de clientes são armazenadas com PBKDF2 e salt aleatório; senhas antigas em texto puro são migradas após autenticação válida.
- Novos cadastros exigem e-mail válido e senha de pelo menos oito caracteres.
- Funcionário não vê as telas de clientes, configurações e relatórios gerenciais.
- A recuperação de senha informa corretamente que não envia e-mail nem redefine senha enquanto o app estiver offline.

## Produtos e relatórios

- Validação de preço positivo, estoque não negativo e nome de produto não duplicado.
- Os totais de vendas e relatórios deixaram de somar um valor fictício.
- Total de vendas passa a ser persistido como texto numérico para evitar a perda de precisão do tipo `Float`, preservando leitura de valores antigos.

## Validação

A estrutura e as alterações foram revisadas estaticamente. A compilação Gradle não pôde ser concluída neste ambiente porque o wrapper precisa baixar Gradle de `services.gradle.org` e a conexão de rede não está disponível. Abra no Android Studio com Android SDK e Gradle acessíveis e execute uma compilação e testes em dispositivo antes de distribuir.


## Administração avançada
- Painel de insights executivos para CEO, vendas do dia, valor estimado de estoque e alertas.
- Gestão de contas e permissões por funcionário.
- Histórico local de sessões com duração calculada.
- Ajustes de estoque (+1, +10, -1) e proteção de estoque negativo.
- Botão e permissão para gerenciamento de pedidos.


## Refinamentos finais da administração
- Contas de CEO solicitadas adicionadas: `Rafaellimeiracarlos@gmail.com` e `renancarlos888@gmail.com`, com criação apenas quando a conta ainda não existe.
- Painel executivo ampliado com número de vendas do dia, ticket médio, unidades em estoque, funcionários ativos e vendas recentes.
- Checkout de demonstração agora verifica disponibilidade e baixa o estoque antes de registrar a venda.
- Auditoria de sessões mostra duração em atualização contínua enquanto aberta.
- Revisão estática adicional: todos os 47 XMLs foram analisados; a compilação ainda depende de baixar a distribuição Gradle, bloqueada pela indisponibilidade de rede deste ambiente.


## Ajustes de navegação e identidade visual (2026-10-21)
- Corrigido o ID do banner de ofertas, que estava compartilhado indevidamente com o acesso interno.
- Banner e botão “Ver ofertas” agora abrem a listagem de produtos no modo Ofertas da semana.
- Menu superior reorganizado para separar categorias, login do funcionário e login do CEO.
- Atualizados os layouts de login interno e login do cliente para uma identidade dark premium com detalhes laranja e campos legíveis.
- Removidos indicadores de avaliações simuladas e substituídos os descontos sem preço de referência por selos de destaque.
- Botões dos produtos em destaque agora adicionam os produtos do catálogo local ao carrinho.

## Tela de Categorias — atualização visual
- Redesenhada a tela de categorias com cabeçalho, banner de catálogo, pesquisa estilizada e cartões em duas colunas.
- Cada categoria agora apresenta ícone, descrição, contagem de produtos correspondentes e acesso por toque.
- A busca filtra nomes e palavras-chave; resultados vazios têm uma mensagem própria.
- A listagem de produtos usa palavras-chave por categoria para que categorias como Freios, Suspensão e Óleos encontrem produtos cujo cadastro ainda usa a categoria genérica Automotivo.

## Padronização de código — versão de formatação

- Reformatados 51 arquivos XML do Android (layouts, drawables, regras XML e manifesto) com indentação de quatro espaços, atributos em linhas próprias e separação visual entre componentes.
- Preservados os atributos, valores, comentários e a estrutura funcional dos XMLs.
- Normalizadas quebras de linha e espaços finais dos arquivos Kotlin/Gradle.
- Reorganizadas as classes `MainActivityClientes`, `MainActivitySobre` e `MainActivityTermos` para melhorar a leitura.
- Adicionado `.editorconfig` para manter o padrão de indentação e `PADRAO_DE_FORMATACAO.md` com instruções de uso no Android Studio.

## v1.9 — Insights, gráficos e funil de conversão
- Novo gráfico de faturamento dos últimos 7 dias no painel do CEO.
- Tela de Vendas redesenhada com KPIs, gráfico de linhas das últimas 24 horas e atualização automática a cada 5 segundos.
- Monitoramento local de jornada: carrinho iniciado, checkout iniciado, compra concluída e abandono após 30 minutos sem atividade.
- Feed de atividade de carrinhos e histórico de vendas recentes.
- Gráficos implementados com Canvas nativo, sem dependências externas adicionais.


## v2.1 — Gestão de Pedidos
- Tela Meus Pedidos reformulada, com cartões, indicadores, filtros e acompanhamento de status.
- Painel administrativo de pedidos com notas internas e atualização de status.
- Acesso do funcionário condicionado à permissão `gerenciar_pedidos` definida pelo CEO.
- Pedidos reais de checkout passam a aparecer no histórico local do cliente e no painel administrativo.

## v2.3 — Operações do funcionário, anúncios, compras e margem
- Produtos novos agora guardam o e-mail responsável pelo cadastro e um estado de anúncio separado do estado do produto.
- Nova área do funcionário: meus produtos, edição dos próprios cadastros, registro de venda com baixa de estoque, solicitação de pausa de anúncio e solicitação de reposição.
- Pausas de anúncio passam a depender da permissão `pausar_anuncios` dada pelo CEO e da aprovação final do CEO; o anúncio é ocultado do catálogo quando aprovado, sem retirar o produto do estoque.
- Novas permissões por funcionário: `pausar_anuncios`, `registrar_vendas` e `solicitar_reposicao_estoque`.
- Nova central do CEO para aprovar/rejeitar pausas e compras, registrar recebimentos, e acompanhar itens em estoque baixo.
- O CEO pode atribuir ou transferir a responsabilidade dos produtos, inclusive cadastros antigos sem responsável associado.
- Nova tela financeira do CEO com faturamento registrado, vendas do dia, vendas detalhadas, custo conhecido e margem bruta estimada por produto.
- Vendas no checkout e vendas manuais da equipe passam a registrar itens, quantidades, preço, custo unitário e responsável quando os dados estão disponíveis.
- Produtos com anúncio pausado deixam de aparecer nas listagens de catálogo/ofertas e os destaques relevantes da tela inicial são ocultados.
- Todos os dados de equipe, solicitações, estoque e vendas continuam em armazenamento local/offline; não existe sincronização entre aparelhos.
- Validação: revisão estática de código concluída. A compilação Gradle não pôde ser executada neste ambiente porque o wrapper tenta baixar Gradle de `services.gradle.org` e a rede está indisponível.

## v2.4 — Tela de detalhe do produto e rodapé profissional
- Reformulada `MainActivityVerProdutos` com identidade visual preta e laranja RCarPremium e a logo original.
- Cabeçalho com busca, banner de ofertas e atalho de carrinho.
- Seletor de quantidade, preço promocional de oferta cadastrada, parcelamento e total selecionado.
- Abas funcionais de descrição, especificações e avaliações.
- Carrossel horizontal dinâmico de produtos relacionados disponíveis no catálogo local.
- Botões fixos de adicionar ao carrinho/comprar agora e rodapé de navegação da loja.
- Botão Voltar ao topo e exclusão da sobreposição genérica de voltar nesta tela para evitar colisão com os controles fixos.
- Versão do app atualizada para 2.4 (versionCode 9).
