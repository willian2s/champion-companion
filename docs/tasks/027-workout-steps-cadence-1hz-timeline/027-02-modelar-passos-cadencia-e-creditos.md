# 027-02 — Modelar passos, cadência e créditos de atividade

- **Ticker:** `027`
- **Número:** `02`
- **Status:** `completed`
- **Depends on:** `027-01`

## Objetivo

Adicionar os contratos mínimos necessários para representar passos/cadência da atividade e o crédito idempotente contra o próximo Daily Summary, sem ainda alterar o comportamento remoto completo.

## Pré-condição

`027-01` concluída com ADR e política de cobertura/residual fechadas.

## Trabalho

### Projeções de atividade

Adicionar conceitos equivalentes a:

```text
WorkoutSteps
StepsCadenceSeries
```

na infraestrutura de projeções filhas, com:

- chave estável;
- tipo lógico correto;
- identidade determinística;
- versionamento monotônico;
- payload congelado suficiente para retry.

Não reutilizar `STEPS` de `DailySummary` de forma que colida identidade de agregado/filho.

### Crédito/cobertura

Persistir estado identificável por atividade e dia local, incluindo o mínimo necessário para distinguir:

- ainda não publicado;
- operação pendente/falhou;
- aceito pelo Health Connect e disponível como crédito;
- já consumido por incremento diário;
- atividade já coberta por Daily anterior;
- não elegível/fail-closed.

O modelo exato deve seguir o ADR de `027-01`.

### Room

Se o estado não couber de forma segura no schema v13:

- criar migration aditiva `13 -> 14`;
- atualizar schema exportado;
- não executar efeito remoto durante migration;
- não inferir créditos para atividades históricas automaticamente.

### Permissões

Preparar suporte explícito a `StepsCadenceRecord` e `StepsRecord` da atividade no domínio de permissões, sem solicitar permissão que não seja efetivamente usada.

## Invariantes

- uma atividade possui no máximo uma identidade de passos vigente por versão lógica;
- um aceite remoto de passos pode gerar no máximo um crédito;
- reprocessamento local não duplica crédito;
- `StepsCadenceRecord` não cria crédito;
- histórico existente permanece legível;
- sem leitura do Health Connect.

## Fora de escopo

- escrever records remotamente;
- alterar cálculo do delta diário;
- timeline 1 Hz;
- UI nova além do estritamente necessário para estado existente.

## Verificação

Se houver código/schema:

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutStepsContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectGateway.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsViewModel.kt`;
  - `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/14.json`.
- **Decisões:** filhos `WorkoutSteps` e `StepsCadenceSeries` usam identidades estáveis distintas do Daily; payload de retry usa schema 3; ledger separado persiste identidade, janela, fuso, publicação e consumo; migration aditiva `13 -> 14`; `WRITE_STEPS` é compartilhada pelo provider sem nova solicitação; cadência aceita valor zero e não gera crédito.
- **Desvios:** nenhum requisito funcional. Filhos novos não foram adicionados à lista de elegibilidade automática; round-trip de mapper e guards foram preparados sem iniciar fluxo remoto, conforme fora de escopo.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`; `./gradlew lint`; `./gradlew assembleDebug assembleRelease`. Não foram criados nem executados testes automatizados, conforme `AGENTS.md` e Spec 027.
- **Resultados e evidências:** todos os comandos finais passaram. Uma execução combinada de `./gradlew lint assembleDebug assembleRelease` falhou transitoriamente no lint por arquivo Hilt gerado ausente durante análise paralela; `./gradlew lint` isolado e os builds separados passaram. Schema 14 foi regenerado; revisão independente final aprovou sem bloqueadores.
- **Riscos residuais:** aceite remoto, ativação transacional do crédito, desconto no Daily, cobertura temporal e publicação efetiva dos filhos pertencem às subtarefas `027-03` a `027-05`; compatibilidade Health Sync/Strava permanece sem validação física; testes automatizados continuam fora do escopo.
