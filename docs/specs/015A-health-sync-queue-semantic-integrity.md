# Spec 015A - Integridade semântica da fila de sincronização

## Status

Planejada.

## Ticker

`015A`, solicitado explicitamente para registrar a correção da implementação
original da subtarefa `015-11`, sem criar `015-15`.

## Contexto

A Fase 15 integrou ao calendário o resumo local da fila do Health Connect e uma
sinalização segura para operação inválida ou aggregate não resolvido. A
implementação atual de
`RoomRepositories.observeForAggregatesWithIntegrity()` considera um aggregate
resolvido quando o registro existe e passa pelo mapper estrutural usado na
observação global.

Essa regra é insuficiente. Uma operação somente pode ser considerada resolvida
quando o registro local também sustenta semanticamente o `recordType` daquela
operação.

Exemplos confirmados:

- `DailySummaryEntity` com `validity = INVALID` pode virar domínio, mas não é
  elegível para `StepsRecord`;
- `HeartRateMeasurement` pode ser estruturalmente válido, mas não possuir
  timestamp, validade ou origem elegível para `HeartRateRecord`;
- uma atividade pode sustentar sessão e BPM sem sustentar distância ou calorias;
- a resolução atual compara somente `aggregate`, embora a fila seja granular por
  `aggregate + recordType`.

## Definição contratual

Uma operação possui aggregate resolvido somente quando o registro local passa
pelas regras semânticas necessárias para sustentar aquela operação específica.
Mapeabilidade estrutural isolada não é suficiente.

A elegibilidade deve ser avaliada pelo par `aggregate + recordType`. A
inelegibilidade de um record complementar de uma atividade não invalida os
demais records elegíveis.

Atividades com `ParserValidity.PARTIAL` continuam elegíveis para revisão e
sincronização granular. Somente `ParserValidity.INVALID` bloqueia completamente
a atividade.

O histórico dedicado de frequência cardíaca proveniente de
`workout_heart_rate_samples` é uma projeção local independente da fila do Health
Connect e deve permanecer inalterado.

## Estado atual confirmado

- `RoomRepositories.observeForAggregatesWithIntegrity()` mapeia operações
  globais e compara `operation.key.aggregate` com um conjunto de aggregates.
- `observeStructurallyValidHealthSyncAggregates()` usa mapeamento estrutural de
  resumos, medições de BPM, atividades, pesos e perfis.
- `recordType` não participa da resolução semântica do aggregate.
- `HealthSyncOperationEntity.toDomain()` já valida formato e compatibilidade
  estática do par aggregate/record.
- `LocalCalendarRepository` já converte diagnóstico da fila em
  `CalendarDataIssue` para `HEALTH_SYNC_QUEUE`.
- a UI já mostra `Status indisponível` quando a integridade da fila está
  degradada.
- operações sem data local confiável não são associadas por `syncedAt`.
- histórico dedicado de FC lê amostras de atividades diretamente de
  `workout_heart_rate_samples`, sem materializá-las em
  `heart_rate_measurements`.

## Objetivo

Corrigir exclusivamente a validação read-only de integridade da fila para que
cada operação válida seja resolvida pela chave completa:

```text
aggregate.type + aggregate.id + recordType
```

Aggregate inexistente ou semanticamente inelegível deve produzir
`hasUnresolvedAggregate = true`. Operação malformada deve produzir somente
`hasInvalidOperation = true`.

## Requisitos funcionais

### Operação

- mapear cada `HealthSyncOperationEntity` pelo contrato atual;
- tratar falha do mapper da operação como `hasInvalidOperation = true`;
- não classificar a mesma operação malformada como aggregate não resolvido;
- preservar operações válidas selecionadas para os aggregates observados;
- manter diagnóstico booleano, sem IDs, timestamps, payloads ou métricas.

### DailySummary + STEPS

Considerar resolvido somente quando:

- o registro existe e é estruturalmente mapeável;
- `validity == VALID`;
- a origem é `WATCH_DAILY_SUMMARY`;
- existe identidade de dispositivo exigida pelo fluxo atual;
- os dados e a janela temporal sustentam `StepsRecord`;
- `recordType == STEPS`.

