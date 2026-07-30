# Task 016-01 — Implementar sync incremental de passos

## Status

concluída em 30/07/2026.

## Objetivo

Implementar a operação incremental imutável de passos preservando
`DailySummary` agregado e restringindo a mudança à fila, payload, mapper e
coordenação do Health Connect.

## Resultado esperado

Cada aumento positivo do resumo gera somente delta ainda não enfileirado, com
intervalo contíguo, identidade própria e retry idempotente. Leituras iguais ou
menores não geram operação.

## Escopo incluído

- payload técnico persistido de passos;
- migration/schema/índice necessários para múltiplas operações por resumo;
- enfileiramento transacional e reconciliação;
- mapper `StepsRecord` baseado no payload congelado;
- identidade, versão, estados e retry;
- compatibilidade segura com operação cumulativa legada;
- ADR da mudança duradoura;
- este arquivo e overview.

## Escopo excluído

- entidade ou alteração semântica de `DailySummary`;
- UI, histórico, distância, calorias, BPM, exercícios, peso, altura;
- parser BLE, fontes, leitura ou limpeza remota do Health Connect;
- testes automatizados.

## Dependências

- baseline atual, `AGENTS.md`, spec 016 e overview 016;
- schema e branch atuais inspecionados antes de editar;
- sem subtarefa 016 anterior.

## Arquivos e símbolos prováveis

- `core/model/HeartRateAndSync.kt`;
- `core/database/ChampionDatabase.kt` e `ChampionDaos.kt`;
- migration e módulo de banco;
- `data/DatabaseMappers.kt` e `RoomRepositories.kt`;
- `core/healthconnect/HealthRecordMapper.kt` e
  `HealthConnectSyncCoordinator.kt`;
- contratos de repository/coordinator somente se necessário;
- schema exportado correspondente;
- `docs/decisions/adr-015-immutable-incremental-daily-steps-health-sync.md`.

## Passos de implementação

1. Confirmar schema, migrations, operações e diff inicial.
2. Definir payload técnico e invariantes sem criar entidade de domínio.
3. Evoluir persistência para armazenar snapshot completo e permitir múltiplos
   incrementos sem quebrar outros records.
4. Enfileirar delta na mesma transação que aceita o maior resumo local; usar
   último total/fim persistido, nunca memória.
5. Rejeitar igualdade, redução, delta não positivo e janela inválida sem mover
   checkpoint.
6. Fazer mapper transformar payload congelado em `StepsRecord`, incluindo offset
   derivado do `timeZoneId` persistido.
7. Garantir que retry reutilize payload, ID e versão, sem recarregar resumo para
   reconstruir count.
8. Separar dia local e impedir coexistência de cumulativo legado e incrementos;
   não excluir records remotos.
9. Preservar comportamento de todos os outros record types.
10. Revisar diff contra escopo e atualizar ADR, subtarefa e overview somente após
    validação.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Podem ser feitas inspeções estáticas de migration, payload, identidade,
reconciliação e ausência de alterações fora do escopo. Registrar somente
resultados realmente obtidos.

## Definição de pronto

- payload técnico contém delta, totais, janela e fuso;
- operação criada e checkpoint avançado na mesma transação;
- operações são imutáveis após criação;
- intervalos não se sobrepõem e IDs diferem entre incrementos;
- mapper não lê `DailySummary.steps` para formar `StepsRecord`;
- retry mantém identidade e versão;
- novo dia reinicia sequência sem usar checkpoint anterior;
- cumulativo legado não é misturado com incremental;
- `DailySummary` e fluxos não relacionados permanecem agregados/inalterados;
- comandos de validação passam ou falhas ficam registradas;
- documentação registra arquivos, decisões, evidências e riscos.

## Riscos e cuidados

- Não usar somente estado em memória.
- Não recalcular payload no coordinator ou mapper.
- Não usar total/delta mutável em identidade.
- Não alterar versão de operação existente para representar novo incremento.
- Não declarar validação física sem execução autorizada e evidência real.

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`;
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/7.json`;
- `docs/decisions/adr-015-immutable-incremental-daily-steps-health-sync.md`;
- esta subtarefa e overview 016.

### Decisões e desvios

- O checkpoint não ganhou entidade de domínio: é derivado das operações
  incrementais persistidas, incluindo estados pendente, sincronizando, falho e
  concluído.
- A fila evoluiu de v6 para v7 com payload técnico, sequência e índice que
  permite múltiplas operações de passos por resumo.
- `collectedZoneId` é congelado no primeiro contexto válido do resumo; operações
  existentes têm precedência para impedir mudança de fuso durante a sequência.
- Operação cumulativa legada recebe ponte única congelada com a identidade
  anterior. Enquanto essa ponte existir, não são criados incrementos no mesmo
  dia; novo dia inicia sequência independente. Nenhum record remoto é removido.
- O mapper de passos deixou de receber `DailySummary`; usa exclusivamente o
  payload persistido e calcula offsets a partir do fuso congelado.
- Nenhum fluxo de BPM, exercícios, peso, altura, parser BLE ou UI local foi
  alterado funcionalmente.

### Comandos executados

- `./gradlew app:compileDebugKotlin` — primeira execução revelou referências
  incorretas ao fuso; correção aplicada e repetição concluída com sucesso.
- `./gradlew lint assembleDebug assembleRelease` — primeira execução excedeu
  timeout após concluir tarefas de assemble; repetição concluída com sucesso.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- `git diff --check` — sucesso antes e após documentação final.
- **Pós-revisão 016-02:** três correções aplicadas (`resolveDailyStepsWindowEnd` com validação de fuso; `isValidDailyStepsChain` com uniformidade de `timeZoneId`; remoção de `allowNewOperation = false` do `reconcileBacklogInternal`). Compilação e build completos novamente: `BUILD SUCCESSFUL`.

Testes automatizados não foram criados, alterados ou executados, conforme
restrição explícita do usuário e `AGENTS.md`.

### Resultados e evidências

- schema v7 contém payload incremental e índice composto por resumo, tipo e
  sequência; migration `6 -> 7` foi registrada no módulo do banco.
- merge local e criação da operação ocorrem dentro de `withWriteTransaction`.
- `5.991 -> 6.028 -> 6.040` é representado por payloads `5.991`, `37` e `12`;
  retries preservam ID e versão fixa da operação.
- igualdade, redução, delta não positivo, janela inválida e cadeia inconsistente
  não criam nova operação nem reduzem checkpoint.
- ponte legada e incrementos são mutuamente exclusivos por resumo; incrementos
  de novo dia usam outro resumo e outro ID.
- `DailySummary` continua agregado e permanece fonte de UI, histórico,
  distância e calorias estimadas.
- `app:compileDebugKotlin` e build completo passaram. Lint manteve apenas
  warnings preexistentes de override deprecated e índice de FK já existente.

### Pós-conclusão

- **Commit `387945a`:** `isDailySummaryEligibleForSteps` impedia passos de
  operações sem `dailyStepsPayload` (legado cumulativo). Corrigido com
  curto-circuito `dailyStepsPayload == null`. Ver spec 016 seção Pós-entrega.

### Riscos residuais

- A ponte de operação cumulativa legada não permite conhecer estado remoto além
  do resumo local; por isso preserva ID e evita limpeza automática.
- A fila cresce por aumento positivo observado; compactação futura não pertence
  a esta subtarefa.
- O provider Health Connect ainda exige validação física específica de intervalos
  incrementais; nenhuma validação física foi declarada como executada.
- O custo de localizar operações do resumo é linear dentro da fila desse resumo;
  não foi introduzido estado em memória.
