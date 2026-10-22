# RCarPremium v3.0.2 — Voltar ao topo da página inicial

## Alterações
- Removidos os dois chips automáticos de **Voltar** somente da tela inicial da loja, sem modificar os fluxos de outras telas.
- Incluído o botão **↑ VOLTAR AO TOPO** ao final do conteúdo rolável, acima do rodapé de navegação flutuante.
- O botão aparece naturalmente apenas quando a pessoa percorre a página até o fim; não se sobrepõe às fotos, preços ou botões de compra.
- Ao tocar, a página retorna suavemente ao cabeçalho.
- Preservadas a logo, as cores preto e laranja, o marketplace, o catálogo e as funcionalidades de login e compra.

## Arquivos alterados
- `app/src/main/java/com/example/rcarpremium/RcarBaseActivity.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivitycomprasonline.kt`
- `app/src/main/res/layout/activity_main_activitycomprasonline.xml`
- `app/src/main/res/drawable/bg_voltar_ao_topo.xml` (novo)

## Teste no Android Studio
1. Abra a pasta `work_rcar` como projeto.
2. Sincronize o Gradle e execute **Build > Make Project**.
3. Entre na **tela inicial da loja** e verifique que não há chips “Voltar” sobre a interface.
4. Role até o final, abaixo de **Produtos em destaque**, e toque em **↑ VOLTAR AO TOPO**.
5. Verifique o retorno suave ao cabeçalho e que o rodapé flutuante continua funcionando.

> Foram realizadas verificações estáticas de arquivos XML e de vinculação de IDs. Não foi executada uma compilação Android completa neste ambiente sem SDK/Gradle configurados.
