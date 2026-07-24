# ADR-011 — Fase 11 termina em READY_TO_SYNC

## Status

Aceita

## Contexto

Ao fim da Fase 10, Champion Companion já entrega importação local-first de treinos D3/D4 até `PENDING_REVIEW`, com persistência local de atividade importada, intervalos, amostras de BPM, validade do parser, sugestão de tipo, reimportação por chave natural e preservação de revisão/calibração já existentes.

Auditoria registrada em `011-01` confirmou também que o código atual já possui base parcial importante para a Fase 11:

- `WorkoutActivity` já contém estados `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED` e `DISCARDED`;
- `WorkoutActivity.confirmReview(...)` já bloqueia confirmação quando `parserValidity != VALID`;
- `WorkoutActivity.confirmReview(...)` já separa revisão não esteira (`READY_TO_SYNC`) de revisão de esteira (`PENDING_CALIBRATION`);
- `RoomRepositories.planWatchWorkoutPersistence(...)` já preserva `confirmedType`, `workflowState`, `reviewedAt`, `calibrationId` e métricas calibradas na reimportação;
- ainda não existe caminho funcional de Health Connect para treinos importados.

Ao mesmo tempo, roadmap atual ainda colocava na Fase 11 responsabilidades de sincronização real, retry do provider e até critério de que o Health Connect receberia a distância calibrada. Isso criava mistura indevida entre:

```text
Fase 10 -> importar -> PENDING_REVIEW
Fase 11 -> revisar/calibrar -> READY_TO_SYNC
Fase 12 -> sincronizar -> SYNCING/SYNCED/SYNC_FAILED
```

Essa mistura conflita com princípios já aceitos no projeto:

- persistência local é fonte operacional e visual primária;
- revisão local não depende de provider externo;
- falha futura do provider não deve desfazer dado local válido;
- Health Connect não substitui Room/repositories locais como fonte visual primária;
- `PARTIAL` continua bloqueando avanço até pronto.

## Decisão

Fase 11 passa a terminar formalmente em `READY_TO_SYNC`.

### 1. Responsabilidades da Fase 11

Fase 11 deve:

- listar atividades importadas e filtrar por workflow;
- permitir revisão local com tipo confirmado, título e observações;
- bloquear confirmação quando `parserValidity != VALID`;
- avançar atividade não esteira de `PENDING_REVIEW` para `READY_TO_SYNC`;
- avançar atividade de esteira de `PENDING_REVIEW` para `PENDING_CALIBRATION` e depois para `READY_TO_SYNC`;
- persistir calibração como camada derivada local;
- manter descarte lógico local;
- preservar decisões do usuário na reimportação.

### 2. Responsabilidades fora da Fase 11

Fase 11 não deve implementar:

- `HealthConnectClient`;
- detecção de provider;
- permissões do Health Connect;
- criação de records;
- envio real;
- update ou delete remoto;
- fila de atividades para provider;
- retry do provider;
- marcação funcional de `SYNCING`, `SYNCED` ou `SYNC_FAILED`.

### 3. Responsabilidades da Fase 12

Fase 12 passa a ser owner da integração real com Health Connect para atividades importadas e backlog local já produzido.

Fase 12 deve:

- consumir atividades `READY_TO_SYNC` já persistidas localmente;
- usar tipo confirmado, título, observações e distância calibrada quando existirem;
- mapear sessão, distância, calorias, passos e BPM para o provider;
- controlar `SYNCING`, `SYNCED` e `SYNC_FAILED`;
- manter falhas parciais como pendentes ou failed sem desfazer os dados locais.

### 4. Imutabilidade das métricas importadas

Métricas importadas continuam imutáveis como registro do que veio do relógio.

Regras:

- revisão do usuário não altera parser validity, origem, CRC nem métricas importadas;
- calibração não substitui silenciosamente métricas importadas;
- distância calibrada, ritmo calibrado e velocidade calibrada formam camada derivada;
- atividade pronta continua exibindo fonte local preservada e derivação separada.

### 5. Calibração como camada derivada

Calibração de esteira é registro imutável e derivado.

Regras:

- atividade aponta para calibração ativa por `calibrationId`;
- fator pode existir ou permanecer ausente quando não houver distância-base positiva;
- ausência de fator não bloqueia `READY_TO_SYNC`;
- valor enviado futuramente ao Health Connect deve usar distância calibrada, não sobrescrever histórico importado local.

### 6. Descarte lógico

Descarte de atividade importada passa a ser lógico.

Regras:

- usar `workflowState = DISCARDED`;
- não apagar atividade, import record, intervalos, amostras, calibração nem diagnóstico;
- descarte impede sync futuro enquanto estado permanecer descartado.

### 7. Bloqueio de PARTIAL

Atividade `PARTIAL` continua visível, descartável e reimportável, mas não pode:

- ser confirmada como revisada;
- ser calibrada;
- avançar para `READY_TO_SYNC`.

### 8. Preservação de revisão na reimportação

Reimportação do mesmo treino por `(deviceId, origin, crc)` deve preservar decisões do usuário já persistidas.

No mínimo:

- `confirmedType`;
- `title`;
- `notes`;
- `workflowState`;
- `reviewedAt`;
- `calibrationId`;
- métricas calibradas.

## Consequências

### Positivas

- remove ambiguidade entre revisão local e sync real;
- protege Fase 11 como entrega local-first utilizável sem provider;
- preserva Room/repositories locais como fonte visual primária;
- prepara backlog consistente para Fase 12;
- formaliza descarte lógico e bloqueio de `PARTIAL` sem reabrir parser.

### Negativas

- obriga atualização imediata do roadmap;
- adiciona etapa documental para consolidar fronteira da fase;
- exige que Fase 12 consuma dados locais já revisados e calibrados em vez de recalcular responsabilidade da Fase 11.

## Alternativas consideradas

- manter sync real e retry do provider dentro da Fase 11;
- tratar revisão como se já fosse sincronização;
- deixar `PARTIAL` avançar manualmente por ação do usuário;
- apagar fisicamente atividades descartadas;
- recalcular e substituir métricas importadas em vez de mantê-las imutáveis.

Alternativas rejeitadas por misturarem fases, acoplarem revisão ao provider externo, aumentarem risco de perda de histórico local ou violarem fronteiras locais-first já adotadas no projeto.
