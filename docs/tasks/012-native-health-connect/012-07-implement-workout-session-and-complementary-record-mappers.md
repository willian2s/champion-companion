# Task 012-07 - Implementar mapeadores de sessão de treino e records complementares

## Status

Concluída em 2026-07-24.

## Objetivo

Implementar os mapeadores de atividade para `ExerciseSessionRecord`, `DistanceRecord`, `TotalCaloriesBurnedRecord` e `HeartRateRecord`, respeitando ação explícita, granularidade e falha parcial.

## Contexto

As atividades em `READY_TO_SYNC` já existem localmente, com tipo confirmado, título, notas, distância calibrada e amostras reais de BPM. Falta transformá-las em records Health Connect independentes e idempotentes.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-03-add-health-connect-sdk-manifest-rationale-and-availability.md`
- `docs/tasks/012-native-health-connect/012-05-implement-sync-queue-reconciliation-and-repository-contracts.md`
- `docs/specs/011-activity-review-calibration.md`

## Dependências

- `012-03`
- `012-05`

## Entradas

- workflow local real de `WorkoutActivity`;
- contratos de fila já estáveis;
- política de `PARTIAL`, distância e amostras reais.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-07-implement-workout-session-and-complementary-record-mappers.md`
- arquivos Kotlin de mapeadores, gateway e infraestrutura Health Connect relacionados a atividade
- contratos mínimos estritamente necessários de domínio ou infra

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `SettingsScreen.kt`
- `HomeViewModel.kt`
- `HealthViewModel.kt`
- `AndroidManifest.xml`

## Escopo

- mapear sessão com tipo confirmado, título e observações;
- mapear distância final com prioridade calibrada > original > estimada;
- mapear calorias quando elegíveis;
- mapear BPM da atividade com amostras reais;
- não criar `StepsRecord` de atividade;
- não criar rota;
- tratar `PARTIAL` de forma segura.

## Fora do escopo

- coordinator global;
- UI;
- triggers explícitos de sync.

## Passos

1. Implementar mapeador de sessão com validações temporais.
2. Implementar mapeadores complementares independentes.
3. Garantir `clientRecordId` estável por record granular.
4. Tratar casos `PARTIAL` e ausência de dados mínimos.
5. Atualizar spec e overview.

## Critérios de aceite

- atividade gera operações independentes por record;
- sessão é obrigatória;
- distância, calorias e BPM só existem quando elegíveis;
- nenhum `StepsRecord` de atividade é criado;
- nenhum dado inventado é produzido para `PARTIAL`.

## Validação manual

- revisão do mapeamento de atividade e dos fluxos de ausência de dados.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- lista de records mapeados;
- lista de regras de ausência e fallback implementadas.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-07` como concluída;
- registrar granularidade de atividade estabilizada.

## Execução

### Arquivos atualizados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-07-implement-workout-session-and-complementary-record-mappers.md`

### Implementação concluída

- `HealthRecordMapper` passou a mapear `WorkoutActivity` para `ExerciseSessionRecord`, `DistanceRecord`, `TotalCaloriesBurnedRecord` e `HeartRateRecord` granulares.
- sessão exige janela temporal válida, tipo confirmado e workflow explícito elegível (`READY_TO_SYNC` ou `SYNC_FAILED`).
- distância usa prioridade calibrada > original > estimada; calorias exigem valor positivo; BPM usa apenas amostras reais dentro da sessão.
- nenhum `StepsRecord` de atividade e nenhuma rota foram introduzidos.
- `findEligibleWorkoutRecordTypes(...)` foi adicionado para que tasks seguintes criem operações apenas para complementos realmente elegíveis, inclusive em `PARTIAL`.

### Validação executada

- `./gradlew :app:compileDebugKotlin` concluído com sucesso.

### Observações

- `ParserValidity.PARTIAL` não inventa distância, calorias ou BPM ausentes.
- workflow explícito de sincronização da atividade continua para tasks posteriores da fase.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
