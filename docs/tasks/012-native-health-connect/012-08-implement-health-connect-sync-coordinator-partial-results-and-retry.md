# Task 012-08 - Implementar coordinator de sync Health Connect, resultados parciais e retry

## Status

Planejada.

## Objetivo

Implementar o `HealthConnectSyncCoordinator` com processamento foreground limitado, resultados tipados, tratamento de falha parcial, retry e reparo de operações interrompidas.

## Contexto

Depois de estabilizar fila e mapeadores, a fase precisa de orquestração única para disponibilidade, permissão, upsert idempotente, transições de estado e workflow de atividade.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-06-implement-health-connect-gateway-and-steps-heart-profile-mappers.md`
- `docs/tasks/012-native-health-connect/012-07-implement-workout-session-and-complementary-record-mappers.md`

## Dependências

- `012-06`
- `012-07`

## Entradas

- gateway funcional;
- fila persistida;
- mapeadores simples e de atividade.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-08-implement-health-connect-sync-coordinator-partial-results-and-retry.md`
- arquivos Kotlin do coordinator, resultados de sync, safe errors, Hilt e infraestrutura diretamente relacionada
- arquivos de contratos estritamente necessários

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `SettingsScreen.kt`
- `HomeViewModel.kt`
- `HealthViewModel.kt`
- `ExercisesViewModel.kt`

## Escopo

- implementar `HealthConnectSyncCoordinator`;
- implementar resultados `Completed`, `CompletedWithFailures`, `NothingToSync`, `PermissionRequired`, `ProviderUnavailable`, `Disabled`, `Failed`;
- implementar limite foreground;
- implementar reset de `SYNCING` abandonado;
- implementar partial result para atividade;
- implementar retry somente de pendentes/falhas;
- não executar BLE.

## Fora do escopo

- triggers específicos de UI;
- surface de Settings;
- feedback final de Home, Saúde e Exercícios.

## Passos

1. Implementar tipos de resultado e erro seguro.
2. Implementar coordinator e limite de processamento.
3. Integrar permissões, disponibilidade e gateway.
4. Implementar reset de interrompidos e retry idempotente.
5. Atualizar workflow de atividade conforme sucesso integral ou parcial.
6. Atualizar spec e overview.

## Critérios de aceite

- coordinator processa backlog com limite foreground;
- `SYNCING` abandonado é reparado;
- resultado parcial não apaga records já sincronizados;
- retry reusa `clientRecordId` e processa somente pendentes/falhas.

## Validação manual

- revisão da orquestração e dos resultados seguros sem executar provider real.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- lista de resultados tipados;
- lista de safe errors;
- diff do coordinator.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-08` como concluída;
- registrar coordinator estabilizado e pronto para integrações de feature.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
