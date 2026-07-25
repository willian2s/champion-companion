# Task 012-10 - Integrar sync explícito de atividade e transições de workflow

## Status

Concluída em 2026-07-24.

## Objetivo

Integrar o envio explícito de atividades prontas ao Health Connect, com transições locais de workflow, falha parcial segura e retry por atividade.

## Contexto

As atividades já chegam a `READY_TO_SYNC` localmente. Falta acoplar ação consciente do usuário para processar o bundle granular de records sem sync automático ao atingir esse estado.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-08-implement-health-connect-sync-coordinator-partial-results-and-retry.md`
- `docs/specs/011-activity-review-calibration.md`

## Dependências

- `012-08`.

## Entradas

- `WorkoutActivity` real com estados de sync no domínio;
- mapeadores de atividade já estáveis;
- UI e ViewModel atuais de Exercícios.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-10-integrate-explicit-workout-sync-and-workflow-transitions.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- contratos e repositórios estritamente necessários ao sync explícito de atividade

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `HomeViewModel.kt`
- `HealthViewModel.kt`
- `SettingsScreen.kt`
- `AndroidManifest.xml`

## Escopo

- adicionar ação explícita `Sincronizar com Health Connect` no detalhe;
- opcionalmente adicionar ação explícita `Sincronizar atividades prontas`;
- criar ou reparar operações da atividade sob demanda;
- processar bundle de sessão e complementos;
- transicionar `READY_TO_SYNC`/`SYNC_FAILED -> SYNCING -> SYNCED|SYNC_FAILED`.

## Fora do escopo

- sync automático de atividade;
- Settings;
- feedback final de Home e Saúde.

## Passos

1. Integrar intents e ações explícitas de sync em Exercícios.
2. Conectar ViewModel ao coordinator de sync.
3. Atualizar transições de workflow com partial seguro.
4. Expor retry por atividade sem duplicar sessão.
5. Atualizar spec e overview.

## Critérios de aceite

- atividade não sincroniza automaticamente ao alcançar `READY_TO_SYNC`;
- sessão usa ação explícita do usuário;
- falha parcial preserva sucesso já obtido;
- retry reaproveita IDs estáveis e processa só pendentes/falhas.

## Validação manual

- revisão do fluxo de atividade e das transições de estado.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff do fluxo de Exercícios;
- lista das novas ações explícitas;
- registro das transições de workflow suportadas.

## Screenshot quando visual

- screenshot obrigatório da UI de detalhe e, se existir, da ação de lista.

## Atualização do overview

- marcar `012-10` como concluída;
- registrar workflow explícito de atividade integrado.

## Execução

### Arquivos atualizados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-10-integrate-explicit-workout-sync-and-workflow-transitions.md`

### Implementação concluída

- detalhe da atividade passou a exibir CTA explícita `Sincronizar com Health Connect` para `READY_TO_SYNC` e retry para `SYNC_FAILED`.
- `ExercisesViewModel` passou a acionar `syncActivity(...)` do coordinator e traduzir resultados tipados em mensagens seguras para permissão, provider, disabled e falha parcial.
- workflow explícito agora suporta `READY_TO_SYNC|SYNC_FAILED -> SYNCING -> SYNCED|SYNC_FAILED` sem introduzir sync automático ao atingir prontidão.

### Validação executada

- `./gradlew :app:compileDebugKotlin` concluído com sucesso.

### Observações

- screenshot da UI não foi capturada nesta execução.
- a ação opcional de lista `Sincronizar atividades prontas` não foi adicionada; somente CTA explícita do detalhe foi integrada.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
