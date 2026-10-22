# RCarPremium 3.3 — Conferência de peças e supervisão do CEO

## Fluxo implementado

1. O CEO entra em **Painel do CEO → Conferência de peças** para verificar conferências pendentes, devolvidas e aprovadas. O funcionário autorizado usa o mesmo atalho em seu painel ou **Meus produtos → Minhas conferências**.
2. **XML da NF-e:** em Importar NF-e, selecione arquivo/link/QR que forneça XML estruturado, confira o destino de cada produto, informe a **quantidade realmente recebida** e envie para o CEO. Não há entrada automática.
3. **Recebimento manual:** use Nova conferência, selecione produto já cadastrado, indique referência da compra, quantidade prevista, quantidade contada e observação. Pode incluir vários produtos no mesmo recebimento.
4. Enquanto estiver **PENDENTE**, o estoque permanece inalterado. Divergências entre previsto e recebido ficam destacadas.
5. O CEO pode **APROVAR** (inclusive informando quantidades aprovadas diferentes, com justificativa) ou **DEVOLVER** (motivo obrigatório). Após devolução, o funcionário ajusta a contagem e **REENVIA**.
6. **APROVAÇÃO** grava catálogo e histórico de conferências em uma operação local. Notas aprovadas também entram no registro de notas, evitando nova importação no aparelho. Produtos novos vindos da NF-e ficam com anúncio pausado.
7. O CEO pode corrigir estoques de uma conferência já aprovada, com **ajuste positivo ou negativo** e justificativa. O evento registra conta, data, produto, quantidade e motivo.
8. Produtos cadastrados por funcionário com estoque inicial maior que zero ficam primeiro com **saldo zero** e geram conferência pendente de aprovação.

## Permissões

- **CEO:** analisa, devolve, ajusta e aprova qualquer conferência.
- **Funcionário:** necessita da permissão `Conferir recebimentos` ou `Cadastrar produtos` para abrir conferências; consulta e reenvia apenas suas próprias.
- **Cliente:** não tem acesso ao painel de conferências.
- Vá em **CEO → Logins e permissões** para ativar `Conferir recebimentos` em cada funcionário.

## Como testar

1. Abra `work_rcar` no Android Studio e sincronize o Gradle.
2. Entre como funcionário autorizado, abra Conferência de peças, crie uma entrada manual de 3 unidades, mas **não aprove**; verifique no estoque que a quantidade continua igual.
3. Entre como CEO, devolva com o motivo `Quantidade divergente da nota`, volte ao funcionário e reenvie com quantidade corrigida.
4. Como CEO, aprove; verifique que a quantidade passou a refletir **somente a quantidade aprovada**.
5. Abra o registro aprovado, faça ajuste de `-1` com motivo e confira a trilha de auditoria.
6. Importe uma NF-e pelo XML, envie para conferência e confirme que importação repetida é bloqueada enquanto estiver pendente ou após aprovada.

## Limitações importantes

- Persistência em `SharedPreferences` **somente neste aparelho**. CEO e funcionário precisam usar a **mesma instalação** para enxergar as conferências. Para diferentes dispositivos, implementar banco e backend com transações de aprovação, trilha imutável e permissões validadas no servidor.
- Logs locais não são uma auditoria inviolável: um usuário com acesso aos dados do aparelho pode alterar os registros.
- XML fiscal é interpretado, mas não há verificação online de autenticidade/cancelamento na SEFAZ. Conferir documento e fornecedor antes de aprovar.
- O fluxo de conferência cobre recebimentos via NF-e, entrada manual e estoque inicial cadastrado pelo funcionário. O CEO conserva outras ferramentas administrativas de edição de produto; alterá-las fora da conferência não compõe esta trilha.
- A aprovação da conferência vinculada a uma compra de reposição atualiza o status da solicitação em etapa local subsequente à gravação de estoque; em falhas do aplicativo entre operações, esse status pode requerer reconciliação.
- Compile no seu Android Studio antes de usar comercialmente. Esta entrega passou por verificações de arquivos e referências, **não** por compilação e execução em dispositivo Android.

## Revisão de peças devolvidas

Quando o CEO devolver uma conferência, o funcionário poderá alterar a quantidade contada **e o vínculo com um produto cadastrado** antes de reenviar. Para XML, também pode marcar um item como produto novo. O custo fiscal original continua associado à linha da nota. O CEO deve avaliar novamente esses vínculos e as divergências antes de aprovar.

## Verificação estrutural opcional

Execute `python scripts/verificar_conferencia.py` a partir da pasta `work_rcar`. Esse script verifica os XMLs e ligações principais entre as telas, mas não substitui a compilação Kotlin nem os testes em celular.
