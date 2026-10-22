# RCarPremium v3.4.8 — Cargos, primeira senha e recuperação por token

## O que mudou

1. **Novos tipos de conta** no painel do CEO: Funcionário, Gerente, RH, Financeiro, Estoque, Vendas, Logística, Suporte, Compras e Cliente.
2. **Acesso por permissão individual**: os cargos de equipe são criados sem permissões operacionais. O CEO precisa concedê-las na Central de Controle. O tipo de conta sozinho não dá poderes administrativos.
3. **Primeiro acesso**: qualquer conta nova criada pelo CEO recebe o marcador `SENHA PROVISÓRIA`. Após autenticar com essa senha, o app abre uma tela obrigatória para trocar por uma senha definitiva. Não inicia sessão antes disso.
4. **Recuperação por token**: o CEO autenticado pode abrir um cartão de usuário não-CEO e tocar em `Gerar token de recuperação • CEO`. O código aparece uma vez, em formato hexadecimal com hífens, com opção de cópia explícita.
5. **Redefinição**: no login do cliente ou da equipe, a pessoa pode abrir `Esqueci minha senha`, informar e-mail, código e senha nova. O token é válido por **15 minutos**, para **uma única utilização**, com limite de **5 tentativas incorretas**.
6. **Senha definitiva**: pelo menos 10 caracteres, incluindo maiúscula, minúscula, número e símbolo.
7. **Dados protegidos**: o token não é guardado em texto puro. Seu hash é salvo localmente, e os dados de credenciais são excluídos das regras de backup do Android.

## Como testar

1. Abra a pasta `work_rcar` no Android Studio e execute a sincronização do Gradle.
2. Entre como CEO, abra **Logins e permissões** e crie uma conta RH (ou outro cargo), com senha provisória de ao menos 8 caracteres.
3. No painel de controle, conceda apenas as permissões necessárias a essa função.
4. Saia da conta do CEO no Perfil, entre com a conta criada e confira a tela **Crie sua senha definitiva**.
5. Informe senha provisória e nova senha forte; após confirmação, deve abrir o painel de sua função.
6. Para testar recuperação, retorne à conta CEO, gere um token no cartão da conta recém-criada e anote-o.
7. Saia da conta CEO. Na tela de login da equipe, abra **Esqueci minha senha**, informe o e-mail, token e uma nova senha forte.
8. O token deve funcionar somente uma vez; tente usá-lo novamente para confirmar o bloqueio.

## Limitações importantes

**Este é um protótipo Android com armazenamento local, ainda não um sistema de identidade pronto para produção.**

- Um token gerado em um celular não pode redefinir a conta armazenada em outro celular, porque as contas ainda não estão sincronizadas por backend.
- Não há envio automático de token por e-mail ou SMS; o CEO deve transmiti-lo por canal seguro.
- A recuperação via token não foi habilitada para contas CEO, para evitar redefinição de administradores por esse fluxo.
- O aplicativo ainda inclui credenciais administrativas iniciais de demonstração nas versões anteriores. Remover essas contas e implementar autenticação, sessão, auditoria, permissões e recuperação no **servidor** é obrigatório antes de uso comercial real.
- Controles e permissões no dispositivo não substituem validação no backend.

## Verificação feita

- Recursos XML e AndroidManifest validados estruturalmente.
- Novas referências de telas e ID de recuperação conferidos.
- Compilação Kotlin **não confirmada**: o Gradle 9.4.1 não pôde ser baixado no ambiente de criação.
