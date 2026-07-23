# Task 010-12 - Resolver achados físicos e executar Gate B

## Status

Pendente.

## Objetivo

Corrigir achados reais da validação física D3/D4 e executar o Gate B final com treino curto, treino longo ou multipart e retomada válida.

## Contexto

Esta task existe para absorver divergências reais encontradas em `010-11`. O escopo exato depende dos achados físicos e não pode ser fechado sem nova rodada no relógio real.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-11-run-initial-d3-d4-physical-validation.md`
- `docs/protocol/workouts-d3-d4.md`

## Dependências

- `010-11`.

## Entradas

- lista de achados físicos reais;
- logs sanitizados e screenshots locais;
- hardware real ainda disponível para revalidação.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutBlockProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutImportSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/**`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-12-resolve-physical-findings-and-run-gate-b.md`
- `docs/protocol/workouts-d3-d4.md`

## Arquivos proibidos

- roadmap;
- ADRs, salvo decisão explícita do usuário em nova execução;
- fases concluídas não relacionadas.

## Escopo

- corrigir divergências reais encontradas em `010-11`;
- rerodar validação física necessária;
- executar Gate B final;
- atualizar protocolo e spec com fatos confirmados.

## Fora do escopo

- encerrar fase documentalmente;
- inventar comportamento sem nova evidência física.

## Passos

1. Priorizar achados físicos por impacto funcional.
2. Corrigir somente arquivos atingidos pelos achados.
3. Revalidar no relógio real após cada correção relevante.
4. Executar Gate B completo.
5. Atualizar protocolo, spec e overview com fatos confirmados.

## Critérios de aceite

- achados físicos críticos resolvidos;
- Gate B executado com sucesso;
- deduplicação, cancelamento, retomada e `PENDING_REVIEW` confirmados no hardware;
- protocolo D3/D4 atualizado só com fatos confirmados.

## Testes pequenos aplicáveis

- não criar nem ajustar testes nesta task sem pedido explícito do usuário.

## Validação mínima

- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew installDebug`

## Evidências

- nova rodada física registrada;
- outputs de build;
- screenshots relevantes;
- diffs do protocolo e da spec alinhados aos fatos observados.

## Atualização do overview

- marcar `010-12` como concluída ou bloqueada;
- registrar riscos aceitos e follow-ups restantes.

## Validação final

- confirmar que Gate B não foi dado como concluído sem hardware e treinos reais.
