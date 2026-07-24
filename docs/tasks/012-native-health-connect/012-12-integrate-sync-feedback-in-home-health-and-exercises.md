# Task 012-12 - Integrar feedback de sync em Home, Saúde e Exercícios

## Status

Planejada.

## Objetivo

Expor feedback secundário de Health Connect em Home, Saúde e Exercícios, sem transformar sucesso local em falha principal quando o provider falhar.

## Contexto

Depois dos triggers e do sync explícito, a fase precisa fechar a experiência de produto mostrando pendentes, falhas e sincronização parcial de forma segura e subordinada ao dado local.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-09-integrate-post-persistence-steps-manual-bpm-and-d1-triggers.md`
- `docs/tasks/012-native-health-connect/012-10-integrate-explicit-workout-sync-and-workflow-transitions.md`
- `docs/tasks/012-native-health-connect/012-11-create-health-connect-settings-permissions-and-manage-access-ui.md`

## Dependências

- `012-09`
- `012-10`
- `012-11`

## Entradas

- triggers de passos/manual/D1;
- sync explícito de atividade;
- surface funcional de Settings.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-12-integrate-sync-feedback-in-home-health-and-exercises.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `AndroidManifest.xml`
- `ChampionDatabase.kt`
- `ChampionDaos.kt`

## Escopo

- Home: mostrar sync separado do refresh local;
- Saúde: mostrar sync como estado secundário após manual e D1;
- Exercícios: mostrar status resumido na lista e status detalhado no detalhe;
- expor falha parcial com linguagem segura.

## Fora do escopo

- novas coletas BLE;
- alteração da arquitetura do coordinator;
- nova tela além das já existentes.

## Passos

1. Integrar resumo de fila/sync nas features.
2. Ajustar mensagens e estados para não derrubar sucesso local.
3. Expor falha parcial e retry onde fizer sentido.
4. Atualizar spec e overview.

## Critérios de aceite

- Home não confunde refresh local com sync HC;
- Saúde mantém sucesso local como mensagem principal;
- Exercícios mostra `READY_TO_SYNC`, `SYNCING`, `SYNCED` e `SYNC_FAILED` com segurança;
- falhas do provider não rebaixam resultado local já persistido.

## Validação manual

- inspeção visual dos estados e mensagens nas três features.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff das features;
- lista dos estados visuais adicionados;
- screenshots das superfícies alteradas.

## Screenshot quando visual

- screenshot obrigatório.

## Atualização do overview

- marcar `012-12` como concluída;
- registrar feedback de sync integrado nas features.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
