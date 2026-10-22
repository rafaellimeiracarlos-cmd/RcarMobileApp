## Atualização v3.2 — Importação por nota fiscal

Abra o painel interno e escolha **Importar NF-e • XML / QR / Link**. Veja [IMPORTACAO_NOTAS_FISCAIS_v3.2.md](IMPORTACAO_NOTAS_FISCAIS_v3.2.md) para passos, permissões e limitações.

> **NOVO v3.1:** [Busca de peças por placa Mercosul — instruções e limitações](BUSCA_POR_PLACA_MERCOSUL_v3.1.md).

# RCarPremium Mobile

Aplicativo Android nativo para catálogo de autopeças e operações administrativas da RCarPremium.

## Tecnologias

- Kotlin e Android SDK (minSdk 24; targetSdk 35)
- Layouts XML com View Binding habilitado
- Gradle Kotlin DSL e Version Catalog (`gradle/libs.versions.toml`)
- SharedPreferences/JSON para persistência local de demonstração

## Abrir e executar

1. Extraia o projeto e abra **a pasta `work_rcar`** no Android Studio (a pasta que contém `settings.gradle.kts`).
2. Configure o Android SDK exigido por `compileSdk` e permita que o Android Studio sincronize o Gradle.
3. Se necessário, ajuste o caminho do SDK no Android Studio. `local.properties` é uma configuração local e não deve ser distribuída.
4. Selecione um emulador ou dispositivo Android e execute a configuração `app`.
5. Para gerar um APK de depuração, use **Build > Build APK(s)** ou `./gradlew assembleDebug` em um ambiente com acesso à distribuição Gradle e ao Android SDK.

## Áreas principais

| Área | Arquivos de referência |
|---|---|
| Entrada e navegação inicial | `MainActivitycomprasonline.kt`, `BottomNav.kt` |
| Login e cadastro do cliente | `MainActivity.kt`, `MainActivityCadastro.kt`, `UsuarioLocalRepository.kt` |
| Carrinho e pagamento | `MainActivityItemCarrinho.kt`, `MainActivityPagamento.kt`, `RcarLocalStore.kt` |
| Acesso interno | `MainActivityAcessoInterno.kt`, `MainActivityPainelInterno.kt` |
| Produtos e estoque | `MainActivityCadastroProduto.kt`, `MainActivityListaProdutos.kt`, `MainActivityEstoque.kt`, `AdminLocalStore.kt` |
| Vendas e relatórios | `MainActivityVendas.kt`, `MainActivityRelatorios.kt` |
| Interfaces | `app/src/main/res/layout/` |
| Imagens, cores e temas | `app/src/main/res/drawable/`, `app/src/main/res/values/` |
| Declaração das Activities | `app/src/main/AndroidManifest.xml` |

## Acessos de demonstração (modo local)

- **CEO principal:** `Rafaellimeiracarlos@gmail.com` / `006365Rafa@`
- **CEO adicional:** `renancarlos888@gmail.com` / `00636527`
- **Funcionário de demonstração:** `funcionario@rcarpremium.com` / `Funcionario@123`
- **Cliente:** cadastre pela tela de cadastro ou crie um acesso na gestão de logins do CEO.

As contas internas são criadas localmente no primeiro uso. Não reutilize essas senhas em serviços externos; credenciais de demonstração não são adequadas para produção.

## Armazenamento e limitações

- Catálogo, carrinho, usuários e dados administrativos são armazenados no próprio dispositivo.
- Os dados não são sincronizados entre dispositivos nem entre instalações diferentes.
- A autenticação local e a restrição de telas por perfil são úteis para demonstração, mas não substituem autorização no servidor. Um aplicativo comercial precisa de backend, autenticação robusta e validação de permissões no servidor.
- A tela de pagamento é demonstrativa; ela não processa pagamentos reais. Não informe dados reais de cartão.
- Os botões de login social apenas exibem mensagens enquanto não houver integração oficial.

## Organização e manutenção

- Kotlin: `app/src/main/java/com/example/rcarpremium/`.
- Layouts XML: `app/src/main/res/layout/`; recursos visuais: `drawable/` e `values/`.
- Não edite arquivos gerados em `app/build/`; o Gradle os recria.
- Não versionar `.idea/`, `local.properties`, `.gradle/`, `build/` nem artefatos temporários.
- Após alterações, sincronize o Gradle e teste login, cadastro, carrinho, pagamento, estoque e permissões.


## Administração avançada (CEO)

O painel interno inclui insights, gestão de usuários e permissões, e auditoria local de sessões. No painel do CEO, use **Insights executivos**, **Logins e permissões** e **Sessões e tempo de acesso**. O painel de insights mostra faturamento e quantidade de vendas do dia, ticket médio, acumulado, valor e quantidade do estoque, alertas, usuários, funcionários ativos, vendas recentes e acessos recentes. Funcionários têm acesso somente às operações autorizadas pelo CEO. O estoque permite entradas e saídas rápidas por produto e alerta itens com cinco unidades ou menos.

**Atenção:** esta versão é offline e demonstrativa. Usuários, permissões, sessões, vendas e estoque ficam no armazenamento local do dispositivo; não há sincronização entre dispositivos. Para uso comercial, implemente backend e autorização no servidor. O tempo de sessão é recalculado ao atualizar a tela de auditoria.


## Correção de acesso do CEO (versão 1.1)

Esta versão inclui uma migração única do armazenamento local para corrigir contas de CEO que foram salvas em versões anteriores com papel ou senha antiga. Na primeira abertura da versão 1.1, as duas contas de CEO de demonstração abaixo são restauradas. A migração é executada apenas uma vez e não redefine as senhas em cada abertura.

- `rafaellimeiracarlos@gmail.com` / `006365Rafa@`
- `renancarlos888@gmail.com` / `00636527`

A migração altera apenas os registros dessas duas contas no armazenamento local de usuários; não apaga produtos ou registros de vendas guardados em outras preferências. Depois de validar o acesso, altere as credenciais de demonstração antes de qualquer uso real.


## Auditoria de acessos — v2.2

A tela de auditoria recebeu um painel com indicadores (registros, sessões abertas, acessos de clientes no dia e acessos da equipe), busca por e-mail/perfil, filtros por tipo de sessão, cartões detalhados com entrada/saída e duração atualizada ao vivo. A tela continua restrita ao CEO. Os dados são registrados localmente no dispositivo e não substituem uma auditoria central em servidor.


## Operações de equipe e margem — v2.3

O painel do funcionário inclui **Meus produtos / ações** para listar produtos cadastrados pelo próprio usuário, editar produtos quando autorizado, registrar venda, solicitar pausa de anúncio com permissão concedida pelo CEO e solicitar reposição. O CEO processa pedidos em **Solicitações da equipe / estoque baixo** e consulta desempenho em **Margem de lucro e vendas**. As novas permissões individuais ficam em **Logins e permissões**. Detalhes: `MELHORIAS_EQUIPE_ESTOQUE_MARGEM_v2.3.md`.

A gestão continua offline: os dados não são sincronizados entre aparelhos. Para operar com CEO e funcionários em dispositivos distintos, é necessário integrar backend e autorização no servidor. A margem é estimada e depende de custo de compra corretamente cadastrado; não representa lucro líquido nem substitui contabilidade.

## Marketplace v3.0
Consulte **ARQUITETURA_MARKETPLACE_V3.md** para instruções de instalação, execução do backend e a distinção entre funções locais, integração HTTPS opcional e pendências de produção.
