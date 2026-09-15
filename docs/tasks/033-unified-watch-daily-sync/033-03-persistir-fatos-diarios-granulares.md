# 033-03 — Persistir fatos diários granulares D1

- **Status:** completed
- **Depende de:** 033-01
- **Pode rodar em paralelo com:** 033-02
- **Validação física posterior:** `docs/integration/033-03-d1-v2-physical-validation.md`

## Objetivo

Evoluir Room de v17 para v18 e persistir D1 como fatos diários completos e
idempotentes.

## Escopo

- entidade/tabela D1 granular;
- chave única estável por dispositivo + data + posição;
- DAO e repository para insert/update/dedup;
- BPM nullable e raw adicionais;
- metadata de importação/component success sem reutilizar `lastImportedAt` de
  forma ambígua;
- migration `17 -> 18` aditiva;
- schema exportado v18;
- adapters de domínio/read path necessários.

## Guard rails

- não backfillar fatos inexistentes dos registros D1 BPM-only antigos;
- migration não cria snapshot, fila remota ou efeito Health Connect;
- preservar `heart_rate_measurements` históricos;
- preservar schema/contratos da 032.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchDailyD1.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD1HistoryProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/18.json`

## Decisões e desvios

- Fatos D1 usam identidade natural `deviceId + resolvedLocalDate + currentPosition`; UUID permanece somente como chave técnica da linha.
- Reimportação preserva `id` e `importedAt` existentes, atualiza fatos alterados e classifica linhas idênticas como duplicatas.
- BPM inválido permanece como `bpm = null`, enquanto `bpmRaw` e demais campos estruturais são persistidos.
- Foi reutilizada a tabela dedicada existente `device_import_metadata` com tipo próprio `WATCH_DAILY_D1` para sucesso/tentativa e contagens dos fatos, incluindo `updatedCount`; metadata legacy `WATCH_D1_HISTORY` permanece separada para BPM. Falhas/cancelamentos preservam o último sucesso conhecido. `lastImportedAt` do dispositivo não foi usado como status do componente.
- A migration `17 -> 18` adiciona `updatedCount` à metadata e cria tabela/índice dos fatos; não backfilla linhas BPM-only antigas, não cria fila remota e não altera `heart_rate_measurements`.
- O binding Hilt foi conectado ao coordinator para que runtime use repository granular real, sem alterar contratos legacy de BPM.

## Comandos executados

```text
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Resultados e evidências de implementação

- `./gradlew lint assembleDebug assembleRelease`: sucesso; lint, compilação e empacotamento debug/release concluídos.
- `git diff --check`: sucesso, sem erro de whitespace.
- Schema exportado `ChampionDatabase/18.json` registra tabela `watch_daily_d1_samples`, foreign key para `devices`, BPM nullable e índice único natural.
- Metadata terminal D1 registra falha/cancelamento mesmo sem pacotes e preserva `lastSuccessAt`; reimportação idêntica conta como duplicata e alteração de fato conta como atualização.
- Testes automatizados não foram criados nem executados, conforme autorização ausente.

## Validação física posterior — 2026-09-13

Dois dumps sanitizados de banco real confirmaram:

- posições D1 contínuas e Room v18 funcional;
- `distanceMeters == distanceRaw` nos samples observados;
- `caloriesKilocalories == caloriesRaw / 10.0` nos samples observados;
- BPM inválido não elimina passos, distância, calorias ou raw do mesmo pacote;
- reimportação preserva IDs dos samples existentes e insere apenas novos buckets;
- `Σ D1.steps` fechou exatamente com FEA1 em duas leituras sucessivas;
- workouts D4 existentes permaneceram inalterados após reimportação D1.

A mesma evidência mostrou que D1 já pode conter fatos de workout e, por isso,
levou à correção do contrato de composição da 033-04. Ver:
`docs/integration/033-03-d1-v2-physical-validation.md`.

## Riscos residuais

- `dayOffset > 0` ainda depende de validação física posterior;
- semântica funcional dos campos raw adicionais permanece desconhecida;
- composição D1/D4, read models, cutover Health Connect e remoção dos entrypoints de produto permanecem nas subtarefas seguintes;
- suíte histórica ainda contém expectativas de `d1-v1`; não foi alterada nem executada por regra do repositório;
- construção manual do coordinator sem repository granular falha de forma explícita; o binding de produção injeta `RoomRepositories`.
