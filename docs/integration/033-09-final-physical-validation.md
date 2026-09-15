# 033-09 — Validação física final e fechamento

- **Data:** 2026-09-15
- **Dispositivo:** Champion C001 real, validado pelo app no `SM-S901E`
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Privacidade:** evidência sanitizada; sem payload BLE integral, MAC, IDs pessoais ou BPM individual

## Validação técnica

```text
./gradlew lint assembleDebug assembleRelease  -> BUILD SUCCESSFUL
./gradlew installDebug                       -> instalado em 1 dispositivo
git diff --check                             -> sem erros
```

Nenhum teste automatizado foi criado ou executado, conforme `AGENTS.md` e a
restrição explícita da task.

O dump foi obtido após `adb shell am force-stop` com cópia temporária de
`champion-companion.db`, `-wal` e `-shm` via `adb exec-out run-as`; consultas
`sqlite3` registraram somente `PRAGMA user_version`, contagens, somas,
continuidade de posições e estados agregados. Arquivos temporários foram
descartados e não entram no repositório.

## Dumps antes e depois do sync explícito

Antes de acionar o sync explícito, o snapshot local sanitizado mostrou:

```text
Room:                         user_version 19
FEA1 2026-09-15:              2488 passos
D1 2026-09-15:                184 samples, posições 1..184, Σ 2466 passos
Fila acionável:              4 WORKOUT_ACTIVITY/STEPS PENDING
Créditos WorkoutSteps:       4 BLOCKED
```

Os quatro itens `WORKOUT_ACTIVITY/STEPS` estavam pendentes somente como
operações técnicas; os créditos já estavam `BLOCKED/ALREADY_COVERED`, portanto
não havia trabalho remoto acionável para eles.

Após Home `Sincronizar com Health Connect`, o snapshot permaneceu:

```text
Fila não resolvida:           4 WORKOUT_ACTIVITY/STEPS PENDING
Créditos WorkoutSteps:       4 BLOCKED
D1 STEPS:                    74 SYNCED
D1 DISTANCE:                 63 SYNCED
D1 ACTIVE_CALORIES_BURNED:  63 SYNCED
```

Não surgiram linhas duplicadas. Como não havia operação elegível nova no
snapshot pré-sync, essa rodada observou um no-op explícito; a escrita de fila
elegível e a preparação de `READY_TO_SYNC` permanecem comprovadas pela
revalidação de `033-06`.

## Estado físico após refreshes

O app foi instalado no dispositivo real com dados locais preservados. A Home
exibiu `Relógio: Conectado`. O primeiro pull-to-refresh concluiu a leitura local
com os três componentes da linha unificada e mostrou, de forma sanitizada:

```text
FEA1 exibido:       2466 passos
distância exibida:  1,9 km
completude:         completa
```

Um segundo refresh manteve os fatos já persistidos, atualizou o agregado FEA1 e
exibiu o estado seguro de partial success:

```text
FEA1 persistido:    2488 passos
Σ D1.steps:         2466 passos
diferença:          22 passos
estado visual:      cauda recente + sincronização parcial
```

O banco exportado após interromper o processo confirmou Room `user_version = 19`
e, para `2026-09-15`:

```text
D1 samples:         184
posições:            1..184
totalPositions:      184
Σ D1.steps:          2466
Σ D1.distance:       1863,0 m
Σ D1.calories:       121,8 kcal
BPM ausente:         129 samples
dayOffset observado: 0
```

BPM ausente não impediu a persistência das demais métricas. A diferença positiva
FEA1 × D1, com estrutura completa, foi classificada como cauda recente; nenhuma
operação de `STEPS` apontou para agregado D1 inexistente, portanto não houve
residual sintético.

## Projeção e idempotência

O dump sanitizado local confirmou:

