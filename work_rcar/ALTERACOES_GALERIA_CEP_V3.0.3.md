# RCarPremium v3.0.3 — galeria do anúncio e consulta de CEP

## Tela de detalhes do produto

- Foto do anúncio com gestos: tocar no terço esquerdo exibe a imagem anterior; tocar no terço direito exibe a próxima. Também é possível deslizar horizontalmente para trocar a foto.
- Toque no centro abre a imagem em tela cheia, com controles discretos para ver as demais visualizações e fechar.
- O indicador abaixo da imagem acompanha a posição atual.
- Esta versão usa uma imagem já existente no projeto (`pecasautomotiva.jpg`) mais três aproximações recortadas da mesma fotografia. **Não são fotos novas de diferentes produtos**. A estrutura da galeria poderá ser conectada a várias fotos reais por anúncio quando elas estiverem cadastradas no catálogo.

## Entrega pelo CEP

- Toque em **Entrega para seu endereço** para expandir a consulta sem sair da tela.
- Digite um CEP de oito dígitos e toque em **CONSULTAR**. O app faz uma chamada HTTPS ao ViaCEP (`https://viacep.com.br/ws/{CEP}/json/`) para identificar localidade e UF.
- Frete exibido = **frete padrão da loja** previamente configurado pelo CEO; **não é uma cotação específica daquele CEP**.
- Prazo exibido = previsão informativa em dias úteis cadastrada pelo CEO; **não é uma promessa/estimativa calculada pela transportadora**. Sem cadastro, a tela indica que o prazo ainda não está informado.
- Para configurar a previsão: acesso do CEO > Configurar Pix / frete da loja > preencher **Prazo máximo estimado em dias úteis** e tocar em **Salvar prazo de entrega**. A configuração de prazo funciona independentemente de salvar uma chave Pix.
- A consulta do CEP requer conexão à internet; falhas de rede e CEPs não encontrados são tratados com mensagem legível. O CEP digitado não é gravado no servidor nem salvo pelo aplicativo nesta tela.

## Arquivos principais

- `app/src/main/java/com/example/rcarpremium/MainActivityVerProdutos.kt`
- `app/src/main/res/layout/activity_main_ver_produtos.xml`
- `app/src/main/java/com/example/rcarpremium/MainActivityConfigurarPix.kt`
- `app/src/main/res/drawable/pecas_detalhe_{freios,filtros,pastilhas}.jpg`

## Validação / limitações

- XML e recursos: verificados localmente.
- Compilação Android: **não executada com sucesso neste ambiente** porque o Gradle Wrapper não consegue baixar `gradle-9.4.1-bin.zip` da internet; testar em Android Studio em rede com Gradle e SDK disponíveis.
- Não há integração com serviços de transportadoras para cotação de prazo por CEP. Para produção, conectar à API da transportadora com origem, peso, dimensões e endereço de destino.