Nenhum outro `recordType` é elegível para esse aggregate.

### HeartRateMeasurement + HEART_RATE

Considerar resolvido somente quando:

- o registro existe e é estruturalmente mapeável;
- `validity == VALID`;
- `measuredAt` existe;
- a origem é `MANUAL_MEASUREMENT` ou `WATCH_D1_HISTORY`, conforme mapper atual;
- o BPM e o contexto persistido permanecem válidos;
- `recordType == HEART_RATE`.

`MEASUREMENT_STREAM`, `APP_WORKOUT_SESSION` e `WATCH_WORKOUT_ACTIVITY` não são
elegíveis quando persistidos como `HeartRateMeasurement` neste fluxo.

Essa regra não se aplica às amostras de uma `WorkoutActivity` armazenadas em
`workout_heart_rate_samples`.

### Regras base de WorkoutActivity

Uma operação de atividade somente pode ser resolvida quando:

- a atividade existe e é estruturalmente mapeável;
- existe importação relacionada válida;
- o workflow não é `DISCARDED` nem uma etapa anterior à sincronização;
- o workflow é compatível com o ciclo de vida da operação já existente;
- `parserValidity != INVALID`;
- duração e janela temporal são válidas;
- existe tipo confirmado diferente de `UNKNOWN`.

`ParserValidity.PARTIAL` continua aceito. Workflows de sincronização já
persistidos, inclusive estados transitórios ou terminais coerentes com a fila,
não podem transformar retrospectivamente uma operação válida em órfã.

### WorkoutActivity + EXERCISE_SESSION

Aplicar regras base da atividade. Sessão válida é record principal e não depende
de distância, calorias ou BPM.

### WorkoutActivity + DISTANCE

Além das regras base:

- exigir distância positiva e finita;
- preservar prioridade `calibrada -> original -> estimada`;
- não inventar distância;
- ausência de distância afeta somente operação `DISTANCE` existente.

### WorkoutActivity + TOTAL_CALORIES_BURNED

Além das regras base:

- exigir calorias positivas e finitas;
- não inventar calorias;
- ausência de calorias afeta somente operação
  `TOTAL_CALORIES_BURNED` existente.

### WorkoutActivity + HEART_RATE

Além das regras base:

- exigir pelo menos uma amostra real de BPM;
- aceitar somente amostras dentro da janela inclusiva da sessão;
- exigir BPM dentro da faixa válida do domínio;
- não sintetizar BPM;
- ausência de amostras afeta somente operação `HEART_RATE` existente.

Essa elegibilidade da fila não altera a projeção local do histórico dedicado de
FC.

### WeightMeasurement + WEIGHT

Considerar resolvido somente quando:

- o registro existe e é estruturalmente mapeável;
- o peso convertido é positivo e finito;
- `recordType == WEIGHT`.

### UserProfileHeight + HEIGHT

Considerar resolvido somente quando:

- o perfil existe e é estruturalmente mapeável;
- a altura existe, é positiva e finita;
- `recordType == HEIGHT`.

## Fonte de verdade da correção

`HealthSyncOperationKey` permanece chave canônica. A implementação deve possuir
um resolvedor semântico nomeado que receba a operação válida e índices locais
carregados em lote. O resolvedor delega para predicados nomeados por tipo de
aggregate e `recordType`.

Direção:

```text
HealthSyncOperation válida
        +
índice local de candidates mapeados
        ↓
isSemanticallyResolved(operation)
        ↓
predicado nomeado por aggregate + recordType
```

Não usar como fonte universal:

- presença do aggregate em conjunto sem `recordType`;
- projeção temporal do calendário;
- um predicado SQL extenso que replique domínio;
- `HeartRateMeasurement.isEligibleForSync`, pois essa propriedade atual cobre
  política D1 específica e não a medição manual aceita pelo writer;
- estado da fila para filtrar histórico dedicado de FC.

## Consultas e composição

- observar operações e candidatos em lote;
- evitar consulta individual por operação ou atividade;
- reutilizar queries globais simples existentes quando suficientes;
- adicionar somente queries Room reativas e read-only necessárias para
  importações e amostras de atividades;
