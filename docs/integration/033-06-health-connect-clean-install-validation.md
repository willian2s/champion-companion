# 033-06 — Validação física Health Connect em instalação limpa

- **Data:** 2026-09-13
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Escopo:** validação física pós-implementação inicial da 033-06
- **Privacidade:** evidência sanitizada; sem payload BLE integral, identificadores pessoais ou BPM individual

## Cenário

Após validar a migration real v18 → v19 e o cutover em instalação com legado, os
dados do app foram limpos para exercitar uma instalação nova sem backlog local.
O relógio foi associado novamente, a Home executou o watch sync unificado e,
depois, `Configurações -> Sincronizar agora` foi acionado explicitamente.

## Estado local observado

O banco continha fatos locais íntegros do mesmo dia:

```text
cutover solicitado: 2026-09-13

DailySummary/FEA1:
  5056 passos

D1:
  202 buckets
  5056 passos
  3731 m
  244.0 kcal

Workouts D4 v4:
  3 atividades
  1 RUNNING em READY_TO_SYNC
  2 em PENDING_REVIEW
```

BPM e altura sincronizaram com sucesso. A fila Health Connect, porém, continha
somente operações desses dois tipos:

```text
HEART_RATE  57  SYNCED
HEIGHT       1  SYNCED
```

Não havia operação local para:

```text
STEPS
DISTANCE
TOTAL_CALORIES_BURNED
EXERCISE_SESSION
STEPS_CADENCE
SPEED
```

Isso prova que a ausência observada no provider aconteceu antes da escrita remota:
as projeções não estavam sendo preparadas localmente.

## Achado 1 — cutover de STEPS prematuro

Na instalação limpa, a data de cutover solicitada coincidiu com o dia atual. O
contrato inicial da 033-06 bloqueava `DailySummary/FEA1 -> STEPS` a partir dessa
data, mas a projeção D1 granular permaneceu deliberadamente `deferred` porque o
firmware ainda não fornece uma fronteira temporal comprovada para construir o
intervalo remoto e a convivência com `WorkoutSteps` ainda exige contabilidade de
não sobreposição.

O resultado foi uma lacuna funcional:

```text
cutover efetivo
  -> DailySummary STEPS desligado
  -> D1 granular STEPS deferred
  -> nenhum StepsRecord seguro disponível
```

### Correção

A data de cutover **solicitada** continua persistida por dispositivo, mas o
cutover **efetivo de STEPS** fica adiado enquanto
`GRANULAR_D1_STEPS_REMOTE_PROJECTION_READY = false`.

Enquanto isso:

```text
FEA1/DailySummary
  -> continua no caminho incremental seguro da Spec 016
  -> continua usando guards/créditos da Spec 027

D1 granular
  -> permanece local
  -> não fabrica intervalo
  -> não cria residual FEA1 - ΣD1
```

Quando houver prova suficiente para publicar D1 granular, a política de fallback
deve ser removida/ativada explicitamente; a data solicitada já permanece
preservada no banco para auditoria.

## Achado 2 — sync global não iniciava READY_TO_SYNC

`Configurações -> Sincronizar agora` chamava apenas `syncPending()`. Para workouts
do relógio, o caminho genérico preparava reconciliação somente quando já existia
fila local ou tombstone legado. Uma atividade `READY_TO_SYNC` sem primeira
projeção persistida era ignorada.

### Correção

O gesto explícito de Settings agora:

1. processa a fila local existente;
2. carrega somente `WorkoutActivityFilter.READY_TO_SYNC`;
3. restringe a `WATCH_D3_D4`;
4. chama `syncActivity(activity.id)` para cada atividade elegível;
5. mantém `PENDING_REVIEW`, `PENDING_CALIBRATION`, `DISCARDED` e `SYNCED` fora da
   criação inicial.

Assim o fluxo continua `review-before-sync` e reutiliza exatamente a preparação
congelada/manifesto/guards já existentes no sync explícito de workout.

## Achado 3 — backlog antigo podia atrasar dados atuais

Na validação anterior com banco legado, o limite padrão de 100 operações e a
ordenação causal/antiga da fila fizeram retries `FAILED` antigos ocuparem o lote
antes de operações `PENDING` atuais.

Reordenar simplesmente por recência ou estado antes de respeitar as cadeias
incrementais poderia quebrar causalidade de passos e créditos.

### Correção

O gesto `Sincronizar agora` passa a drenar a fila em passes de até 100 operações,
continuando somente enquanto houver progresso e operações remanescentes. O loop é
limitado a 20 passes por gesto para permanecer finito e foreground.

Isso preserva a ordenação causal existente dentro de cada consulta, mas impede o
caso físico observado — backlog de centenas de retries — de exigir vários cliques
manuais antes de alcançar fatos atuais.

Um passe que não produz progresso ou falha integralmente interrompe o loop para
evitar retry agressivo/infinito.

## Métricas que permanecem deferred

### Distância diária

D1 já inclui distância ocorrida durante workout D4. Publicar D1 diário e D4 workout
sem prova de cobertura causaria potencial dupla contagem. Continua fail-closed.

### Calorias diárias

Além da sobreposição D1/D4, ainda não há evidência suficiente para promover o raw
do relógio com segurança a `ActiveCaloriesBurned` ou `TotalCaloriesBurned`.
Continua local/deferred.

Esses bloqueios não impedem que uma atividade `READY_TO_SYNC` publique os filhos
de workout já suportados pelo contrato existente quando forem elegíveis.

## Guard rails preservados

- Home continua sendo o único entrypoint de leitura BLE de dados;
- watch sync não chama Health Connect automaticamente;
- Health Connect continua write-only;
- nenhuma leitura do provider foi adicionada;
- nenhuma atividade `PENDING_REVIEW` ou `PENDING_CALIBRATION` é sincronizada pelo
  gesto global;
- nenhum residual `FEA1 - ΣD1` é fabricado;
- `RECENT_TAIL` permanece inalterado;
- operações `SYNCED` não são reabertas;
- ledgers/créditos das Specs 016/027 permanecem autoritativos para evitar dupla
  publicação de passos;
- distância/calorias diárias continuam fail-closed.

## Validação física necessária após a correção

Em instalação limpa:

```text
associar relógio
-> Home pull-to-refresh
-> Configurações -> Sincronizar agora
-> dump
```

Confirmar:

1. `DailySummary/FEA1` volta a preparar `STEPS` enquanto D1 granular estiver
   deferred;
2. `WorkoutSteps` não duplica o total diário e respeita créditos da Spec 027;
3. a atividade `READY_TO_SYNC` ganha projeções filhas elegíveis e pode chegar a
   `SYNCED`;
4. as duas atividades `PENDING_REVIEW` permanecem sem primeira projeção remota;
5. BPM/altura continuam funcionando;
6. distância/calorias diárias continuam sem publicação insegura;
7. um banco com backlog antigo consegue avançar em vários lotes com um único
   gesto de Settings sem loop infinito.
