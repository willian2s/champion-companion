# 033-10 — Publicar distância e calorias D1 granulares no Health Connect

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADR:** `docs/decisions/adr-033-d1-granular-distance-active-calories.md`
- **Status:** `completed` — implementação, validação técnica e validação física concluídas
- **Dependências:** 033-03, 033-04, 033-05, 033-06; ADR-031

## Objetivo

Estender o cutover granular D1 já validado para publicar:

- `DISTANCE` como `DistanceRecord`;
- calorias D1 como `ActiveCaloriesBurnedRecord`;

preservando ownership D4 dentro de workouts, sem dupla contagem e sem rateio de
buckets de fronteira.

## Contrato

```text
sample D1 em t
=> bucket [t - 5 min, t)

STEPS > 0
  -> StepsRecord (contrato ADR-031)

DISTANCE > 0
  + sem overlap com D4 de distância positiva
    -> DistanceRecord
  + overlap total ou parcial com D4 de distância positiva
    -> fail-closed para DISTANCE D1

CALORIES > 0
  + sem overlap com D4 de calorias positivas
    -> ActiveCaloriesBurnedRecord
  + overlap total ou parcial com D4 de calorias positivas
    -> fail-closed para calorias D1
```

D4 com valor zero/ausente para a métrica **não** suprime D1 válido.

## Semântica de calorias

Dumps físicos de 2026-09-14 mostraram buckets parados em `0 kcal` e energia D1
acompanhando a energia observada no workout. O contrato D1 passa a classificar
`caloriesRaw / 10.0` como energia ativa para Health Connect.

A família D4 já existente permanece `TOTAL_CALORIES_BURNED` nesta task. Migrá-la
para `ACTIVE_CALORIES_BURNED` é decisão futura independente, porque atividades já
sincronizadas não podem receber uma segunda família remota sem reconciliação
explícita.

## Implementação realizada

- [x] adicionar `HealthSyncRecordType.ACTIVE_CALORIES_BURNED`;
- [x] adicionar `WRITE_ACTIVE_CALORIES_BURNED` ao manifesto/gateway;
- [x] mapear delete/permission para `ActiveCaloriesBurnedRecord`;
- [x] introduzir payload congelado D1 schema v5 para distância/calorias;
- [x] preservar schema v4 de STEPS sem alteração;
- [x] usar identidade estável por `sampleId + recordType`;
- [x] mapear D1 `DISTANCE` para `DistanceRecord`;
- [x] mapear D1 calorias para `ActiveCaloriesBurnedRecord`;
- [x] preservar janela exata `[t-5min,t)` e offsets congelados;
- [x] preparar somente valores positivos e finitos;
- [x] suprimir D1 por métrica quando bucket toca D4 positivo da mesma métrica;
- [x] não suprimir D1 quando D4 tem métrica zero/ausente;
- [x] não ratear bucket de fronteira;
- [x] esconder da contagem acionável D1 PENDING/FAILED que passou a ser coberto por D4 positivo;
- [x] fail-close de primeira projeção D4 quando já existe D1 `SYNCED` sobreposto da mesma métrica;
- [x] solicitar ACTIVE + TOTAL no grupo de calorias enquanto D1/D4 usam famílias distintas;
- [x] preservar sync explícito, write-only e ausência de auto-sync no pull da Home;
- [x] nenhuma migration Room;
- [x] nenhuma alteração na política de WorkoutSteps/StepsCadence.

### Correção de distância nativa D4 v4 — 2026-09-14

- `FREE_WORKOUT` preserva `WATCH_NATIVE` quando header e série são íntegros;
- `TREADMILL` usa distância nativa, permanece `PENDING_CALIBRATION` e só fica
  pronto após distância externa válida;
- calibração de `TREADMILL` usa fator `referência / distância D4` por intervalo e
  perfil contextual próprio;
- calibração por comprimento de passo permanece restrita a `RUNNING` e
  `WALKING`;
- snapshots nativos são reidratados após confirmação de `FREE_WORKOUT` ou
  `TREADMILL`, sem reescrever fatos raw ou atividades `SYNCED`;
- workflow temporário de automação foi removido; não existe escrita automática
  adicional.

## Compatibilidade concluída em 033-08

`RoomRepositories.hasSupportedHealthSyncPair()` permanece helper legado, mas agora
reconhece `WATCH_DAILY_D1_SAMPLE` para `STEPS`, `DISTANCE` e
`ACTIVE_CALORIES_BURNED` na leitura de integridade. O writer/queue efetivo continua
sendo `GranularHealthSyncRepository`; nenhuma superfície de escrita remota foi
alterada.

