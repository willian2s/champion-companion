# Task 011-07 - Implementar workflow de calibração de esteira

## Status

Concluída em 2026-07-24.

## Objetivo

Implementar operação atômica de calibração para atividade importada, com próxima revisão, fallback sem distância-base positiva e avanço para `READY_TO_SYNC`.

## Contexto

`TreadmillCalibrationRepository` atual só registra uma calibração já construída e `TreadmillCalibration.forImportedActivity(...)` ainda exige `originalMeters > 0`. A fase precisa mover a lógica para um fluxo completo de produto.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-03-define-review-calibration-contracts-and-ui-state.md`
- `docs/tasks/011-activity-review-calibration/011-04-add-activity-review-fields-and-room-migration.md`

## Dependências

- `011-03`
- `011-04`

## Entradas

- contrato final de calibração;
- schema v4;
- estrutura atual de `treadmill_calibrations`.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/TreadmillCalibration.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-07-implement-treadmill-calibration-workflow.md`

## Arquivos proibidos

- `feature/exercises/**`
- `feature/home/**`
- `core/navigation/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- implementar `calibrateImportedActivity(input)` equivalente;
- calcular próxima revisão;
- suportar `original > 0`, `estimated > 0` e ausência de base positiva;
- persistir fator ou ausência de fator;
- atualizar atividade com métricas derivadas e `calibrationId`;
- expor observação da calibração ativa.

## Fora do escopo

- UI de calibração;
- Home;
- Health Connect.

## Passos

1. Evoluir domínio de `TreadmillCalibration` para suportar fallback sem base positiva.
2. Implementar operação atômica no repository.
3. Garantir `revision = latest + 1`.
4. Atualizar mappers e observação da calibração ativa.
5. Atualizar spec e overview.

## Critérios de aceite

- calibração só roda em `PENDING_CALIBRATION`;
- fator usa `original`, senão `estimated`, senão ausente;
- distância calibrada continua válida mesmo sem fator;
- operação persiste calibração e atividade no mesmo write;
- atividade termina em `READY_TO_SYNC`.

## Validação manual

- revisar fórmula, fallback e transação;
- conferir ausência de divisão por zero.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff do domínio de calibração;
- diff do repository;
- diff do mapper/DAO da calibração ativa.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-07` como concluída;
- registrar qualquer risco residual de revisão futura ou recalibração.

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-07-implement-treadmill-calibration-workflow.md`

### Resultado no repository

- `RoomRepositories.calibrateImportedActivity(input)` agora:
  - carrega atividade e import record;
  - reconstrói domínio da atividade;
  - valida `PENDING_CALIBRATION`;
  - consulta calibração mais recente;
  - calcula `revision = latest + 1`;
  - cria `TreadmillCalibration` pelo domínio;
  - aplica calibração à atividade no domínio;
  - persiste calibração e atividade no mesmo write;
  - retorna `TreadmillCalibrationSnapshot` da calibração ativa.

### Resultado funcional

- calibração só roda quando a atividade está em `PENDING_CALIBRATION`;
- fator usa `original`, senão `estimated`, senão ausente;
- distância calibrada continua válida mesmo sem fator;
- `READY_TO_SYNC` é gravado junto da atualização da atividade no mesmo write.

### Gate executado

- `./gradlew lint assembleDebug assembleRelease`
- resultado: sucesso.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: nenhuma UI, Home ou integração Health Connect foi implementada nesta task.