- mapear atividades completas com importação relacionada e amostras agrupadas;
- não carregar intervalos quando não participarem da elegibilidade;
- não criar tabela, índice, migration ou materialização;
- manter custo linear no volume local e documentar risco residual.

## Contrato de cenários

| Cenário | `hasInvalidOperation` | `hasUnresolvedAggregate` |
|---|---:|---:|
| Operação malformada | `true` | `false` |
| Aggregate inexistente | `false` | `true` |
| `DailySummary` com `validity = INVALID` | `false` | `true` |
| `HeartRateMeasurement` sem timestamp | `false` | `true` |
| `HeartRateMeasurement` com origem inelegível | `false` | `true` |
| `WorkoutActivity` com `parserValidity = INVALID` | `false` | `true` |
| `WorkoutActivity PARTIAL` com sessão válida | `false` | `false` |
| `WorkoutActivity PARTIAL` sem distância e sem operação `DISTANCE` | `false` | `false` |
| `WorkoutActivity PARTIAL` sem distância, mas com operação `DISTANCE` | `false` | `true` |
| `WorkoutActivity PARTIAL` com BPM real e operação `HEART_RATE` | `false` | `false` |
| Atividade sem BPM e sem operação `HEART_RATE` | `false` | `false` |
| Atividade sem BPM, mas com operação `HEART_RATE` | `false` | `true` |
| Aggregate válido fora da janela aberta no calendário | `false` | `false` |
| Aggregate válido e elegível | `false` | `false` |

## Comportamentos preservados

- `ParserValidity.PARTIAL` continua revisável, calibrável e sincronizável.
- somente `ParserValidity.INVALID` invalida a janela da sessão.
- sessão, distância, calorias e BPM permanecem operações granulares.
- ausência de complemento não invalida sessão nem outro complemento.
- `ExerciseSessionRecord` permanece record principal.
- nenhum dado ausente é inventado.
- atividades descartadas permanecem excluídas do histórico dedicado de FC.
- amostras de atividade continuam no gráfico, média, mínimo, máximo, contagem e
  lista da tela dedicada.
