# 014-03 — Migrar Room para perfil, raw/measured e derivação

- **Ticker:** `014`
- **Número:** `03`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Implementar evolução Room aditiva após `v8` para persistir comprimento pessoal
do passo, fatos raw/measured, duração efetiva, elegibilidade, plausibilidade e
snapshots derivados locais.

Resultado: banco novo e legado abrem sem perda, sem derivação automática, sem
mudança de workflow e sem efeito Health Connect durante migration.

## Dependências

- `014-02` concluída com schema lógico local fechado.

## Arquivos permitidos

- `core/database/ChampionDatabase.kt`;
- `core/database/ChampionDaos.kt`;
- `core/database/ChampionMigrations.kt`;
- `core/database/DatabaseModule.kt`;
- `data/DatabaseMappers.kt`;
- `data/RoomRepositories.kt`, somente adapters mínimos;
- novo schema exportado;
- `docs/baseline/database-reference.md` e overview.

## Arquivos proibidos

- parser BLE, UI e Health Connect gateway/mapper;
- manifesto e Gradle;
- schemas históricos `1.json` a `8.json`;
- manifesto, filhos, ações ou checkpoints Health Connect, tratados em `014-12`;
- remoção de estruturas da Fase 013.

## Escopo

### Perfil

Persistir:

```text
stepLengthMeters
stepLengthSource
stepLengthUpdatedAt
```

### Intervalos raw/measured

Persistir de forma aditiva:

```text
rawMotionValue
rawDistanceValue
rawCaloriesValue
decodedSteps
effectiveDurationNanos
temporalEligibilityReason
plausibilityStatus
```

Não reutilizar `distanceMeters` legado como canônico.

### Compatibilidade `d4-v1`

Para `WATCH_D3_D4` com parser version conhecida `d4-v1`:

```text
rawMotionValue = valor legado de workout_intervals.steps
decodedSteps = null
```

A ponte preserva raw, não decodifica e não altera o valor legado. Parser version
desconhecida não recebe backfill presumido.

### Derivação local

Persistir snapshot e série derivada com:

- uma derivação ativa por atividade;
- `stepLengthMetersUsed`, origem, método, versão e `derivedAt`;
- distância canônica total como soma da série;
- métricas/ausências/elegibilidade;
- intervalos derivados e splits quando previstos pelo contrato.

## Fora do escopo

- backfill de `decodedSteps` ou métricas;
- reprocessamento de atividades;
- alteração para `READY_TO_SYNC`;
- manifesto/fila/projeção Health Connect;
- escrita ou deleção remota;
- testes automatizados.

## Implementação concluída

- `ChampionDatabase` está em `v9`, com `MIGRATION_8_9` registrada no módulo de
  banco.
- `user_profiles` persiste `stepLengthMeters`, `stepLengthSource` e
  `stepLengthUpdatedAtEpochMillis` somente quando o perfil é válido.
- `workout_intervals` recebeu fatos raw/measured aditivos; `distanceMeters`
  legado não foi reutilizado como distância canônica.
- A ponte de migration copia `steps` para `rawMotionValue` somente para
  `WATCH_D3_D4`/`d4-v1` dentro da faixa u24. Não preenche `decodedSteps`, não
  deriva métricas e não interpreta versões desconhecidas.
- O adapter de importação mantém a mesma ponte raw para novas gravações
  `WATCH_D3_D4`/`d4-v1` até a adoção do parser seguinte, sem decodificar passos.
- Snapshots, intervalos derivados e splits usam FK em cascata. Filhos apontam
  somente para `snapshotId`; a atividade é determinada pelo snapshot pai.
- `activeActivityId` nullable recebe `activityId` somente para snapshots ativos e
  possui índice UNIQUE. Isso garante a unicidade também em instalação limpa v9;
  não há índice parcial redundante.
- Mappers validam enumerações fail-closed, sequência contínua, durações efetivas
  contra a resolução, identidade da atividade relógio, filhos e splits
  recalculados do agregado. `WorkoutDerivationRecord` possui round-trip completo
  para `WorkoutDerivationSnapshot`; substituição de snapshot ativo é
  transacional.
- Timestamps derivados mantêm millis para ordenação e `epochSecond` + `nano`
  para round-trip exato de `Instant`. Durações efetivas permanecem em nanos.
