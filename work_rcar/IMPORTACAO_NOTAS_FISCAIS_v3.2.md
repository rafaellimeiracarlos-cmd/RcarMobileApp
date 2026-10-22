# RCarPremium v3.2 — Entrada de produtos por nota fiscal

## O que foi implementado

- Tela **Importar NF-e • XML / QR / Link** no painel do CEO e de funcionários com permissão `cadastrar_produtos`.
- Escolha de documento XML (NF-e modelo 55 ou NFC-e modelo 65, estrutura `NFe`/`nfeProc`).
- Leitor de QR Code/ código de barras pelo **Google Code Scanner**; requer Google Play Services e instalação do módulo do scanner. Não exige permissão direta de câmera.
- Download de URL **HTTPS direta** para um arquivo XML (limite 4 MB, sem redirecionamentos).
- Leitura de `emit/xNome`, `emit/CNPJ`, `ide/nNF`, `det/prod/cProd`, `xProd`, `cEAN`, `uCom`, `qCom`, `vProd`, `NCM` e `CFOP`.
- Exibição de custo unitário estimado de mercadoria como `vProd/qCom` (não inclui impostos, desconto rateado, frete ou despesas acessórias). Por isso deve ser conferido antes de lançar custos contábeis.
- Revisão individual de itens com nome, categoria, preço de venda e quantidade, incluindo vínculo manual com o estoque existente.
- Entradas em estoque de produtos existentes sem alterar preço de venda nem status de anúncio. Custo unitário atualizado pelo último custo obtido no XML.
- Produtos novos com `anuncioAtivo=false` até o CEO revisar o cadastro.
- Registro local do ID da nota (chave, se presente) ou hash SHA-256 e da operação, com bloqueio de segunda importação no mesmo aparelho.
- Alteração conjunta do cadastro e histórico em uma única gravação `SharedPreferences.commit()`, evitando entradas parcialmente gravadas dentro do armazenamento local.

## Como usar

1. Entre como CEO ou funcionário autorizado. Abra **Painel interno → Importar NF-e • XML / QR / Link**.
2. Escolha o arquivo XML da nota, leia um QR Code ou cole a URL HTTPS direta do XML.
3. Confirme os dados da nota e do fornecedor.
4. Revise os itens. Para cada item, escolha **vincular produto existente** ou **criar produto novo**; ajuste o preço de venda e a quantidade. Itens com quantidade fracionada ou unidade fiscal não unitária ficam desmarcados inicialmente.
5. Toque em **Conferir e adicionar ao estoque** e confirme o resumo.
6. Veja os produtos em **Produtos cadastrados / editar**. Ative os anúncios novos apenas após revisar os dados.

## Limitações importantes

- **DANFE/PDF:** o PDF é reconhecido na seleção, mas NÃO é convertido automaticamente em itens. Faça download do XML com o emissor ou por serviço fiscal autorizado. Scanner de QR Code de DANFE geralmente retorna uma **página de consulta pública**, não o arquivo XML.
- **Chave da NF-e:** os 44 dígitos não permitem por si só baixar dados estruturados da nota sem o acesso ou a autorização exigidos pelo provedor.
- **Validade fiscal:** a importação lê o `cStat` do XML quando presente, mas não verifica assinatura digital, status de autorização atual, eventos de cancelamento ou devoluções no SEFAZ. Confira a nota e os dados do fornecedor antes de aceitar a entrada.
- **Custo:** `vProd/qCom` é custo comercial unitário aproximado. Não é cálculo completo de custo fiscal/contábil.
- **Estoque:** o modelo atual armazena estoque como número inteiro; conversões KG/LT/CX devem ser validadas manualmente.
- **Sincronização:** histórico e produtos ficam no armazenamento local de cada aparelho. Para múltiplos funcionários em dispositivos diferentes, precisa de API e banco transacional no backend.
- **Permissão e segurança:** a conferência de papéis é no app local, apropriada apenas para versão demonstrativa. Produção exige verificação de permissões no backend.
- **Dependência nova:** `com.google.android.gms:play-services-code-scanner:16.1.0`; na primeira sincronização Gradle e uso do scanner precisa de acesso à internet.
- **Tamanho:** máximo 4 MB de XML e 400 itens por nota para reduzir risco de travamento.

## Verificações manuais recomendadas

1. Importar uma NF-e XML válida contendo dois itens inteiros.
2. Reimportar a mesma nota e confirmar bloqueio de duplicação.
3. Importar nota de fornecedor com EAN já existente e confirmar apenas acréscimo de estoque.
4. Testar uma nota com quantidade fracionada (KG/LT) e confirmar que a linha fica desmarcada.
5. Tentar importar HTML de URL de consulta pública e verificar mensagem de erro.
6. Importar uma nota com status `cStat=101` e confirmar o bloqueio.
7. Testar login de cliente ou funcionário sem permissão e confirmar que a importação não abre.
8. Confirmar que anúncios novos não aparecem na loja até aprovação manual.

> Importação não emite nota, não solicita certificado digital, não confirma pagamento de fornecedor e não registra compras fiscais em contabilidade.
