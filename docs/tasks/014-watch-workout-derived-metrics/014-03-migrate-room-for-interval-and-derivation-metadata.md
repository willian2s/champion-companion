# 014-03 — Migrar Room para perfil, raw/measured e derivação

- **Ticker:** `014`
- **Número:** `03`
- **Status:** pending

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
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- diff de schema novo;
- matriz tabela/coluna/default/índice;
- matriz parser version → tratamento da coluna legada;
- abertura sanitizada de banco legado;
- resultado dos builds.

## Definição de pronto

Migration local aditiva registrada, schema novo exportado, raw `d4-v1`
preservado e item `014-03` marcado.

## Rollback

Antes de release, reverter migration/schema e entidades novas. Após distribuição,
rollback exige migration forward; nunca reduzir versão ou apagar dados.
