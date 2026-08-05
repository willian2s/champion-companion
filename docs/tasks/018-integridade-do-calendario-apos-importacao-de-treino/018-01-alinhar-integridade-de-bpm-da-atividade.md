# 018-01 — Corrigir integridade temporal da reimportação

- **Ticker:** `018`
- **Número:** `01`
- **Status:** concluído

## Execução atual

- **Arquivos alterados:** documentação SDD 018, ADR-016, baseline, domínio de
  atividade, planner de persistência, migration, versão do banco, schema 8 e
  registro da migration.
- **Decisões e desvios:** pacote 018 foi normalizado porque 015, 015A e 017 já
  estão concluídas. A hipótese inicial de BPM foi refutada: predicado atual já
  filtra janela. Causa confirmada é `reviewedAt < recordedAt` após reimportação;
  correção preserva `recordedAt` e adiciona migration 7→8 estreita.
- **Comandos executados:** consulta de banco via ADB autorizado, inspeção de
  código, smoke manual de abertura do Calendário, `git diff --check`,
  `./gradlew --stop`, `./gradlew clean`, `./gradlew lint assembleDebug
  assembleRelease` e `./gradlew installDebug`.
- **Resultados e evidências:** banner reproduzido no Galaxy S22; diagnóstico
  seguro mostrou `HEALTH_SYNC_QUEUE/UNRESOLVED_AGGREGATE`. Logcat de debug
  temporário isolou quatro tipos de operação de atividade; consulta do banco
  confirmou revisão anterior à reimportação. Diagnóstico temporário foi
  removido antes da correção final.
- **Resultados:** `git diff --check`, lint, debug, release e `installDebug`
  passaram. Schema 8 foi exportado. APK final abriu Calendário sem banner;
  screenshot sanitizado salvo em `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/calendar-final-visible-2.png`.
  Banco pós-migration confirmou versão 8, zero linhas com
  `reviewedAt < recordedAt` e 14 operações de atividade `SYNCED`. Smoke com
  Bluetooth desligado não mostrou banner nem iniciou eventos BLE; Bluetooth foi
  restaurado ao final.
- **Riscos residuais:** linhas legadas com `reviewedAt < startedAt` não são corrigidas
  pela migration e continuam degradando fail-closed; novo comportamento depende
  de preservar `recordedAt` em todos os caminhos de reimportação.
- **Revisão independente:** encontrou risco de `recordedAt` preservado anterior
  ao novo `startedAt`; guard fail-closed adicionado em `reimport` e planner.
  Revisão final aprovou implementação sem bloqueadores.

## Objetivo

Corrigir o falso estado de dados parciais do mês causado pela ordem temporal
inválida em atividade D3/D4 reimportada após revisão.

## Resultado esperado

Atividade reimportada preserva `recordedAt` original, mantém `reviewedAt` válido
e volta a resolver seus aggregates. Registro legado elegível é corrigido por
migration; inconsistência sem reparo seguro continua degradando.

## Escopo incluído

- confirmar revisão do checkout e APK instalado;
- reproduzir banner com banco local preservado;
- identificar categoria segura da issue;
- preservar `recordedAt` em reimportação no domínio e na persistência;
- adicionar migration 7→8 estreita para registros legados elegíveis;
- validar candidate resolvido e quatro tipos de operação da atividade;
- registrar status, diff, comandos, evidências e riscos.

## Escopo excluído

- alteração de `CalendarScreen`, texto ou navegação;
- alteração de writer, coordinator, importador, workflow ou histórico;
- apagar, reparar ou inserir dados durante consulta do Calendário;
- DAO ou refactor;
- testes automatizados;
- logs com IDs, métricas, payloads ou timestamps individuais.

## Dependências

- Spec `018-integridade-do-calendario-apos-importacao-de-treino`;
- baseline atual e `AGENTS.md`;
- código de `RoomRepositories`, `HealthRecordMapper` e
  `LocalCalendarRepository`;
- banco local do APK debug disponível para reprodução manual.

## Arquivos e símbolos prováveis

- `RoomRepositories.kt`: `planWatchWorkoutPersistence` e índice semântico;
- `WatchWorkoutActivity.kt`: `WorkoutActivity.reimport`;
- `ChampionMigrations.kt`: `MIGRATION_7_8`;
- `ChampionDatabase.kt` e `DatabaseModule.kt`: versão e registro da migration;
- este arquivo e overview 018.

## Critérios de aceite cobertos

1. Causa confirmada antes da edição.
2. Reimportação preserva `recordedAt` original.
3. Migration corrige somente `reviewedAt >= startedAt && reviewedAt < recordedAt`.
4. Candidate e quatro operações da atividade reimportada são resolvidos.
5. Operações e aggregates realmente inválidos continuam diagnosticados.
6. Nenhuma escrita ocorre durante consulta; migration executa somente na abertura
   do banco.
7. Validação completa possui evidência real, sem declarar testes não executados.

## Comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar, alterar ou executar testes automatizados. Executar smoke manual no
dispositivo, preservando banco existente, e registrar screenshot somente se a
interface for alterada (não esperado).

## Definição de pronto

- causa reproduzida e documentada sem dados sensíveis;
- menor correção coerente aplicada, ou bloqueio registrado se checkout já
  estiver correto;
- diff revisado e comandos executados com resultados;
- subtarefa e overview atualizados conforme status real;
- revisão independente solicitada.

## Riscos residuais

- outro aggregate global pode continuar gerando issue;
- APK anterior pode mascarar correção até reinstalação;
- custo linear da integridade global permanece se não for parte da causa;
- amostras e operações históricas não reproduzidas integralmente no dispositivo.
