# 037-03 — Implementar read model bounded de corridas

- **Ticker:** `037`
- **Número:** `03`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Criar leitura dedicada e bounded de corridas confirmadas, snapshot ativo e
samples em batch, entregando ao domínio somente fatos mínimos e sem reidratar
snapshot completo.

## Requisitos cobertos

- população SQL exata e janela máxima de 56 dias;
- snapshot ativo por atividade;
- escalares de pace, provenance, versão, temporalidade e plausibilidade;
- batch de HR sem N+1;
- falha por linha versus falha da fonte;
- Room v20 sem migration, entidade, coluna ou índice especulativo.

## Escopo incluído

- projection/record interno com `activityId` apenas para agrupamento;
- query dedicada em `ChampionDaos`;
- método bounded no `WorkoutActivityRepository`;
- mapeamento mínimo em `DatabaseMappers`;
- composição transacional em `RoomRepositories`;
- ordenação determinística por `startedAt` e desempate interno.

## Escopo excluído

- ampliar `CardiovascularLoadWorkoutInput`;
- reutilizar `CalendarWorkoutSummary` como fonte final;
- carregar imports, header, intervalos, derived intervals, splits ou payload;
- reidratar `WorkoutActivity` completo;
- migration, schema, índice novo, tabela, cache ou backfill;
- qualquer Health Connect ou alteração de fatos.

## Dependências

- `037-01` e `037-02` concluídas;
- `WorkoutActivityRepository` e leitura da 035-08;
- `ChampionActivityDao`/`WorkoutDetailDao`;
- `WorkoutDerivationSnapshotEntity` e índice de samples existente.

## Arquivos e símbolos prováveis

- `core/database/ChampionDaos.kt`;
- `core/database/ChampionDatabase.kt` somente leitura de entidades existentes;
- `core/model/RepositoryContracts.kt`;
- `data/DatabaseMappers.kt`;
- `data/RoomRepositories.kt`;
- novo `RunningPerformanceWorkoutRecord` interno, se necessário.

## Passos de implementação

1. Criar projection SQL com `origin`, `confirmedType`, `workflowState`,
   `startedAt`, `duration` e snapshot ativo por `activeActivityId`.
2. Filtrar `WATCH_D3_D4`, `confirmedType = RUNNING`, não descartado e janela
   semiaberta no SQL.
3. Selecionar apenas escalares/provenance necessários e manter `LEFT JOIN` para
   representar snapshot ausente como estado por linha.
4. Carregar samples com uma consulta bounded batch, ordenar e agrupar por ID.
5. Mapear linhas sem `WorkoutActivity` completo e sem reidratação derivada.
6. Representar falha batch de samples com `SOURCE_UNAVAILABLE` e status
   top-level `UNAVAILABLE`; não emitir histórico parcial como disponível.
7. Emitir uma única fonte reativa bounded, sem `Flow` por corrida e sem DAO no
   ViewModel.
8. Validar que Room/schema/migrations não mudaram.

## Testes e comandos de validação

Não criar testes automatizados. Executar, quando código for autorizado:

```bash
git diff --check
```

Build completo pertence ao gate da implementação autorizada, não à execução
documental atual.

## Definição de pronto

- consulta retorna apenas população confirmada requerida;
- snapshot ausente/inválido não desaparece silenciosamente;
- samples carregam em batch e não há N+1;
- nenhum caminho lê snapshots completos, splits ou derivados;
- repository expõe read model dedicado;
- Room permanece v20 sem schema/migration/index novos.

## Riscos e cuidados

- `LEFT JOIN` deve usar `activeActivityId` e `isActive = 1`, não snapshot
  histórico arbitrário;
- projection deve validar enumerações desconhecidas sem derrubar linhas válidas;
- falha de fonte inteira não pode ser convertida em lista vazia;
- não logar ID, BPM, samples ou payload;
- medir custo da query antes de propor índice separado.

## Status

`completed`

## Resultado da implementação

O read model bounded foi implementado com projection dedicada de atividade e
snapshot ativo, filtragem SQL estrita para corrida confirmada do relógio e duas
leituras reativas dentro de `withReadTransaction`: projection mínima e batch de
samples ordenado por atividade e temporalidade. Nenhuma atividade, snapshot,
intervalo ou split completo é reidratado.

O mapper preserva cada linha quando enumeração ou conjunto persistido é
desconhecido, marcando o fato como inválido para a policy sem promovê-lo a valor
válido. Timestamp persistido de sample que não pode ser reconstruído é ignorado
fora da janela da atividade e, dentro dela, preserva a linha e produz
`INVALID_SAMPLES`. Falha da consulta batch de samples mantém a projection da
corrida e emite `heartRateSourceAvailable = false`, permitindo à policy produzir
`SOURCE_UNAVAILABLE` e `UNAVAILABLE`.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
  — projection dedicada, `LEFT JOIN` do snapshot ativo e batch de samples.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
  — método bounded dedicado no repository.
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
  — mapeamento mínimo de projection e preservação de falhas por linha.
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
  — composição reativa e transacional sem N+1 ou hidratação completa.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RunningPerformanceHistory.kt`
  — flags mínimas para falha fechada de metadata e timestamp de sample.
- este arquivo e `037-00-overview.md` — status e evidências.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou; lint, debug e
  release concluídos.
- nenhum teste automatizado novo criado ou executado.

## Resultados e evidências

- Projection SQL filtra `WATCH_D3_D4`, `confirmedType = RUNNING`, não
  descartadas e janela semiaberta bounded; `LEFT JOIN` usa somente snapshot
  ativo por `activeActivityId`/`isActive`.
- Samples são lidos em batch, ordenados e agrupados por atividade dentro de
  `withReadTransaction`; não há N+1, `Flow` por corrida ou hidratação completa.
- Falha batch preserva pace e sinaliza `heartRateSourceAvailable = false`;
  timestamp persistido inválido preserva linha e sinaliza `INVALID_SAMPLES`.
- Falha também na invalidação reativa de samples é convertida em falha da fonte;
  quando não há linhas para anexar o erro não vira `NO_RUNS`.
- Sample inválido fora da janela da atividade não participa da decisão; sample
  inválido dentro da janela produz `INVALID_SAMPLES`.
- Inspeção de `ChampionDatabase.kt`, migrations e schemas: nenhum arquivo
  alterado; Room permanece v20.

## Decisões e desvios

- `CardiovascularLoadWorkoutInput` não foi alterado.
- Ordenação por `startedAt` e `activityId` permanece interna à projection;
  nenhum ID chega ao contrato visual.
- Falha da leitura da projection de atividades/snapshot continua propagada como
  falha de leitura para a integração posterior; esta subtarefa trata
  explicitamente a falha batch de samples exigida pelo contrato.
- `037-04`, `037-05` e `037-06` permanecem pendentes: integração em ViewModel/
  navegação, renderização e validação física ainda não foram feitas.

## Riscos residuais

- Falha da query de atividades/snapshot propaga erro para integração posterior;
  mapeamento de estado `UNAVAILABLE` dessa fonte pertence ao consumidor da
  subtarefa seguinte.
- Não há índice temporal composto em `workout_activities`; custo de query ainda
  precisa ser medido na validação física, sem antecipar schema ou índice.
- Composição física de corridas, proveniências e cenários naturais permanece
  pendente para 037-06.

## Revisão independente

- Agente `review` aprovou a implementação após correção do transporte de falha
  reativa de samples e da filtragem temporal de samples inválidos.
- Riscos mantidos: validação física e custo da query pertencem à 037-06; não há
  testes automatizados novos conforme regra do repositório.
