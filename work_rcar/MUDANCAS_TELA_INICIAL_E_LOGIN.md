# Melhorias da tela inicial e dos logins — RCAR Premium

## Corrigido
- O cartão de ofertas estava usando indevidamente o ID `btnAcessoInterno`, que era ligado ao diálogo de acesso do funcionário/CEO. O cartão agora possui o ID próprio `cardOfertasSemana`.
- O banner e o botão `VER OFERTAS` abrem a listagem de produtos no modo “Ofertas da semana”.
- O menu superior passou a separar categorias, login do funcionário e login do CEO.

## Melhorias visuais e funcionais
- Layout do login interno reorganizado em cartão escuro, campos com contraste, selo de perfil e botão específico para CEO/funcionário.
- Login do cliente alinhado à identidade visual escura com detalhes laranja e tamanho de logo responsivo. Os botões sociais foram ocultados por ainda não terem autenticação Google/Facebook real conectada.
- Tela de listagem com título/subtítulo e mensagem de ausência de resultados na pesquisa.
- A pesquisa da tela inicial procura no catálogo local em vez de abrir um detalhe de produto com preço genérico.
- Botões dos produtos em destaque adicionam itens correspondentes ao catálogo local ao carrinho.
- Selos de destaque substituem percentuais de desconto que não tinham preço promocional calculado.

## Como testar no Android Studio
1. Extraia o ZIP e abra a pasta `work_rcar`.
2. Execute **File → Sync Project with Gradle Files**.
3. Rode o app no emulador.
4. Na tela inicial, toque em **VER OFERTAS** e no cartão **OFERTAS DA SEMANA**; ambos devem abrir a lista de produtos.
5. Toque no menu do topo para acessar categorias ou os logins do funcionário e do CEO.
6. Abra o login de cliente e o login interno para verificar campos e botões.

## Limitações desta validação
Os 50 XMLs de recursos foram validados sintaticamente. A compilação Android não pôde ser concluída neste ambiente porque o Gradle Wrapper tentou baixar Gradle 9.4.1 e a rede estava indisponível (`UnknownHostException`). A interface e os fluxos devem ser executados no Android Studio para confirmação final. Os dados de catálogo, contas e sessões continuam no armazenamento local, sem sincronização entre dispositivos.
