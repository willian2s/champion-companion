# Task 016-02 — Revisar cenários e fechar sync incremental

## Status

concluída em 30/07/2026.

## Objetivo

Revisar independentemente a implementação 016-01 contra os requisitos de
imutabilidade, concorrência, retry, novo dia e compatibilidade legada, sem
alterar código salvo achado novo e materialmente bloqueante.

## Resultado esperado

Cada cenário relevante possui evidência estática ou manual explicitamente
classificada; documentação final não declara testes ou validações não
executados.

## Escopo incluído

- revisão do diff e da migration;
- inspeção da tabela de cenários da spec;
- comandos de build permitidos;
- atualização da spec, subtarefa 016-01, esta subtarefa e overview;
- correção única se revisão encontrar falha material nova.

## Escopo excluído

- refatoração oportunista;
- novos testes;
- limpeza remota ou leitura do Health Connect;
- mudanças em BPM, exercícios, peso, altura, UI, BLE ou `DailySummary`.

## Dependências

- `016-01` concluída e documentação preenchida.

## Arquivos e símbolos prováveis

- diff de `016-01`;
- spec e overview 016;
- ADR-015;
- entities/DAOs/mappers/repository/coordinator tocados pela implementação.

## Passos de revisão

1. Confirmar que só uma subtarefa foi executada e que `DailySummary` continua
   agregado.
2. Verificar primeira operação, crescimentos sucessivos, igualdade, redução,
   zero, janela inválida e novo dia.
3. Verificar transação, concorrência, ordinal, ausência de sobreposição e
   derivação persistida após reinício.
4. Verificar payload, ID, versão e offsets constantes no retry.
5. Verificar ponte legada sem mistura de cumulativo e incremental.
6. Verificar que mapper não consulta total mutável para count.
7. Verificar preservação de outros records e ausência de testes alterados.
8. Executar comandos permitidos e registrar evidências e riscos residuais.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validação manual física somente deve ser registrada se realmente executada.

## Definição de pronto

- cenários e critérios da spec estão classificados;
- riscos e desvios reais estão documentados;
- overview marca somente subtarefas realmente concluídas;
- não há alegação de evidência inexistente;
- build e diff check possuem resultado registrado.

## Execução

### Cenários revisados (passo 2‑8)

| # | Cenário | Resultado |
|---|---------|-----------|
| 1 | Primeira operação cobre `0 -> currentTotal` desde `dayStart` até `collectedAt` | `enqueueDailyStepsInternal` usa `dayStart` como `windowStart` quando `latest == null`; `windowEnd` = `resolveDailyStepsWindowEnd` com validação de fuso |
| 2 | Crescimentos sucessivos geram deltas contíguos sem sobreposição | `latest.second.endTime` → `windowStart` do próximo; sequência monótona |
| 3 | Igualdade (steps == previousTotalSteps) não cria operação | Retorna `operation = latest.first.toDomain()` sem inserir |
| 4 | Redução (steps < previousTotalSteps) não cria operação | Mesmo caminho |
| 5 | Zero (steps <= 0) aborta no guard inicial | `summary.steps <= 0` → `return DailyStepsEnqueueResult()` |
| 6 | Janela inválida (endTime <= startTime) aborta | `!windowEnd.isAfter(windowStart)` → retorna sem inserir |
| 7 | Novo dia inicia sequência independente | Novo `DailySummary` = novo aggregateId → sem operações existentes → `latest == null` → começa de `dayStart` com `sequence = 1` |
| 8 | Transação atômica (merge + inserção) | `withWriteTransaction` no repositório; operação criada dentro da mesma transação |
| 9 | Concorrência: checkpoint derivado de operações persistidas | Sempre lê do banco; nunca de memória. `findDailyStepsEntities` + `sortedBy` + `isValidDailyStepsChain` |
| 10 | Retry preserva payload, ID e versão | `newPendingOperationEntity` gera `clientRecordId = "${stableId}:increment:$sequence"`; `clientRecordVersion` = `INCREMENTAL_STEPS_CLIENT_RECORD_VERSION` fixo |
| 11 | Mapper não lê `DailySummary.steps` | `HealthRecordMapper.mapDailySummary` usa exclusivamente `dailyStepsPayload`; `HealthConnectSyncCoordinator.loadAndMap` não carrega `DailySummary` para steps |
| 12 | Ponte legada sem mistura com incremental | Guarda: `legacyEntities.size > 1 || (legacy.isNotEmpty() && payload.isNotEmpty())` → aborta. Ponte única congelada com identidade original |
| 13 | Outros records inalterados | Diff confirma: apenas `RoomRepositories.kt` tocado funcionalmente; BPM, peso, altura, exercícios sem alteração |
| 14 | Schema v7 compatível | Migration `6→7`; índice único `(aggregateId, recordType, operationSequence)`; export `7.json` |
| 15 | Fuso uniforme na cadeia | `isValidDailyStepsChain` verifica `payloads.any { it.timeZoneId != chainTimeZoneId }` → `false` |

### Achados da revisão

Dois achados médios e um bug identificados e corrigidos:

**Médio 1 — Validação de janela entre dias (windowEnd)**
`resolveDailyStepsWindowEnd` aceitava qualquer `collectedAtEpochMillis` sem verificar se o timestamp pertencia ao mesmo dia local do resumo. Risco: janela cruzando meia-noite criaria intervalo inválido.
Correção: adicionado `takeIf { it.atZone(zoneId).toLocalDate() == localDate }`.

**Médio 2 — Uniformidade de fuso na cadeia**
`isValidDailyStepsChain` não verificava se todos os payloads compartilhavam o mesmo `timeZoneId`. Risco: mistura de fusos na mesma cadeia.
Correção: adicionado `payloads.any { it.timeZoneId != chainTimeZoneId }`.

**Bug — reconcileBacklogInternal com `allowNewOperation = false`**
`reconcileBacklogInternal` recebeu `allowNewOperation = false` no mesmo patch, impedindo criação de operações para resumos sem operações existentes. Backlog reconciliation deve poder criar operações novas.
Correção: removido `allowNewOperation = false` do `reconcileBacklogInternal`; mantido só em `ensureDailyStepsOperationInternal`.

**Baixo — Documentação do overview**
Overview ainda marcava `1/2` e `016-02` como pendente. Corrigido nesta subtarefa.

### Comandos executados

```bash
git diff --check
# sem whitespace errors
./gradlew app:compileDebugKotlin
# BUILD SUCCESSFUL
./gradlew lint assembleDebug assembleRelease
# BUILD SUCCESSFUL
```

Testes automatizados não foram criados, alterados ou executados.

### Riscos residuais

Os mesmos de 016-01, acrescidos de:
- A correção de `reconcileBacklogInternal` não foi testada com dados reais; apenas inspeção estática.
- `resolveDailyStepsWindowEnd` com fono pode rejeitar resumos cujo `collectedAtEpochMillis` esteja em fuso diferente do `collectedZoneId`. Isso é desejado (consistência), mas pode estranhar se o usuário mudar de fuso entre coletas.

## Riscos e cuidados

- Inspeção estática não prova comportamento do provider.
- Estado remoto anterior pode exigir limpeza manual fora do app.
- Não iniciar próxima implementação automaticamente.
