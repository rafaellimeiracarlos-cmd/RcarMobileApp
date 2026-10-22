# Padrão de formatação do RCarPremium

## XML do Android

Os layouts, drawables, regras XML e o `AndroidManifest.xml` foram reformatados para seguir um padrão uniforme:

- indentação de quatro espaços por nível;
- uma propriedade XML por linha;
- tags filhas alinhadas com a estrutura do layout;
- separação visual entre elementos irmãos;
- comentários preservados;
- atributos e conteúdo funcional mantidos.

## Kotlin e Gradle

O arquivo `.editorconfig` define UTF-8, quebras de linha LF, quatro espaços e o padrão oficial do Kotlin. Quebras de linha inconsistentes e espaços finais foram normalizados. As classes de telas de Clientes, Sobre e Termos também foram reorganizadas para facilitar a leitura.

Para aplicar a formatação automática do Android Studio em um arquivo Kotlin, abra o arquivo e use **Code > Reformat Code** (atalho padrão no Windows/Linux: `Ctrl+Alt+L`). Para organizar os imports, use **Code > Optimize Imports** (`Ctrl+Alt+O`). Revise as alterações antes de aceitar modificações em arquivos com lógica complexa.

## Escopo

Esta atualização prioriza a legibilidade dos XMLs de interface, que são os arquivos usados para desenhar as telas do Android. Não altera a arquitetura nem as regras de negócio do aplicativo.
