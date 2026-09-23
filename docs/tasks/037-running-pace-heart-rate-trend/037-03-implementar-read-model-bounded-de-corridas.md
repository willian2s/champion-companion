# 037-03 — Implementar read model bounded de corridas

- **Ticker:** `037`
- **Número:** `03`
- **Status inicial:** `pending`

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