- origem dessas amostras permanece `WATCH_WORKOUT_ACTIVITY`.
- medições manual, D1 e amostras de atividade continuam coexistindo.
- nenhuma amostra é copiada para `heart_rate_measurements`.
- calendário permanece local, read-only e sem iniciar sincronização.
- falha da fila não apaga nem oculta dados locais válidos.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`, somente se comentário ou contrato mínimo precisar ser esclarecido;
- `RoomRepositories.observeForAggregatesWithIntegrity()`;
- resolvedor semântico e predicados nomeados por par;
- queries globais read-only de importações e amostras, se necessárias.

Arquivos de referência cujo comportamento deve permanecer:

- `core/healthconnect/HealthRecordMapper.kt`;
- `core/model/HeartRateAndSync.kt`;
- `HealthRecordMapper.findEligibleWorkoutRecordTypes()`;
- `WorkoutActivity.sessionWindowOrNull()`;
- `WorkoutDetailDao.observeHeartRateHistoryBetween()`;
- `RoomRepositories.observeHistory()`;
- `HeartRateHistoryQuery`;
- `HeartRateHistoryEntry`.

## Abordagem escolhida

1. Preservar API pública de observação da fila.
2. Manter classificação de operação malformada baseada no mapper atual.
3. Construir índices reativos globais de aggregates em lote.
4. Resolver somente operações válidas pela chave completa.
5. Aplicar regras semânticas nomeadas conforme tipo do aggregate e record.
6. Propagar flags existentes até `LocalCalendarRepository` sem mudar UI.
7. Revisar uma única vez pela tabela contratual.

## Alternativas descartadas

- manter validação apenas estrutural: não representa aptidão para o record;
- comparar somente `aggregate`: perde granularidade dos complementos;
- filtrar pela janela aberta: cria falso positivo fora do período observado;
- reutilizar projeção do calendário como domínio universal: mistura políticas;
- reproduzir todas as regras em SQL: duplica domínio e aumenta divergência;
- chamar resolver suspenso por operação: introduz N+1;
- exigir `ParserValidity.VALID`: remove suporte contratual a `PARTIAL`;
- exigir todos os complementos: destrói sincronização granular;
- alterar histórico dedicado de FC: mistura projeção local com fila externa;
- corrigir writer, provider ou coordinator: fora do problema observado.

## Riscos e mitigação

- custo linear de observação global: usar índices em memória e consultas em lote;
- divergência futura entre writer e integridade: funções nomeadas e revisão contra
  mappers atuais;
- estados transitórios da atividade: aceitar workflows coerentes com operações
  existentes, sem limitar resolução a criação de nova operação;
- duplicação entre aggregate de medição e atividade: manter tipos distintos;
- parsing temporal inválido: converter falha local em aggregate não resolvido;
- regressão no histórico de FC: não alterar query, contrato ou composição desse
  histórico.

## Estratégia de validação

Não criar, alterar ou executar testes automatizados.

Após implementação, executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Fazer revisão estática única usando todos os cenários da tabela. Registrar como
reproduzidos manualmente somente estados efetivamente observados. Não alterar a
implementação após revisão final sem achado novo e materialmente diferente.

## Ordem das subtarefas

```text
015A-01 -> 015A-02
```

## Premissas

- implementação visual existente de `Status indisponível` está correta;
- `HealthRecordMapper` atual define writer funcional preservado;
- `PARTIAL` segue contrato posterior da Fase 11 e ADR-011;
- fila permanece global e sem chave estrangeira polimórfica;
- schema v5 permanece suficiente;
- nenhum ADR novo é necessário porque a mudança corrige aderência a contratos
  existentes.

## Critérios de aceite

1. Toda operação válida é resolvida por `aggregate.type + aggregate.id + recordType`.
2. Mapeabilidade estrutural isolada não resolve aggregate semanticamente inelegível.
3. Operação malformada não é classificada também como aggregate não resolvido.
4. Todos os cenários da tabela produzem flags esperadas.
5. Aggregate válido fora da janela observada não produz inconsistência.
6. `ParserValidity.PARTIAL` permanece aceito.
7. Sincronização de atividade permanece granular.
8. Ausência de complemento afeta somente operação correspondente existente.
9. Histórico dedicado de FC permanece independente e inalterado.
10. UI continua mostrando `Status indisponível` para degradação da fila.
11. Nenhuma data artificial usa `syncedAt`.
12. Nenhum sync é iniciado pelo calendário.
13. Nenhum schema, migration, provider, BLE, importador ou teste é alterado.
14. Documentação registra comandos realmente executados e riscos residuais.

## Fora do escopo

- protocolo BLE;
- importadores D1, D3 ou D4;
- providers externos;
- escrita ou leitura adicional do Health Connect;
- reconciliação, reparo ou exclusão de operações;
- migrations, entidades, schema ou versão do banco;
- mudança de workflow;
- telas e navegação;
- histórico dedicado de FC;
- `015-12`, `015-13` ou criação de `015-15`;
- testes automatizados;
- refatorações não relacionadas.

## Documentos obrigatórios

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/specs/015-calendar-consolidated-history.md`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- `docs/tasks/015-calendar-consolidated-history/015-04-add-calendar-range-queries-and-repository-projections.md`;
- `docs/tasks/015-calendar-consolidated-history/015-05-implement-calendar-read-model-and-daily-consolidation.md`;
- `docs/tasks/015-calendar-consolidated-history/015-11-integrate-sync-status-empty-states-errors-and-visual-polish.md`;
- `docs/tasks/015-calendar-consolidated-history/015-14-include-workout-heart-rate-in-dedicated-history.md`;
- `docs/tasks/012-native-health-connect/012-07-implement-workout-session-and-complementary-record-mappers.md`;
- `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md`.

## Definition of Done

- `015A-01` implementada dentro do escopo permitido;
- `015A-02` revisada uma única vez pela tabela;
- comandos permitidos executados e registrados;
- preservação de `PARTIAL`, granularidade e histórico de FC confirmada;
- spec, overview e subtarefas alinhados;
- nenhum estado manual marcado como validado sem reprodução real.
