# RCarPremium v3.1 — Busca de peças por placa Mercosul

## Novidades

- Tela **Busca pela placa**, acessível pela home, marketplace e painel interno.
- Aceita os formatos `ABC1D23` (Mercosul) e `ABC1234`/`ABC-1234` (antigo).
- Validação apenas do **formato**, não da existência da placa, propriedade ou autenticidade do veículo.
- Consulta opcional a backend HTTPS com um **provedor licenciado**; é necessário configurar backend e autenticar-se nele.
- Busca manual por marca, modelo, ano e motor, disponível mesmo sem API de placas.
- O CEO pode cadastrar por SKU as compatibilidades confirmadas: **marca, modelo, ano inicial, ano final e motor**.
- Mostra somente produtos ativos, com anúncio ativo, estoque positivo e compatibilidade cadastrada que corresponda exatamente ao carro.
- Botão separado para ver todo o catálogo **sem garantia de compatibilidade**.
- Com consentimento explícito, guarda no aparelho um vínculo local com a placa usando um hash SHA-256 com sal específico da instalação. **A placa em texto puro não é guardada na base local.** Não há sincronização entre aparelhos.

## Uso pelo CEO (necessário antes de encontrar peças)

1. Faça login como CEO.
2. No **Painel do CEO**, abra **Compatibilidade por carro • CEO**.
3. Selecione o produto/SKU e informe os dados de aplicação técnica, por exemplo: `Volkswagen | Gol | 2019–2022 | 1.0 MPI`.
4. Salve a associação. Para cada versão adicional do veículo, cadastre outra aplicação.
5. Repita por produto. É possível remover associações incorretas na mesma tela.

**Não cadastre compatibilidades apenas pelo nome comercial do produto.** Use número de peça/OEM e catálogo técnico. Não existem associações inventadas pré-carregadas.

## Uso pelo cliente ou funcionário

1. Na loja, escolha **Buscar peças pela placa Mercosul**.
2. Digite a placa (sem espaço). Se ela já tiver sido vinculada *por você* neste aparelho, o veículo informado anteriormente será mostrado, marcado como não verificado.
3. Se houver uma conexão a serviço autorizado, o sistema tentará consultar o veículo pelo backend após autenticação.
4. Caso contrário, preencha **marca, modelo, ano e motor**. É possível fazer isso mesmo sem login.
5. Confira os dados e toque em **Buscar peças compatíveis**.
6. Toque em uma peça para abrir seus detalhes e seguir o fluxo de compra normal.
7. Se desejar, marque **Lembrar este veículo neste aparelho** antes de pesquisar. A opção **Esquecer placa salva** apaga o vínculo local.

## Como ativar consulta automática de placas

O aplicativo **não pode inferir o veículo pela placa por conta própria**. A Senatran oferece um serviço oficial para dados de placa, mas exige **placa e número de série do QR Code**; não se trata de uma API pública genérica de busca por sete caracteres.

A consulta automática adicionada em `backend/app.py` é **uma integração preparada**, não um provedor contratado. Para ativá-la:

1. Contrate um fornecedor de dados veiculares com finalidade permitida de identificação de veículos e direito de uso. Verifique também origem, base legal, termos contratuais e LGPD.
2. Configure em um servidor RCarPremium HTTPS existente:

```bash
export RCAR_PLATE_PROVIDER_URL='https://SEU-PROVEDOR-AUTORIZADO/exemplo/consulta'
export RCAR_PLATE_PROVIDER_TOKEN='TOKEN_SECRETO_SO_NO_SERVIDOR'
```

3. O endpoint configurado deve receber **POST** com JSON `{ "plate": "ABC1D23" }` e responder exatamente aos campos técnicos:

```json
{"brand":"Volkswagen", "model":"Gol", "year":2020, "engine":"1.0 MPI"}
```

4. Configure o endereço HTTPS do backend na tela **Lojas parceiras / servidor IA**, como já previsto no projeto.
5. Na busca por placa, escolha **Conectar consulta autorizada** e faça login com uma conta do **backend** RCarPremium (o login local Android não substitui essa conta de API). Depois use **Consultar placa**.

Se o fornecedor tiver contrato ou esquema diferente, adapte o gateway do servidor; **nunca inclua a chave da API no APK**. O endpoint `/v1/vehicles/lookup` exige autenticação, limita 8 consultas por hora e entrega apenas marca/modelo/ano/motor; não grava placas e não retorna dono, CPF, endereço, chassi ou Renavam. O limite de produção também deve incluir bloqueio por IP, monitoramento, antifraude e retenção mínima adequada.

## Arquivos principais

- `app/src/main/java/com/example/rcarpremium/MainActivityBuscaPlaca.kt`
- `app/src/main/java/com/example/rcarpremium/MainActivityCompatibilidadeCeo.kt`
- `app/src/main/java/com/example/rcarpremium/VehicleFitmentStore.kt`
- `app/src/main/java/com/example/rcarpremium/PlateRules.kt`
- `app/src/main/java/com/example/rcarpremium/MarketplaceRemoteApi.kt`
- `backend/app.py` com novo endpoint `/v1/vehicles/lookup`.
- `backend/tests/test_plate_lookup.py`

## Testes realizados e limites

- Os testes Python do backend passaram: `4 passed` (incluindo testes do novo endpoint).
- Validação isolada Kotlin executada para as placas Mercosul/antiga e correspondência de compatibilidade.
- Os arquivos XML do Android foram validados estruturalmente.
- **A compilação Android não foi concluída**: o Gradle Wrapper tenta obter `gradle-9.4.1-bin.zip`, mas o ambiente atual não resolve `services.gradle.org`. Teste com `Build > Rebuild Project` no seu Android Studio.
- Os cadastros e vínculos de compatibilidade estão em armazenamento local; sincronização entre vendedores/dispositivos requer backend e banco de dados central.
- Antes de venda efetiva, confirme a aplicação com dados completos do veículo e fabricante da peça. A interface não deve garantir compatibilidade legal ou mecânica só com a placa.
