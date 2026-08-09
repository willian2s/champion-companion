# 014-02 — Definir contratos canônicos e temporais

- **Ticker:** `014`
- **Número:** `02`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Criar contratos Android-free que separem raw, measured, snapshot derivado,
elegibilidade temporal, plausibilidade, métricas, splits e identidade conceitual
de projeções filhas.

Resultado: domínio compilável e decisão fechada para a migration local de
`014-03`, sem dependência de Room, Compose, BLE Android ou Health Connect SDK.

## Dependências

- `014-01` concluída e revisada.

## Arquivos permitidos

- `core/model/WatchWorkoutActivity.kt`;
- `core/model/DeviceProfileDailySummary.kt`;
- `core/model/HeartRateAndSync.kt`;
- `core/model/RepositoryContracts.kt`;
- novos arquivos em `core/model`;
- spec, ADR-018 e overview.

Símbolos prováveis:

- `StepLengthSource`;
- `WorkoutDerivationMethod`;
- `MeasuredWorkoutInterval`;
- `WorkoutDerivationSnapshot`;
- `DerivedWorkoutInterval`;
- `TemporalSeriesEligibility` e motivos tipados;
- `WorkoutSeriesPlausibility`;
- `WorkoutSplit`;
- `HealthProjectionChildKey` conceitual;
- `HealthSyncRecordType.SPEED` sem habilitar writer.

## Arquivos proibidos

- `core/bluetooth`, `core/database`, `data`, `feature` e `core/healthconnect`;
- Gradle, manifesto, migrations, schemas e Fase 013.

## Decisões obrigatórias

### Série e distância

```text
canonicalIntervalDistance = decodedSteps × stepLengthMetersUsed
canonicalDistance = soma exata dos intervalos canônicos
```

- Série inelegível implica distância canônica ausente.
- `totalSteps × stepLengthMetersUsed` é somente estimativa agregada diagnóstica.
- Original, calibrada histórica e raw temporal não são fallback.

### Índices

Série V1 exige índices únicos e contínuos `0..N-1`. Lacuna, duplicata, primeiro
índice não zero ou ordem inconsistente recebe motivo explícito de inelegibilidade.

### Duração e plausibilidade

- Modelar os quatro casos `T/N/60 s` da spec.
- Modelar `LAST_INTERVAL_RESIDUAL` separadamente de plausibilidade.
- Definir motivos tipados, incluindo cadência/velocidade implausível.
- Registrar limiar adotado ou manter política inelegível sem limiar até evidência;
  não esconder a decisão em código posterior.

### BPM por split

- Janelas semiabertas `[start, end)`.
- Sample-and-hold: amostra `i` cobre `[sampledAt_i, sampledAt_i+1)`, limitada
  pela sessão.
- Média ponderada pela duração de sobreposição.

### SpeedRecord

A política estável é ponto médio do intervalo:

```text
sampledAt = intervalStart + effectiveDuration / 2
```

### Esteira

Para `WATCH_D3_D4`, `TREADMILL` pode avançar para `READY_TO_SYNC` quando existir
série canônica elegível, sem exigir calibração histórica. Contratos phone
permanecem legados.

### Projeções filhas

Definir conceitualmente uma operação por filho:

```text
aggregate + recordType + projectionChildKey
```

Não desenhar schema ainda, mas impedir contrato que volte a uma operação com
subfila interna de vários records.

## Passos de implementação

1. Definir raw/measured/derived sem reutilizar nomes legados com nova semântica. **Concluído.**
2. Modelar `stepLengthMeters` positivo, origem e snapshot imutável. **Concluído.**
3. Modelar série canônica única e estimativa diagnóstica separada. **Concluído.**
4. Modelar índices, quatro casos temporais e plausibilidade. **Concluído.**
5. Modelar métricas por intervalo, acumulado e split. **Concluído.**
6. Formalizar BPM temporal e ponto médio da velocidade. **Concluído.**
7. Formalizar workflow `TREADMILL` para origem relógio. **Concluído.**
8. Adicionar `SPEED` ao enum sem tornar atividade elegível antes de `014-13`. **Concluído.**
9. Modelar chave conceitual de filho Health Connect. **Concluído.**
10. Manter adapters compatíveis para consumidores ainda não migrados. **Concluído.**

## Critérios de aceite