- `persistImported` continua sem criação ou recálculo de snapshot.

## Passos de implementação

1. Traduzir os contratos de `014-02` em schema local aditivo.
2. Criar migration manual, provavelmente `8 → 9`, com defaults seguros.
3. Implementar ponte raw de `d4-v1` condicionada por origem e parser version.
4. Preservar IDs, `recordedAt`, revisão, calibração, imports, sessões, rotas e
   operações existentes.
5. Adicionar DAOs e mappers mínimos para round-trip transacional.
6. Garantir unicidade de derivação ativa e filhos locais da série.
7. Exportar somente o novo schema e registrar migration.
8. Atualizar referência de banco e overview.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/9.json`;
- `docs/baseline/database-reference.md`;
- este arquivo e o overview da spec.

## Critérios de aceite

- Banco sobe sem destructive migration.
- Linhas legadas permanecem sem derivação inventada.
- `d4-v1` recuperável preserva o u24 em `rawMotionValue` e mantém
  `decodedSteps = null`.
- Versões desconhecidas não são reinterpretadas.
- Schemas históricos não mudam.
- Migration não toca provider, fila remota ou workflow.
- A migration de projeções Health Connect continua pendente para `014-12`.

## Validação

```bash
./gradlew app:compileDebugKotlin
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- `9.json` exportado pelo Room contém v9, índice UNIQUE de
  `activeActivityId`, FK exclusiva por `snapshotId`, campos de precisão
  temporal e colunas raw/measured.
- `ChampionMigrations.kt` contém a matriz aditiva de colunas, defaults, índices,
  FKs e ponte `d4-v1`; schemas `1.json` a `8.json` permanecem intocados.
- `database-reference.md` registra tabela/coluna/default/índice/FK, parser e
  ausência de efeito Health Connect.
- `WorkoutDerivationRecord.toDomain` consome snapshot, intervalos e splits e
  rejeita inconsistências sem reinterpretar dados.
- `WorkoutActivity.toIntervals` preserva `d4-v1` em `rawMotionValue` também após
  upgrade; parser desconhecido não recebe fallback.
- `./gradlew app:compileDebugKotlin` — PASS após correções; warnings
  preexistentes de override deprecated em `RoomRepositories.kt` e recomendação
  preexistente de índice para `associatedDeviceId`.
- `./gradlew lint assembleDebug assembleRelease` — PASS; mesmos warnings
  preexistentes.
- `./gradlew installDebug` — PASS após a última correção; APK instalado no
  Galaxy S22 `SM-S901E`.
- `git diff --check` — PASS.
- `git status --short` — PASS; somente arquivos da subtarefa e schema `9.json`
  aparecem alterados/não rastreados.
- Testes automatizados — não criados nem executados, conforme task e
  `AGENTS.md`.

## Decisões e desvios

- O índice parcial inicialmente usado foi substituído por `activeActivityId`
  nullable com índice UNIQUE declarado na entidade e criado pela migration.
- `activityId` foi removido dos filhos derivados; duplicação de FK e risco de
  associação cruzada deixaram de existir.
- Como `Instant` pode conter nanos não representáveis em millis, entidades
  derivadas persistem pares `epochSecond`/`nano` além dos millis de ordenação.
- A migration continua sem backfill derivado, sem alteração de workflow e sem
  qualquer provider, gateway, fila ou writer Health Connect.

## Riscos e bloqueios remanescentes

- Abertura física de banco legado, cascatas e round-trip em runtime não foram
  executados por testes automatizados; permanecem evidenciados por migration
  manual, schema Room, mappers fail-closed e builds completos.
- Parser `d4-v2`, resolução temporal produtiva, persistência/editing do perfil,
  derivação, UI, reprocessamento e projeções Health Connect continuam
  bloqueados para as subtarefas seguintes. Esta task não avança para `014-04`.

## Definição de pronto

Migration local aditiva registrada, schema novo exportado, raw `d4-v1`
preservado, round-trip estrutural validado e item `014-03` marcado após revisão
independente.

## Rollback

Antes de release, reverter migration/schema e entidades novas. Após distribuição,
rollback exige migration forward; nunca reduzir versão ou apagar dados.
