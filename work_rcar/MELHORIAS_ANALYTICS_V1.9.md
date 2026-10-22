# RCarPremium v1.9 — Insights e análise do funil de compra

## O que mudou

- Painel de Insights do CEO reorganizado em cartões de indicadores e gráfico de barras de faturamento diário dos últimos 7 dias.
- Tela Vendas reformulada com KPIs, gráfico de linhas das últimas 24 horas, etapas do funil e listas de jornadas e vendas recentes.
- Gráfico do funil com três séries: carrinhos iniciados, compras confirmadas e carrinhos abandonados.
- Atualização automática da tela Vendas a cada 5 segundos e do painel executivo a cada 10 segundos.
- Registro local da jornada quando o carrinho passa a conter produtos; registro de checkout quando o cliente avança para pagamento; conversão após a confirmação da venda local.
- Um carrinho passa a ser classificado como abandonado após 30 minutos sem atividade.
- Identidade do cliente é exibida no histórico de jornadas somente para o CEO; funcionários com permissão de vendas veem o rótulo genérico “Cliente”.
- Não foi acrescentada biblioteca externa de gráficos; os gráficos usam Canvas nativo do Android.

## Limitações importantes

Esta edição continua offline. Os eventos e métricas são guardados em `SharedPreferences` no próprio aparelho. O gráfico reflete os eventos que o app registra nesse dispositivo, não uma visão simultânea de todos os clientes que usam outros celulares. Para análise verdadeiramente multiusuário e tempo real entre dispositivos, será necessário um backend autenticado com banco de dados e eventos enviados pelo aplicativo.

O abandono é uma estimativa baseada em 30 minutos sem atividade, não uma confirmação de intenção do cliente. Compras de demonstração só são registradas quando o fluxo local confirma a venda.

## Teste recomendado

1. Abra a tela Vendas com a conta CEO ou com uma conta autorizada para `ver_vendas`.
2. Em outra navegação do app, adicione um item ao carrinho e confira o contador/linha “Carrinhos iniciados”.
3. Avance para pagamento sem concluir; a jornada aparece como checkout iniciado.
4. Aguarde 30 minutos sem atividade para que a jornada seja classificada como abandonada.
5. Conclua uma compra de demonstração; ela aparece na série de compras e no histórico de vendas.
