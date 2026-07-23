# Task 010-01 - Auditar baseline D3/D4, domínio e fronteiras de workflow

## Status

Pendente.

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

## Atualização do overview

- marcar `010-01` como concluída;
- registrar achados iniciais e qualquer risco novo de conflito.

## Validação final

- confirmar que D3/D4 não foram implementados nesta task.
