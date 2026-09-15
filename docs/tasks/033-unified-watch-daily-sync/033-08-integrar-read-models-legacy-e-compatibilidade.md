# 033-08 — Integrar read models, legacy e compatibilidade

- **Status:** completed
- **Depende de:** 033-04 e 033-07

## Objetivo

Fechar consumidores de Home, Saúde, Calendário, Perfil e Exercícios sobre a nova
fonte diária sem quebrar histórico.

## Escopo

- Home mostra FEA1 steps e métricas diárias medidas/completude;
- Saúde/Calendário usam composição D1/D4 local;
- HR histórico continua incluindo manual antigo, D1 e workout;
- métricas antigas continuam reproduzíveis;
- step length legacy permanece somente quando necessário a históricos/fallback;
- remover código de produto morto somente quando não for necessário a leitura ou
  migration;
- documentação/baseline de mapeamentos atualizada.

## Guard rails

- não inventar valor zero para lacuna;
- não apagar tabelas legacy apenas para reduzir código;
- não mudar semântica de snapshots antigos;
- não alterar workflow de workout já sincronizado.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Implementação

- Home passou a observar `WatchDailyReadModel`, mantendo FEA1 como total diário
  de passos e exibindo distância D1 medida somente quando disponível. Lacunas não
  geram cartões ou valores zero.
- Saúde passou a observar a composição local para o resumo do dia. Distância D1
  medida e completude são diferenciadas da estimativa legacy; histórico de FC
  existente continua usando manual, D1 e amostras de workouts.
- Calendário passou a incorporar o read model em presença mensal, detalhe diário e
  históricos. D1 granular preserva distância medida; DailySummary legacy continua
  sendo usado como fallback reproduzível quando não há fatos D1 elegíveis.
- `RoomRepositories` passou a reconhecer operações D1 de STEPS, DISTANCE e
  ACTIVE_CALORIES_BURNED na integridade legacy, incluindo validação da existência
  do fato D1 sem alterar writer, payload congelado ou workflow Health Connect.
- Perfil e Exercícios não receberam dependências artificiais: seus contratos
  legacy, snapshots e fluxos de workout permanecem preservados.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/decisions/adr-033-d1-granular-distance-active-calories.md`
- `docs/specs/033-unified-watch-daily-sync.md`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- `docs/tasks/033-unified-watch-daily-sync/033-09-validar-fisicamente-e-fechar.md`
- `docs/tasks/033-unified-watch-daily-sync/033-10-publicar-distancia-calorias-d1-granulares.md`
- este documento

## Decisões e desvios

- Campos legacy permaneceram nos contratos de Home, Saúde e Calendário; campos
  compostos foram adicionados de forma aditiva para não quebrar consumidores
  antigos.
- Distância D1 incompleta permanece ausente. Fallback para estimativa legacy só
  ocorre quando não há amostras D1 no dia e a fonte D1 não está indisponível; não
  há rateio, soma D1+D4 ou zero sintético.
- A compatibilidade de construção interna do `HealthViewModel` foi preservada
  com adapter somente de `DailySummary`, evitando quebrar consumidores antigos
  sem reintroduzir entrypoint de BLE.
- Quando não há relógio associado, a ausência de fatos D1 não é erro de leitura:
  o read model pode preservar resumo FEA1 legacy. Falha real da fonte D1 fica
  marcada e mantém distância/calorias ausentes, sem fallback enganoso.
- O helper legacy recebeu somente reconhecimento dos agregados já suportados pelo
  contrato granular. Writer granular, migrations, snapshots e sincronização
  remota não foram alterados.
- Não foram criados nem executados testes automatizados, conforme `AGENTS.md`.

## Comandos executados

```text
./gradlew app:compileDebugKotlin
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Resultados e evidências

- `./gradlew app:compileDebugKotlin` — `BUILD SUCCESSFUL`.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL` na
  validação final; debug e release compilados e lint concluído.
- `git diff --check` — sucesso, sem erros de whitespace.
- Nenhum teste automatizado foi criado ou executado.
- Evidência física específica de composição e compatibilidade permanece reservada
  à 033-09; esta task entrega validação técnica e leitura local integrada.

## Riscos residuais

- Validação física de composição D1/D4, partial success e idempotência ponta a
  ponta permanece na 033-09.
- Construtores internos legacy do `HealthViewModel` continuam aceitando somente
  `DailySummary`; isso é ponte de compatibilidade, não caminho de produção.
- Calendário mantém modelo de workout histórico separado do read model para
  preservar invariantes de data civil e contratos existentes.