```text
WATCH_DAILY_D1_SAMPLE / STEPS                    74 SYNCED
WATCH_DAILY_D1_SAMPLE / DISTANCE                63 SYNCED
WATCH_DAILY_D1_SAMPLE / ACTIVE_CALORIES_BURNED  63 SYNCED
WORKOUT_ACTIVITY / STEPS                         4 PENDING
WORKOUT_ACTIVITY / EXERCISE_SESSION              4 SYNCED
WORKOUT_ACTIVITY / DISTANCE                       66 SYNCED
WORKOUT_ACTIVITY / TOTAL_CALORIES_BURNED           4 SYNCED
```

`WORKOUT_ACTIVITY / STEPS` permaneceu sem publicação duplicada: as quatro
operações ficaram `PENDING`, enquanto as quatro linhas de crédito ficaram
`BLOCKED`; isso representa resolução local `ALREADY_COVERED`, não pendência
remota acionável. D1 granular carregou a representação diária. As evidências anteriores
`033-03`, `033-05`, `033-06` e `033-10` confirmam, em dumps independentes, retry
com payload congelado, idempotência D1/D3/D4, cutover granular, overlap por
métrica, distância/calorias D1 e retry explícito do Health Connect.

## Superfícies de produto

- Home mostrou somente coleta no pull-to-refresh e ação separada
  `Sincronizar com Health Connect`.
- Configurações mostrou provider disponível, integração write-only, `Gerenciar
  relógio` e `Sincronizar agora`; a ação explícita foi acionada sem crash.
- Saúde não mostrou CTA BLE, medição manual ou importação D1.
- Exercícios abriu histórico importado sem CTA `Importar`/`Buscar treinos`.
- Perfil mostrou altura e meta de passos, sem calibração por comprimento de passo.

### Correção de renderização validada — 2026-09-15

O primeiro build instalado ainda mostrava `indisponível` para as atividades
importadas. O banco, porém, continha snapshots nativos elegíveis. A causa foi
localizada em `DatabaseMappers.kt`: `FREE_WORKOUT` e `TREADMILL` eram rejeitados
ao reidratar snapshot nativo, fazendo o read path entregar
`canonicalReadStatus = MISSING_STEP_LENGTH`/indisponível. A UI também limitava
calibração por referência a `RUNNING`/`WALKING` e não listava calorias no detalhe.

Após corrigir mapper e UI, nova instalação física mostrou:

```text
lista:       Esteira 0,25 km; Treino livre 0,14 km
Esteira:     0,25 km; fonte nativa calibrada; 274 passos; 13 kcal
Esteira:     parciais e FC média visíveis
```

O detalhe de `FREE_WORKOUT` usa o mesmo read path nativo e a lista confirmou sua
distância canônica. O ajuste não altera dados persistidos nem política de
distância; restaura somente projeção visual dos snapshots já existentes.

## Gates não reproduzidos integralmente

1. `dayOffset > 0` não estava disponível no banco físico; risco permanece para
   firmware/dados futuros.
2. A execução desta rodada não reproduziu uma instalação partindo de um arquivo
   Room v17. O banco físico atual está em v19 e preserva dados legacy; a
   evidência anterior confirmou v18 → v19. A cadeia v17 → v19 permanece risco de
   cobertura de upgrade, não uma falha observada no banco atual.
3. O partial success observado foi uma falha real de componente durante refresh,
   não uma injeção artificial de falha. A UI não identificou qual componente
   falhou; persistência anterior permaneceu intacta.
4. D4 zero/ausente foi mantido como guard técnico conforme evidência 033-10; não
   foi fabricado cenário físico contraditório.
5. `FEA1 < ΣD1`, lacuna estrutural e frescor inexplicado não foram reproduzidos
   fisicamente nesta rodada; o gate permanece aberto.

## Conclusão

A linha unificada, a classificação `RECENT_TAIL`, a preservação de sucesso
parcial, a separação watch sync/Health Connect, o cutover granular e a política
por métrica foram observados no dispositivo real e no banco Room v19. O
Os gates negativos não reproduzidos, `dayOffset > 0`, o upgrade direto v17 → v19
e a ausência de partial success artificialmente injetado foram aceitos por waiver
explícito de fechamento. Permanecem riscos residuais; isso não indica ausência
da migration v18 nem falha observada na cadeia `v17 → v18 → v19`.
