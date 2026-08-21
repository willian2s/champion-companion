# 026-02 — Configurar MapLibre e OpenFreeMap

- **Ticker:** `026`
- **Número:** `02`
- **Status:** completed

## Objetivo

Adicionar a dependência estável do MapLibre Compose e configurar o mapa-base OpenFreeMap sem conta, token, API key, Maven privado ou forma de pagamento.

## Resultado esperado

O build debug consegue renderizar um mapa-base OpenFreeMap via MapLibre Compose, enquanto a variante release continua compilando normalmente e nenhum segredo novo entra no projeto.

## Requisitos e critérios cobertos

- Spec 026: critérios 6 a 10 e 31 a 32;
- MapLibre Compose vindo do Maven Central;
- OpenFreeMap `Positron` como style inicial;
- atribuição cartográfica preservada;
- ausência de credenciais de mapa.

## Escopo incluído

- confirmar versão estável vigente do MapLibre Compose antes de editar Gradle;
- adicionar entrada no version catalog;
- adicionar dependência no app;
- confirmar se `debugImplementation` é tecnicamente compatível com a organização dos source sets;
- utilizar `implementation` somente se necessário e documentar o motivo;
- confirmar Maven Central já disponível antes de alterar `settings.gradle.kts`;
- validar style público do OpenFreeMap;
- criar composição mínima de smoke somente se necessária para confirmar integração técnica.

## Escopo excluído

- tokens;
- secrets;
- `local.properties` para mapa;
- Maven privado;
- billing;
- GPS;
- rota;
- câmera de tracking;
- Room;
- Health Connect.

## Dependências

- `026-01` concluída;
- ADR da PoC vigente;
- documentação oficial do MapLibre Compose e OpenFreeMap revalidada no dia da execução.

## Versão inicial planejada

Planejamento de 2026-08-20:

```text
org.maplibre.compose:maplibre-compose:0.13.0
```

A task deve usar a release estável vigente confirmada no momento da implementação. Snapshot só pode entrar por nova decisão explícita.

## Style inicial

```text
https://tiles.openfreemap.org/styles/positron
```

Fallback permitido para investigação:

```text
https://tiles.openfreemap.org/styles/liberty
```

Se o fallback virar escolha efetiva, registrar desvio e motivo.

## Arquivos prováveis

- `gradle/libs.versions.toml`;
- `app/build.gradle.kts`;
- `settings.gradle.kts` somente se realmente necessário;
- arquivos debug mínimos de smoke, se indispensáveis;
- overview 026.

## Passos de implementação

1. Confirmar release estável e coordenadas Maven oficiais.
2. Confirmar termos atuais do OpenFreeMap e que continuam sem cadastro/API key/pagamento.
3. Confirmar Maven Central no repositório atual.
4. Adicionar dependência MapLibre Compose com menor impacto possível.
5. Configurar `Positron` como style URI do mapa-base.
6. Não desabilitar attribution/ornamentos necessários à atribuição.
7. Não adicionar qualquer mecanismo de token ou secret.
8. Validar configuração de dependências e builds.
9. Atualizar overview com arquivos, versão real, comandos e desvios.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Se houver superfície visual mínima executável nesta task:

```bash
./gradlew installDebug
```

## Definition of Done

- MapLibre Compose está resolvido por Maven Central;
- OpenFreeMap é configurado sem credenciais;
- style inicial está documentado;
- atribuição não foi removida;
- nenhum token, API key ou billing foi adicionado;
- `assembleDebug` passa;
- `assembleRelease` passa;
- lint passa;
- overview registra versão e estratégia Gradle realmente usadas.

## Execução

- **Status:** completed
- **Data:** 2026-08-20
- **Arquivos alterados:** `gradle/libs.versions.toml`,
  `app/build.gradle.kts`, `docs/decisions/adr-024-maplibre-openfreemap-gps-poc.md`,
  `docs/specs/026-maplibre-openfreemap-gps-route-poc.md`,
  `docs/tasks/026-maplibre-openfreemap-gps-route-poc/026-00-overview.md` e este
  arquivo. `settings.gradle.kts` permaneceu intacto porque Maven Central já
  estava configurado com `FAIL_ON_PROJECT_REPOS`.
- **Revalidação:** a documentação oficial do MapLibre Compose consultada em
  2026-08-20 informa `v0.14.0` como latest release e a coordenada
  `org.maplibre.compose:maplibre-compose:0.14.0`; o metadata do Maven Central
  confirmou `0.14.0` como `latest` e `release`. A página oficial do OpenFreeMap,
  seus termos e sua política de privacidade confirmaram instância pública sem
  registro, banco de usuários, API key, cookies ou pagamento, sem SLA/suporte
  personalizado, com atribuição obrigatória e style Positron em
  `https://tiles.openfreemap.org/styles/positron`; o endpoint também retornou
  um style JSON válido durante a consulta.
- **Estratégia Gradle:** adicionada entrada `maplibre-compose` no version catalog
  e dependência somente em `debugImplementation`; isso substituiu a versão
  planejada `0.13.0` pela release estável vigente `0.14.0`. A configuração foi
  resolvida em `debugCompileClasspath`; `releaseCompileClasspath` e
  `releaseRuntimeClasspath` não possuem MapLibre. Nenhum smoke/UI, GPS, rota,
  câmera, teste, credencial ou token foi adicionado.
- **Comandos e resultados:**
  - `git diff --check` — passou;
  - `./gradlew :app:dependencyInsight --dependency org.maplibre.compose:maplibre-compose --configuration debugCompileClasspath` — passou, resolveu `0.14.0`;
  - `./gradlew :app:dependencyInsight --dependency org.maplibre.compose:maplibre-compose --configuration releaseCompileClasspath` — passou, nenhuma dependência encontrada;
  - `./gradlew :app:dependencyInsight --dependency org.maplibre.compose --configuration releaseRuntimeClasspath` — passou, nenhuma dependência encontrada;
  - `./gradlew :app:assembleDebug :app:assembleRelease` — passou;
  - `./gradlew :app:lintDebug` — passou;
  - `./gradlew lint assembleDebug assembleRelease` — passou;
  - `./gradlew :app:dependencyInsight --dependency
    org.maplibre.compose:maplibre-compose --configuration debugCompileClasspath` —
    passou novamente, resolveu `0.14.0`;
  - `./gradlew :app:dependencyInsight --dependency org.maplibre.compose
    --configuration releaseRuntimeClasspath` — passou novamente, nenhuma
    dependência encontrada;
  - inspeção dos APKs — `app-debug.apk` contém runtime MapLibre;
    `app-release.apk` não contém entradas MapLibre.
- **Avisos observados:** build manteve avisos preexistentes de KSP, APIs Java
  depreciadas e cast não verificado; não houve erro novo relacionado ao MapLibre.
- **Bloqueios remanescentes:** renderização visual, carregamento efetivo do
  style, GPS, rota e validação física continuam sem evidência nesta subtarefa e
  dependem das subtarefas seguintes. Nenhuma validação em aparelho foi
  declarada.

## Riscos e cuidados

- API Compose pode diferir entre releases; implementar conforme versão fixada, não conforme snapshot;
- MapLibre Compose ainda não cobre toda a superfície do SDK nativo; confirmar somente APIs necessárias à PoC;
- OpenFreeMap não oferece SLA;
- não introduzir repositórios adicionais sem necessidade comprovada;
- se a dependência em `debugImplementation` inviabilizar referências do source set, preferir solução simples e documentada a hacks de Gradle.
