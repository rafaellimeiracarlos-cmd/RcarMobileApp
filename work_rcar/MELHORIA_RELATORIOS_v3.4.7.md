# RCarPremium v3.4.7 — Tela de Relatórios profissional

## O que foi melhorado
A antiga tela simples de relatórios foi substituída por um painel mais profissional, com identidade visual preta e laranja e foco em uso executivo pelo CEO.

## Novidades principais

### 1) Visão geral executiva
Agora a tela mostra indicadores no topo:
- faturamento acumulado
- vendas de hoje
- ticket médio do dia
- quantidade de produtos ativos
- quantidade de produtos com estoque baixo

### 2) Abas de relatório
A área agora possui duas visões:
- **Relatório de vendas**
- **Relatório de produtos**

### 3) Relatório de vendas
Inclui:
- resumo dos últimos 7 dias
- vendas recentes
- itens vendidos recentemente
- referência para margem bruta estimada por item quando houver custo cadastrado
- botões rápidos para abrir:
  - painel de vendas
  - tela de margem e lucro

### 4) Relatório de produtos
Inclui:
- visão do catálogo
- valor de estoque
- listagem dos produtos com:
  - nome
  - categoria
  - SKU
  - preço de venda
  - custo
  - estoque
  - margem estimada
  - responsável pelo cadastro
  - status do item/anúncio
- botões por produto para:
  - editar produto
  - abrir estoque

### 5) Restrição de acesso
A tela agora faz validação direta da sessão:
- **somente o CEO pode acessar e editar essa área**
- se outro perfil tentar abrir, o app fecha a tela e exibe aviso

## Arquivos alterados
- `app/src/main/java/com/example/rcarpremium/MainActivityRelatorios.kt`
- `app/src/main/res/layout/activity_relatorios.xml`
- `app/src/main/res/drawable/bg_reports_card.xml`
- `app/src/main/res/drawable/bg_reports_metric.xml`
- `app/src/main/res/drawable/bg_reports_chip_active.xml`
- `app/src/main/res/drawable/bg_reports_chip_inactive.xml`
- `app/src/main/res/drawable/bg_reports_button_primary.xml`
- `app/src/main/res/drawable/bg_reports_button_secondary.xml`
- `app/src/main/res/drawable/bg_reports_badge.xml`

## Observação
A tela usa os dados locais já registrados no aplicativo. Assim, quanto mais produtos, vendas e custos forem cadastrados no app, mais completo o relatório ficará.
