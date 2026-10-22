# Correção da prévia de layouts

Foi ajustada a família de fonte global do tema em `app/src/main/res/values/themes.xml`: `sans` foi substituído por `sans-serif`, uma família padrão explícita do Android. A alteração visa contornar a falha do Layout Editor (`Font_Builder_Delegate.createBuffer`, com `assetStream` nulo) ao renderizar `activity_acesso_interno.xml`.

## Como conferir
1. Abra a pasta `work_rcar` no Android Studio.
2. Use **File > Sync Project with Gradle Files**.
3. Abra `app/src/main/res/layout/activity_acesso_interno.xml` e selecione **Design** ou **Split**.
4. Se a prévia continuar presa, clique em **Build > Clean Project** e depois **Build > Rebuild Project**.

A correção de tema remove a referência suspeita, mas a renderização também depende da versão do Android Studio/Android Gradle Plugin instalada no computador.
