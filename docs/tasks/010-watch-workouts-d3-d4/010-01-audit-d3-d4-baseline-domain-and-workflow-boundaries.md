# Task 010-01 - Auditar baseline D3/D4, domínio e fronteiras de workflow

## Status

Concluída em 2026-07-23.

## Objetivo

Auditar MotionLog, baseline factual e código atual do Champion Companion para congelar fatos de D3/D4 antes de qualquer implementação da fase.

## Contexto

Ainda faltam confirmação documental do caminho da baseline em `AGENTS.md`, regra exata de intervalos, offsets D3/D4, timestamps de BPM, política de reimportação e limites entre importação, revisão e Health Connect.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-10-watch-workouts-d3-d4-direction.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/gaps-and-open-questions.md`
- `../motionlog-app/src/lib/ble/watch-workout-parser.ts`
- `../motionlog-app/src/lib/ble/watch-workout-import-adapter.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/src/lib/repositories/watch-workout-repository.ts`

## Dependências

- nenhuma.

## Entradas

- parser e repositório D3/D4 da baseline MotionLog;
- estado atual de domínio, Room e deduplicação no app nativo;
- divergência conhecida do caminho de baseline em `AGENTS.md`.

## Arquivos exatos permitidos

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-01-audit-d3-d4-baseline-domain-and-workflow-boundaries.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/roadmap/**`
- ADRs vigentes e specs/tasks concluídas de fases anteriores.

## Escopo

- auditar MotionLog D3/D4;
- auditar código Champion atual;
- confirmar estado de domínio e Room;
- verificar offsets D3 e D4;
- registrar lacunas e ambiguidades reais;
- confirmar regras de intervalos;
- confirmar timestamps das amostras;
- confirmar unidades;
- confirmar política de reimportação;
- corrigir o caminho da baseline em `AGENTS.md`;
- atualizar spec e overview.

## Fora do escopo

- implementar D3;
- implementar D4;
- tocar em Kotlin, Room, BLE, Compose, manifesto, Gradle ou DataStore.

## Passos

1. Auditar parser, adapter e repositório D3/D4 do MotionLog.
2. Confrontar esses fatos com `WatchWorkoutActivity.kt`, Room atual e `persistImported(...)`.
3. Registrar offsets, `minuteIndex`, regra de timestamp, duração do bucket, comportamento do último intervalo e divergência de `recordCount`.
4. Confirmar política de deduplicação e reimportação já existente no app nativo.
5. Corrigir `AGENTS.md` para usar `docs/baseline/current-baseline.md`.
6. Atualizar spec e overview com achados, limites e lacunas.

## Critérios de aceite

- offsets D3/D4 e unidades auditados sem hipótese nova;
- regra de `minuteIndex`, `sampledAt`, duração do intervalo e último bucket documentada;
- política de reimportação e deduplicação confirmada no estado atual;
- divergência do caminho da baseline corrigida em `AGENTS.md`;
- spec e overview atualizados com achados e lacunas.

## Testes pequenos aplicáveis

- nenhum por padrão; task é documental.

## Validação mínima

- revisão do diff documental;
- conferência dos caminhos citados e das dependências registradas.

## Evidências

- referências exatas do MotionLog auditado;
- lista explícita de offsets, regras e lacunas confirmadas;
- diff de `AGENTS.md`, spec e overview.

## Execução

### Referências auditadas

- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-parser.ts`
- `../motionlog-app/src/lib/ble/watch-workout-import-adapter.ts`
- `../motionlog-app/src/lib/repositories/watch-workout-repository.ts`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`

### Fatos confirmados do baseline MotionLog

- D3 lê CRCs `u16` little-endian a partir do offset `3`.
- D3 considera header multipart plausível com `packetIndex = u16 LE bytes[1..2]` e `totalPackets = u16 LE bytes[3..4]`.
- No baseline auditado, `bytes[3..4]` ficam sobrepostos entre header plausível e primeiro par candidato de CRC; não há evidência para assumir header D3 “limpo” como em D1.
- D3 ignora `0x0000`, preserva ordem de aparição e atribui `blockIndex` sequencial começando em `0`.
- D3 não deduplica CRC repetido antes do loop D4.
- D4 usa `packetIndex = u16 LE bytes[1..2]` e `totalPackets = u16 LE bytes[3..4]`.
- D4 aceita pacotes fora de ordem, ordena antes do parser e preserva duplicatas para diagnóstico.
- Pacotes obrigatórios continuam `1`, `2` e `3`; ausência deles faz parser retornar `null`.
- Pacote `1` preferido usa início `7..13` e fim `14..19`; segundo final ausente vira `0`.
- Pacote `2` preferido usa passos `7..8`, distância `11..12 * 100 cm` e calorias `15..17 / 1000`.
- Pacote `3` preferido usa `recordCount` no byte `9` e CRC `14..15`; fallback legado usa duração `u32 LE 5..8`.
- Pacote `4+` usa `minuteIndex = packetIndex - 4`, BPM byte `6`, passos `u24 LE 7..9`, distância `u24 LE 10..12` e calorias `u16 LE 13..14`.
- `sampledAt` das amostras BPM usa `startedAt + minuteIndex * 60_000`.
- Cada bucket D4 `4+` vale 60 segundos; bucket final não é encurtado para fechar sobra residual.
- Divergência de `recordCount`, timeout, índice ausente ou `minuteIndex` duplicado gera `partial`; baseline não cria amostra sintética.

### Fatos confirmados do app nativo

- `WorkoutActivity.createFromImport(...)` cria atividade `WATCH_D3_D4` em `PENDING_REVIEW`.
- `WorkoutActivity.confirmReview(...)` bloqueia revisão quando `parserValidity != VALID`.
- `WatchWorkoutImportEntity` possui índice único `(deviceId, origin, crc)`.
- `WorkoutIntervalEntity` possui índice único `(activityId, intervalIndex)`.
- `WorkoutHeartRateSampleEntity` possui índice único `(activityId, sampledAtEpochMillis)`.
- `RoomRepositories.persistImported(...)` já faz deduplicação/reimportação por chave natural, preserva `confirmedType`, `workflowState`, `reviewedAt`, `calibrationId` e métricas calibradas, apaga intervalos/amostras antigas e reinsere conjunto novo na mesma escrita.
- `observePendingReview()` já existe e reconstrói domínio com intervalos e amostras persistidos.

### Lacunas e riscos reais registrados

- Deduplicação intra-tentativa de CRC repetido precisa ser implementada explicitamente no Champion Companion; MotionLog não resolve isso sozinho.
- Header D3 multipart continua ambíguo no baseline porque divide bytes com início da área de CRC.
- Offsets D4 e unidades seguem confirmados apenas “pelo código”; validação física continua obrigatória em `010-11` e `010-12`.

## Atualização do overview

- marcar `010-01` como concluída;
- registrar achados iniciais e risco novo de deduplicação intra-tentativa por CRC repetido.

## Validação final

- confirmado: nenhuma implementação D3/D4 foi feita nesta task; mudanças ficaram restritas a `AGENTS.md`, spec e documentação da Fase 10.
