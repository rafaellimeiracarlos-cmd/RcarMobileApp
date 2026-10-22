# RCarPremium v3.4.6 — Tela Clientes profissional

## O que foi melhorado

A antiga tela de clientes foi substituída por uma versão mais profissional e administrativa.

### Melhorias visuais
- Cabeçalho mais moderno
- Cartões escuros com borda laranja
- Busca por nome, e-mail, CPF ou telefone
- Indicadores de total de clientes e cadastros completos
- Lista com cards mais elegantes e legíveis

### Melhorias funcionais
- A tela agora usa os clientes reais cadastrados no aplicativo
- Ao tocar em um cliente, abre a **ficha completa** com:
  - nome
  - e-mail
  - CPF
  - RG
  - telefone
  - endereço
  - número
  - bairro
  - cidade / UF
  - CEP
  - complemento
  - observações
  - data de cadastro
- Exibe também um resumo do cliente:
  - quantidade de pedidos
  - valor total comprado
  - último pedido registrado
  - status da conta
  - nível de cadastro (completo/incompleto)

### Edição rápida
- O CEO pode editar os dados complementares do cliente direto pela tela
- Os dados são salvos localmente no armazenamento do app

## Arquivos alterados
- `app/src/main/java/com/example/rcarpremium/MainActivityClientes.kt`
- `app/src/main/java/com/example/rcarpremium/UsuarioLocalRepository.kt`
- `app/src/main/res/layout/activity_clientes.xml`
- `app/src/main/res/drawable/bg_clientes_stat_card.xml`
- `app/src/main/res/drawable/bg_clientes_search.xml`

## Observação
Se ainda não houver clientes cadastrados no app, a tela mostrará um estado vazio informando isso. Assim que os clientes criarem conta, passarão a aparecer automaticamente nessa área.
