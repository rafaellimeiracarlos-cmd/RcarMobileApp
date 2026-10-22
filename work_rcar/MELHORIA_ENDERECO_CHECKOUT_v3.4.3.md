# RCarPremium v3.4.3 — Melhoria da tela de endereço no checkout

## O que foi melhorado

- Bloco de endereço do checkout com visual mais profissional.
- Resumo do endereço dentro de um cartão interno.
- Botão com texto dinâmico: adicionar, selecionar ou editar endereço.
- Novo formulário escuro para endereço de compra em modo de teste do CEO.
- Campos adicionados: nome do recebedor, CEP, rua, número, bairro, cidade, UF e complemento.
- Validações básicas antes de salvar o endereço.
- Reabertura do formulário com preenchimento automático quando já existir endereço salvo.

## Arquivos alterados

- `app/src/main/java/com/example/rcarpremium/MainActivityPagamento.kt`
- `app/src/main/res/layout/activity_main_pagamento.xml`
- `app/src/main/res/layout/dialog_endereco_checkout_teste.xml`
- `app/src/main/res/drawable/bg_checkout_field_dark.xml`
- `app/src/main/res/drawable/bg_checkout_dialog_panel.xml`
- `app/src/main/res/drawable/bg_checkout_dialog_highlight.xml`
- `app/src/main/res/drawable/bg_checkout_address_warning.xml`
