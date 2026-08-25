# 028-02 — Adotar Vico e registrar decisão arquitetural

- **Ticker:** `028`
- **Número:** `02`
- **Status:** `completed`
- **Depends on:** `028-01`

## Objetivo

Escolher e integrar uma engine Compose de gráficos sem migrar ainda os consumidores, preservando uma boundary própria do Champion Companion.

## Decisão

Adotado **Vico 3.2.3**.

Módulos adicionados:

- `com.patrykandpatrick.vico:compose`;
- `com.patrykandpatrick.vico:compose-m3`.

A escolha e seus limites foram registrados em:

`docs/decisions/adr-027-unified-interactive-charts.md`.

## Boundary

Features não devem importar tipos Vico diretamente. A integração futura fica encapsulada em:

`core/designsystem/chart`.

O domínio, ViewModels, repositories e persistência permanecem independentes da biblioteca.

## Compatibilidade verificada

- app: `minSdk 28`;
- Vico 3.2.3: requisito Android mínimo compatível;
- Compose e Material 3 já fazem parte da stack vigente.

## Implementação

`gradle/libs.versions.toml` passou a declarar:

- versão `vico = "3.2.3"`;
- alias `vico-compose`;
- alias `vico-compose-m3`.

`app/build.gradle.kts` passou a consumir somente esses dois módulos.

Nenhum gráfico foi migrado nesta task e nenhum código de domínio/persistência foi alterado.

## Intercorrência da validação

A primeira execução de CI falhou antes de resolver dependências porque a edição inicial do catálogo truncou acidentalmente o plugin Hilt de `com.google.dagger.hilt.android` para `com.google.dagger`.

A configuração foi restaurada imediatamente e a validação foi repetida. A falha não era causada pelo Vico.

## Verificação

GitHub Actions run `32845432177` executou com sucesso:

```bash
./gradlew lint assembleDebug assembleRelease
```

Nenhum teste automatizado foi criado ou executado.

## Saída

Engine adotada e isolada por decisão arquitetural. A próxima task pode definir os contratos neutros e o theme compartilhado sem alterar consumidores existentes.
