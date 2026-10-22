# RCarPremium v3.4.4 — Padronização de diálogos e correção de campos brancos

## O que foi corrigido

- Padronização global dos `AlertDialog` para o visual preto/laranja do projeto.
- Correção do problema em que o texto digitado "sumia" em alguns formulários.
- Melhoria visual da tela de atualização de entrega/rastreio dos pedidos.
- Revisão de formulários e telas com diálogo branco para deixá-los mais profissionais.

## Melhorias aplicadas

### Tema global de diálogos
- Novo tema para dialogs com:
  - fundo escuro;
  - borda laranja;
  - títulos brancos;
  - texto secundário cinza claro;
  - botões coerentes com a identidade do app.

### Formulários com campos melhores
- Criação do helper `RcarDialogUi.kt` para padronizar:
  - campos de entrada;
  - fundo dos formulários;
  - botões dos dialogs;
  - aparência das janelas.

### Telas revisadas
- `MainActivityPedidos.kt`
  - formulário de entrega/rastreio redesenhado;
  - melhor contraste;
  - correção da digitação invisível.
- `MainActivityCentralNotas.kt`
  - formulário de vincular nota fiscal de venda melhorado.
- `MainActivityLojasParceiras.kt`
  - criação/edição de loja parceira melhorada;
  - configuração de backend melhorada.
- `MainActivityAssistenteCompra.kt`
  - diálogo de autenticação remota melhorado.

## Arquivos criados/alterados
- `app/src/main/java/com/example/rcarpremium/RcarDialogUi.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivityPedidos.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivityCentralNotas.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivityLojasParceiras.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivityAssistenteCompra.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/drawable/bg_rcar_dialog_window.xml`

## Resultado esperado
- Os formulários não devem mais abrir com aspecto branco fora do padrão.
- Os textos digitados devem permanecer visíveis.
- A experiência fica mais profissional e consistente com o restante do projeto.
