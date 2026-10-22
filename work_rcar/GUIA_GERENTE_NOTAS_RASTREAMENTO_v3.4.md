# RCarPremium v3.4 — Gerente, Arquivo de NF-e e Acompanhamento de Pedidos

## Alterações desta versão

- Adiciona o perfil **GERENTE** ao cadastro de usuários feito pelo CEO. O gerente entra pela opção **Login do Funcionário**, mas recebe o painel próprio de gerente.
- No painel **Logins e permissões**, o CEO habilita **Lançar notas fiscais** para a conta do gerente ou funcionário designado. A permissão não é concedida automaticamente.
- **Importar NF-e** por arquivo XML, link direto HTTPS ou QR Code já reconhecido pelo app continua abrindo conferência; o gerente informa quantidades físicas. **O estoque NÃO é aumentado enquanto o CEO não aprovar.**
- Ao confirmar a conferência de um XML, é arquivado o documento de ENTRADA, com número da NF, chave/código, fornecedor, usuário responsável, data e hora e identificação da conferência. O CEO vê o contador de notas novas na **Central de notas fiscais** do painel.
- O CEO pode abrir a conferência, aprovar, ajustar ou devolver pelos recursos anteriores. No arquivo de notas o status é derivado da própria conferência e o CEO pode marcar como lido.
- Arquivo separado para documentos de VENDA: o CEO pode anexar XML da NF-e ou PDF/DANFE ao pedido por número. O cliente só tem acesso à nota vinculada ao próprio pedido, na tela **Acompanhar entrega**.
- Produtos, login persistente, estoque e rodapés existentes foram mantidos.

## Como testar, no MESMO aparelho Android

1. No Android Studio, abra `work_rcar` e compile com SDK configurado.
2. Faça login como CEO, entre em **Logins e permissões**, crie o perfil **Gerente** e ative **Lançar notas fiscais**.
3. Faça login no perfil do gerente (acesso interno de funcionário). Selecione **Importar NF-e • XML / QR / Link**, importe um XML de teste, conte os itens e envie.
4. Volte ao CEO e abra **Central de notas fiscais**. Confira número/código, hora, responsável e aviso de nota não lida. Abra **Conferência de peças** e aprove, devolva ou ajuste. O estoque não deve mudar antes da aprovação.
5. Faça uma compra com usuário cliente em ambiente de testes. Na tela **Pedidos**, abra **Acompanhar entrega • mapa e nota fiscal**.
6. No painel de Gestão de Pedidos do CEO, clique em **Editar rastreio / prazo / localização** no mesmo pedido. Informe apenas coordenadas realmente recebidas da transportadora. A tela do cliente passa a exibir o mapa e o horário da última informação.
7. Na **Central de notas fiscais**, escolha **Anexar NF-e/DANFE a pedido do cliente** e selecione XML/PDF. O cliente consegue abrir seu arquivo na tela de acompanhamento. Outro cliente não deve visualizar essa nota.

## Organização dos documentos privados

Os documentos são armazenados **no armazenamento interno privado do app**, em:

```
files/notas_fiscais/entrada/AAAA/MM/<id>.xml
files/notas_fiscais/saida/AAAA/MM/<id>.xml ou <id>.pdf
```

O índice local registra: tipo, identificação fiscal, responsável, data e hora, hash SHA-256, pedido ou conferência e leitura pelo CEO. A central exibe as notas agrupadas por ano e mês. Esse repositório não valida autenticidade fiscal, eventos da SEFAZ nem substitui um arquivo fiscal oficial com backup controlado. Os anexos são excluídos do backup automático do Android; faça retenção segura no backend antes de operar comercialmente.

## Sobre o mapa e acompanhamento em tempo real

A tela Android já oferece histórico por etapas, pedido, valor, transportadora, código de rastreio, prazo, nota fiscal e um **mapa OpenStreetMap** com a **última latitude/longitude recebida**, se houver. O cliente pode atualizar manualmente; a tela também verifica atualizações locais a cada 30 segundos enquanto estiver aberta. Caso a informação seja antiga, aparece o aviso de localização desatualizada. Não há marcador falso quando a transportadora não disponibiliza localização.

**Limitação importante:** o app ainda usa pedidos e anexos locais. Dados de CEO, gerente, cliente e transportador só são compartilhados automaticamente quando usam a mesma instalação. Para localização AO VIVO entre aparelhos diferentes, é necessário implantar um backend seguro que autentique cliente/entregador, receba telemetria com consentimento do motorista e emita eventos (ou permita polling autenticado), com vínculo ao pedido pago e proteção da localização de terceiros. O backend existente da v3.0 ainda não sincroniza pedidos pagos ou documentos, portanto esta versão NÃO deve ser anunciada como rastreio ao vivo em produção.

## Requisitos de publicação

1. Backend HTTPS com login seguro, RBAC no servidor para CEO/gerente/cliente/entregador e registro imutável de eventos.
2. Integração com transportadora para atualizações de status e localização somente durante entregas ativas; expiração de dados de localização.
3. Serviço fiscal autorizado para emissão e disponibilização de NF-e reais ao destinatário, com verificação de autenticidade.
4. Armazenamento central de notas com criptografia, backups, logs de acesso e política de retenção conforme orientação contábil/LGPD.
5. Pagamentos reais apenas após integração com gateway, confirmação e conciliação no backend.

## Validação desta entrega

- Estrutura e referências XML verificadas.
- Testes existentes do backend Python: 4 passaram.
- Compilação Android completa não executada: o Gradle 9.4.1 não está disponível localmente e o download a partir de `services.gradle.org` falhou. Execute **Build → Rebuild Project** no Android Studio.