Os read models legados também reconhecem fatos D1 existentes sem substituir
DailySummary histórico, evitando falso `hasInvalidOperation` sem ampliar a
superfície de escrita remota.

## Validação técnica obrigatória

Executar localmente, sem criar/executar testes automatizados:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Validação local concluída em 2026-09-14:

- `lint`, `assembleDebug` e `assembleRelease`: **BUILD SUCCESSFUL**;
- `git diff --check`: sem saída/erros;
- warnings observados permaneceram não bloqueantes e fora do escopo desta task.

Validação local após a correção de política nativa, em 2026-09-14:

- `./gradlew lint assembleDebug assembleRelease`: **BUILD SUCCESSFUL**;
- `git diff --check`: sem saída/erros.

Validação final após os guards de workflow e bloqueio de fallback por step length
em D4 v4 inelegível de `FREE_WORKOUT`/`TREADMILL`, em 2026-09-14:

- `./gradlew lint assembleDebug assembleRelease`: **BUILD SUCCESSFUL**;
- `git diff --check`: sem saída/erros;
- warnings Kotlin de overrides deprecated permaneceram não bloqueantes.

Validação física da nova política `FREE_WORKOUT`/`TREADMILL` e dos dois cenários
normais de D1/D4 foi concluída em 2026-09-14. Aceite global concluído; o caso
D4 zero/ausente foi reclassificado como contrato técnico, sem exigência de
reprodução física artificial.

## Registro da evolução de política — 2026-09-14

- **Status:** `completed`; política física e aceite global concluídos.
- **Arquivos alterados:** `WatchWorkoutDerivedMetrics.kt`, `WatchWorkoutActivity.kt`,
  `DistanceCalibrationEvidence.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt`,
  `ExercisesScreen.kt`, `ExercisesViewModel.kt`, ADR-029, Spec 032, este arquivo e
  `033-00-overview.md`, além de
  `docs/integration/033-10-native-distance-policy-physical-validation.md`. O workflow temporário
  `.github/workflows/apply-033-native-distance-policy.yml` foi removido.
- **Decisões:** `FREE_WORKOUT` preserva `WATCH_NATIVE`; `TREADMILL` usa série D4
  nativa, exige referência externa e aplica fator proporcional por intervalo;
  perfis nativos de `RUNNING`, `WALKING` e `TREADMILL` permanecem separados;
  calibração por comprimento de passo continua somente em `RUNNING`/`WALKING`;
  D4 v4 inelegível não cai silenciosamente em step length para `FREE_WORKOUT` ou
  `TREADMILL`.
- **Desvios:** nenhum requisito funcional alterado. TREADMILL não resolve fator
  aprendido de `RUNNING`/`WALKING`; pode consultar somente o fator aprendido de
  TREADMILL, mas a revisão sempre permanece `PENDING_CALIBRATION` até a distância
  final da atividade ser informada. A aplicação explícita da referência gera
  `WATCH_NATIVE_CONTEXT_CALIBRATED` e libera `READY_TO_SYNC`. O fator contextual
  próprio continua persistido e observado para o contexto TREADMILL.
