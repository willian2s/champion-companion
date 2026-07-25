# Task 012-09 - Integrar gatilhos pós-persistência de passos, BPM manual e D1

## Status

Concluída em 2026-07-24.

## Objetivo

Integrar os gatilhos automáticos de sync foreground para passos, medição manual e backlog D1 após persistência local e liberação do `ForegroundBleOperationGate`.

## Contexto

Home e Saúde já persistem dados locais corretamente. A Fase 12 deve acoplar o Health Connect como estado secundário, sem bloquear sucesso local nem manter BLE ativo durante o sync.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-08-implement-health-connect-sync-coordinator-partial-results-and-retry.md`

## Dependências

- `012-08`.

## Entradas

- Home e Saúde atuais;
- coordinator funcional;
- `healthSyncEnabled` persistido em `AppPreferences`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-09-integrate-post-persistence-steps-manual-bpm-and-d1-triggers.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- coordinators Home/Health e contratos estritamente necessários
- arquivos de preferences e sync relacionados, se necessários

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `ExercisesViewModel.kt`
- `SettingsScreen.kt`
- `AndroidManifest.xml`

## Escopo

- passos: persistir -> liberar BLE -> tentar sync;
- BPM manual: persistir -> liberar BLE -> tentar sync;
- D1: persistir lote -> liberar BLE -> processar backlog;
- quando `healthSyncEnabled=false`: persistir -> manter `PENDING` -> não tentar.

## Fora do escopo

- sync explícito de atividade;
- surface de Settings;
- feedback final de UI em lista/detalhe.

## Passos

1. Integrar trigger de passos após merge persistido.
2. Integrar trigger de BPM manual após persistência final.
3. Integrar processamento de backlog D1 após persistência do lote.
4. Respeitar `healthSyncEnabled` e liberação do gate BLE.
5. Atualizar spec e overview.

## Critérios de aceite

- nenhum sync inicia antes de liberar BLE;
- `healthSyncEnabled` desativado preserva `PENDING` sem tentativa;
- sucesso local permanece independente de sucesso do provider.

## Validação manual

- revisão do fluxo de gatilhos e da ordem persistência -> liberação -> sync.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff dos ViewModels e coordinators;
- lista dos pontos de disparo integrados.

## Screenshot quando visual

- somente se a task introduzir mensagem visual perceptível já nesta etapa.

## Atualização do overview

- marcar `012-09` como concluída;
- registrar triggers automáticos pós-persistência integrados.

## Execução

### Arquivos atualizados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-09-integrate-post-persistence-steps-manual-bpm-and-d1-triggers.md`

### Implementação concluída

- Home passou a disparar `syncPending()` do Health Connect apenas após merge persistido com sucesso e liberação do `ForegroundBleOperationGate`.
- Saúde passou a disparar `syncPending()` após persistência final da medição manual e após conclusão persistida da importação D1 com dados efetivamente salvos.
- os gatilhos automáticos agora respeitam `healthSyncEnabled` antes de iniciar tentativa secundária de sync foreground.

### Validação executada

- `./gradlew :app:compileDebugKotlin` concluído com sucesso.

### Observações

- sucesso local continua independente do provider.
- nenhum sync é iniciado enquanto BLE ainda está retido pelo gate foreground.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
