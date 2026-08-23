# 027-03 — Publicar passos da atividade no Health Connect

- **Ticker:** `027`
- **Número:** `03`
- **Status:** `completed`
- **Depends on:** `027-01`, `027-02`

## Objetivo

Adicionar `StepsRecord` como projeção da `WorkoutActivity` quando os passos ainda não estiverem cobertos por um Daily Summary previamente sincronizado.

## Pré-condições

- `027-01` e `027-02` concluídas;
- política de cobertura definida;
- persistência/identidade pronta.

## Elegibilidade

Uma atividade pode publicar passos somente quando:

- possui total de passos válido e positivo;
- janela temporal válida;
- tipo de atividade admite passos;
- não está descartada/inválida;
- ainda não possui projeção de passos aceita;
- não está coberta por Daily Summary já sincronizado segundo a política de `027-01`.

Se cobertura diária anterior existir, marcar estado local equivalente a `ALREADY_COVERED_BY_DAILY` e não escrever `StepsRecord`.

## Mapeamento

Criar `StepsRecord` com:

- `count` derivado do total de passos persistido da atividade;
- janela da atividade ou janela segura definida pelo ADR;
- metadata coerente com origem no relógio;
- `clientRecordId` estável;
- `clientRecordVersion` monotônico;
- payload congelado para retry.

## Crédito

O crédito não nasce ao enfileirar.

Fluxo obrigatório:

```text
operação pendente
  -> escrita remota
  -> Health Connect aceita
  -> operação local marcada como sucesso
  -> crédito identificado fica ativo
```

Se a escrita falhar antes do aceite, nenhum crédito pode reduzir o Daily.

Se o efeito remoto for aceito e ocorrer falha local posterior, preservar identidade para retry/reconciliação sem criar segundo record ou segundo crédito.

## Idempotência

- sincronizar a mesma atividade novamente não duplica passos;
- retry usa mesma identidade congelada;
- reimportação da mesma atividade não cria novo crédito sem nova versão semanticamente válida;
- passos históricos não são promovidos automaticamente.

## Fora de escopo

- descontar crédito do Daily Summary — `027-04`;
- `StepsCadenceRecord` — `027-05`;
- timeline 1 Hz.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não executar testes automatizados sem pedido explícito.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutStepsContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
  - este arquivo;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.
- **Decisões:** `WorkoutSteps` entra como filho independente; elegibilidade exige
  parser `VALID`, passos positivos, tipo não ciclismo, janela válida contida no
  dia local e cobertura Daily comprovadamente compatível. `ALREADY_COVERED` e
  `COVERAGE_UNKNOWN` permanecem bloqueados sem escrita remota. Crédito é ativado
  somente após `Accepted`, com operação `SYNCED` e checkpoint pós-efeito na mesma
  transação Room.
- **Desvios:** nenhum requisito funcional. A execução combinada inicial teve
  falha transitória do lint por arquivo Hilt gerado ausente; execução final do
  comando exigido passou.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint`; `./gradlew assembleDebug assembleRelease`; `./gradlew lint
  assembleDebug assembleRelease`. Não foram criados nem executados testes
  automatizados, conforme `AGENTS.md` e Spec 027.
- **Resultados e evidências:** compilação Kotlin, lint, assemble Debug, assemble
  Release e verificação combinada passaram. Revisão independente final aprovou
  sem bloqueadores. Cobertura Daily valida cadeia completa, fuso, data, payload e
  metadata antes de permitir `StepsRecord`.
- **Riscos residuais:** Daily ainda não consome créditos; isso pertence a
  `027-04`. Cadência pertence a `027-05`. Health Connect permanece write-only e
  sem readback; validação física Health Sync/Strava pertence a `027-06`. Não há
  testes automatizados por requisito explícito da Spec 027.