- A revisão FREE_WORKOUT força nova derivação quando snapshot ativo não é nativo;
  D4 v4 inelegível falha fechado, sem preservar fallback por step length.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`.
- **Resultados e evidências:** `git diff --check` sem saída; Gradle terminou com
  `BUILD SUCCESSFUL`; revisão estática confirmou os guards nativos, o pipeline
  explícito TREADMILL, o isolamento dos conjuntos de calibração e a preservação
  do caminho CYCLING. Não houve novo teste automatizado nesta passagem.
- **Correção durante validação:** uma execução intermediária falhou por nulidade
  em `RoomRepositories.kt:1458` ao bloquear o caminho legacy de calibração; o
  import record passou a ser validado antes da chamada nativa e a execução final
  passou integralmente.
- **Riscos residuais:** nenhum bloqueador de aceite. O guard técnico que mantém
  D1 elegível quando D4 da mesma métrica é zero/ausente permanece protegido no
  código; parser/firmware pode produzir esse cenário excepcional, mas sua
  reprodução física não é requisito. Fluxos FREE_WORKOUT/TREADMILL,
  idempotência e overlap por métrica estão cobertos em
  `docs/integration/033-10-native-distance-policy-physical-validation.md`.

### Validação física concluída da nova política — 2026-09-14

Validação física informada para esta passagem confirmou revisão de um treino
`FREE_WORKOUT` com:

```text
parser: d4-v4 válido
steps: 173
native distance: 140 m
calories: aproximadamente 9.169 kcal
```

Após confirmar o tipo, a distância nativa foi preservada como `WATCH_NATIVE`,
com distância canônica positiva e sem encaminhamento para `PENDING_CALIBRATION`.
Essa evidência fecha o cenário FREE_WORKOUT. O dump TREADMILL, os detalhes de
calibração, publicação, overlap e idempotência estão registrados em
`docs/integration/033-10-native-distance-policy-physical-validation.md`.
Aceite global da 033-10 está concluído; D4 zero/ausente permanece como guard
técnico, e a reprodução física desse caso contraditório ou excepcional não é
exigida para aceite.

### Reclassificação do critério D4 zero/ausente — 2026-09-14

D1 e D4 vêm da mesma atividade e fonte do relógio. D1 agrega aproximadamente
cinco minutos, enquanto D4 fornece intervalos de aproximadamente um minuto.
Exigir simultaneamente D1 positivo, cobertura D4 do mesmo bucket e todos os
intervalos D4 da mesma métrica zero/ausentes é cenário contraditório ou
excepcional, dependente de inconsistência de parser, firmware ou fonte.

O comportamento continua obrigatório como contrato técnico: D4 zero/ausente
não suprime D1 válido. O aceite físico fica baseado nos dois cenários normais já
observados: D1 positivo sem D4 positivo sobreposto é publicado; D1 positivo
com qualquer D4 positivo sobreposto é suprimido por métrica, inclusive overlap
parcial.

## Validação física registrada

### 1. Pré-sync remoto

1. realizar o workout no Champion C001;
2. executar pull-to-refresh na Home;
3. **não** tocar no sync Health Connect ainda;
4. extrair dump sanitizado do Room.

Verificar:

- D1 estruturalmente completo para o `totalPositions` atual;
- buckets continuam em 300 s;
- operações D1 `DISTANCE`/`ACTIVE_CALORIES_BURNED` existem apenas para valores
  positivos elegíveis;
- não há operações D1 dessas métricas nos buckets que intersectam D4 positivo da
  mesma métrica;
- bucket que intersecta D4 zero/ausente continua elegível se D1 for positivo;
- nenhum novo D1 já aparece `SYNCED` antes da ação explícita;
- STEPS granular permanece sem regressão;
- nenhum residual `FEA1 - ΣD1` é materializado.

### 2. Sync explícito

Usar o botão de Health Connect da Home ou `Configurações -> Sincronizar agora`.
Se o provider solicitar `calorias ativas`, conceder a nova permissão.

### 3. Pós-sync

Extrair novo dump e verificar:

```text
WATCH_DAILY_D1_SAMPLE / DISTANCE                -> SYNCED onde elegível
WATCH_DAILY_D1_SAMPLE / ACTIVE_CALORIES_BURNED -> SYNCED onde elegível
FAILED                                          -> 0 esperado
WorkoutSteps                                    -> BLOCKED/ALREADY_COVERED quando aplicável
pendências acionáveis                           -> 0 esperado após drenar fila
```

Também confirmar:

- D4 `DISTANCE` continua somente no workout;
- D4 `TOTAL_CALORIES_BURNED` continua no contrato histórico da atividade;
- nenhuma operação D1 sincronizada sobrepõe D4 positivo da mesma métrica na
  ordem normal do fluxo;
- `PENDING_REVIEW`/`PENDING_CALIBRATION` continuam sem primeira projeção remota.

### 4. Idempotência

Executar `Sincronizar com Health Connect` novamente sem novos dados.

Esperado:

- nenhum novo `clientRecordId` para o mesmo `sampleId + recordType`;
- nenhuma segunda operação remota equivalente;
- contadores permanecem estáveis;
- zero pendências acionáveis.

## Aceite

A task foi marcada `completed` após:

- [x] `lint`, `assembleDebug`, `assembleRelease` e `git diff --check` passarem;
- [x] dump pré-sync confirmar preparação e supressão por métrica;
- [x] dump pós-sync confirmar `DISTANCE` e `ACTIVE_CALORIES_BURNED` D1 sincronizados;
- [x] não houver duplicação D1/D4 observável na ordem normal;
- [x] D4 zero não suprimir D1 válido — contrato técnico protegido; reprodução
  física artificial não exigida;
- [x] STEPS/WorkoutSteps permanecerem sem regressão;
- [x] segunda sincronização for idempotente;
- [x] evidência sanitizada for registrada em `docs/integration/`.

## Fora do escopo

- migrar D4 `TOTAL_CALORIES_BURNED` para energia ativa;
- dividir proporcionalmente buckets de fronteira;
- readback do Health Connect;
- corrigir dados históricos remotos já publicados;
- alterar writer granular ou reescrever históricos além da compatibilidade entregue
  pela 033-08;
- criar ou executar testes automatizados.
