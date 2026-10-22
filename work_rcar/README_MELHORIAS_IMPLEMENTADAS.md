# RCarPremium — melhorias de loja e painel CEO

## Novos recursos

- Painel do CEO: produtos cadastrados/edição, pré-visualização da loja, ofertas e configuração de Pix/frete.
- Edição de produtos existentes pelo formulário de cadastro.
- Descontos percentuais persistidos localmente e exibidos na lista de ofertas.
- Menu inferior protegido: carrinho, pedidos e perfil pedem login de cliente quando necessário.
- A compra agora pede login antes de permitir adicionar produtos ao carrinho/finalizar.
- Checkout mostra endereço padrão, frete configurado e total com frete.
- Gerador local de QR Code Pix e código copia-e-cola, usando a chave Pix cadastrada pelo CEO.
- Atalhos flutuantes de voltar no topo e na parte inferior das telas.
- Botão “↑ TOPO” na página inicial da loja.

## Configuração necessária pelo CEO

1. Abra o acesso interno e entre como CEO.
2. No Painel do CEO, abra **Configurar chave Pix da loja**.
3. Informe a chave Pix comercial, nome do recebedor, cidade e valor padrão do frete.
4. Salve a configuração.
5. No checkout, o cliente deve cadastrar/selecionar um endereço; ao escolher Pix, o app mostra QR Code e código copia-e-cola.

## Observações importantes

- O QR Code Pix é montado localmente com a chave configurada. O app não consulta o banco e não consegue confirmar automaticamente o recebimento; a confirmação do pagamento e a integração com um provedor devem ser adicionadas antes de uso comercial em produção.
- O frete nesta versão é uma taxa padrão definida pelo CEO, não uma cotação real por CEP/transportadora.
- O armazenamento de produtos, ofertas, endereço e configuração é local ao dispositivo, conforme a arquitetura existente; não sincroniza entre aparelhos.
- O projeto não foi compilado neste ambiente: o Gradle Wrapper tentou baixar Gradle, mas o ambiente estava sem acesso à rede. Valide em Android Studio com Android SDK instalado e internet disponível para baixar a dependência ZXing.