- Contratos representam todas as decisões sem tipos Android.
- Não existe API que permita duas distâncias canônicas.
- Estimativa agregada é nomeada como diagnóstica.
- Lacunas e duplicatas são representáveis sem exceção silenciosa.
- Plausibilidade não modifica duração nem passos.
- BPM temporal e midpoint de velocidade são determinísticos.
- Workflow de esteira diferencia `WATCH_D3_D4` de phone histórico.
- `SPEED` não habilita writer prematuramente.
- A chave conceitual suporta uma operação por filho.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela requisito → tipo/invariante;
- matriz de elegibilidade e plausibilidade;
- exemplos de split/BPM e midpoint;
- resultado dos builds e revisão de arquitetura.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`.

### Requisitos → tipos e invariantes

| Requisito | Contrato/invariante |
| --- | --- |
| Raw/measured/derived | `RawWorkoutInterval`, `MeasuredWorkoutInterval`, `DerivedWorkoutInterval` e `WorkoutDerivationSnapshot`; ponte `d4-v1` não preenche `decodedSteps`. |
| Comprimento individual do passo | `StepLengthProfile` exige valor finito e positivo, registra `StepLengthSource` e `updatedAt`. |
| Distância única | `CanonicalWorkoutSeries` calcula distância somente pela soma de `decodedSteps × stepLengthMetersUsed`; `DiagnosticAggregateStepEstimate` é explicitamente diagnóstico. |
| Índices | `TemporalSeriesValidator` representa vazio, primeiro índice diferente de zero, ordem, duplicata e lacuna com motivos tipados. |
| Duração | `WorkoutDurationResolver` modela `EXACT`, `LAST_INTERVAL_RESIDUAL`, `INSUFFICIENT_TOTAL_DURATION` e `EXCESS_TOTAL_DURATION`; cauda excedente fica em `tailUncovered`. |
| Plausibilidade | `WorkoutSeriesPlausibility` separa indicadores de cadência/velocidade da duração e bloqueia projeção sem política explicitamente aceita. |
| Métricas | `WorkoutIntervalMetrics` e `WorkoutCumulativeMetrics` preservam cadência, velocidade, ritmo, passos acumulados e duração coberta; zero passos não inventa ritmo. |
| Splits e BPM | `WorkoutSplitCalculator` usa interpolação linear, janelas `[start, end)` e média BPM ponderada pela sobreposição sample-and-hold. |
| Velocidade | `DerivedWorkoutInterval.speedSampledAt` usa ponto médio da janela efetiva. |
| Esteira | `WatchWorkoutWorkflowPolicy` libera somente `WATCH_D3_D4` com snapshot funcional elegível; contrato legado phone permanece inalterado. |
| Filhos Health Connect | `HealthProjectionChildKey` e `HealthSyncOperationKey` representam `aggregate + recordType + projectionChildKey`; `SPEED` existe no enum, mas branch do writer falha fechado. |

### Matriz de elegibilidade

| Situação | Série canônica | Projeção funcional/remota |
| --- | --- | --- |
| Índices contínuos, duração válida, passos decodificados, política aceita | elegível | elegível |
| Lacuna, duplicata, primeiro índice não zero ou ordem inconsistente | ausente | inelegível |
| `T <= (N-1) × 60 s` | ausente, motivo `INSUFFICIENT_TOTAL_DURATION` | inelegível |
| `T == N × 60 s` | elegível temporalmente | depende de plausibilidade |
| `(N-1) × 60 s < T < N × 60 s` | último intervalo residual positivo | depende de plausibilidade |
| `T > N × 60 s` | intervalos cobrem `N × 60 s`; cauda em `tailUncovered` | depende de plausibilidade |
| `d4-v1` sem `decodedSteps` | ausente, motivo `MISSING_DECODED_STEPS` | inelegível |
| Indicador `IMPLAUSIBLE_CADENCE` ou `IMPLAUSIBLE_SPEED` | preservada para diagnóstico | bloqueada |
| `TREADMILL` + origem `WATCH_D3_D4` + snapshot elegível | preservada | pode usar `READY_TO_SYNC` pela política nova |
| `TREADMILL` phone histórico | contrato legado | exige calibração histórica |

### Exemplos determinísticos

- `T = 1.637 s`, `N = 28`: 27 janelas de 60 s e última janela de 17 s;
- limite de split em 1.000 m dentro de um intervalo usa fração linear da distância e da duração daquele intervalo;
- amostra BPM em `t0` cobre `[t0, t1)` e a média do split é `Σ(bpm × sobreposição) / Σ(sobreposição)`;
- intervalo `[10:00, 10:17)` produz `SpeedRecord` conceitual em `10:08.5`;
- `totalSteps × stepLengthMetersUsed` permanece `DiagnosticAggregateStepEstimate`, nunca distância canônica.

### Validação e revisão

| Comando | Resultado |
| --- | --- |
| `./gradlew app:compileDebugKotlin` | PASS |
| `./gradlew lint assembleDebug assembleRelease` | PASS; warnings preexistentes de override deprecated em `data/RoomRepositories.kt` |
| `git diff --check` | PASS |
| Testes automatizados | Não criados nem executados, conforme task e `AGENTS.md` |
| Revisão independente | PASS; risco residual P2: snapshot ainda não recebe origem diretamente |

### Decisões e desvios

- O workflow legado de `WorkoutActivity.confirmReview` não foi alterado antes da migration/consumidores canônicos; `WatchWorkoutWorkflowPolicy` fecha a regra nova sem permitir que mapper antigo envie distância não canônica.
- A política padrão de plausibilidade é inelegível sem limiar auditado. Projeção exige `EXPLICITLY_ACCEPTED` e ausência de indicadores.
- `HealthSyncRecordType.SPEED` foi adicionado. Uma guarda fail-closed em `core/healthconnect/HealthConnectSyncCoordinator.kt` mantém o `when` exaustivo sem habilitar writer; esse arquivo está fora da lista original de permitidos por necessidade de compilação e fica como desvio explícito para revisão.

## Definição de pronto

Contratos compilam, schema lógico local está fechado para `014-03` e item
`014-02` está marcado no overview.

## Rollback

Remover novos contratos/adapters e restaurar enum sem `SPEED`; sem efeito em
dados persistidos.
