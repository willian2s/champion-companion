# ADR-022 — Calibração contextual do comprimento do passo

- **Status:** aceita para implementação na Fase 16 / Spec 023
- **Data:** 2026-08-12

## Contexto

Os treinos D3/D4 do Champion Companion calculam distância canônica a partir de
passos decodificados e de um `stepLengthMeters` congelado por atividade. O perfil
atual possui apenas um comprimento de passo base, obtido manualmente ou por
sugestão baseada em altura.

Esse valor único é útil como fallback, mas não captura a diferença natural entre
corrida e caminhada. Ao mesmo tempo, o usuário possui distâncias externas de
referência obtidas por GPS ou percurso conhecido e quer usá-las para melhorar os
treinos futuros em que o Champion C001 fornece apenas passos.

O aplicativo não deve reintroduzir GPS ou rota como feature própria.

## Decisão

### 1. Perfil base continua existindo

O `StepLengthProfile` atual permanece como perfil `BASE`.

Ele continua podendo ter origem:

- `MANUAL`;
- `HEIGHT_SUGGESTION_ACCEPTED`.

O valor base continua sendo usado onde não existe contexto mais específico.

### 2. Perfis aprendidos separados

A Fase 16 adiciona dois contextos aprendidos:

```text
RUNNING
WALKING
```

Eles não são preenchidos por defaults artificiais. Só existem quando há pelo
menos uma evidência válida de calibração para o contexto.

### 3. Evidência de calibração

Uma calibração associa uma atividade D3/D4 revisada como `RUNNING` ou `WALKING`
a uma distância total de referência fornecida explicitamente pelo usuário.

A atividade deve possuir série D4 elegível e quantidade positiva de
`decodedSteps`.

Para cada evidência:

```text
observedStepLengthMeters = referenceDistanceMeters / decodedSteps
```

A distância de referência não é assumida como coletada pelo Champion Companion.
Ela pode vir de GPS externo ou de um percurso conhecido.

### 4. Aprendizado determinístico

O perfil aprendido de cada contexto é calculado pela razão entre os totais das
evidências ativas:

```text
learnedStepLengthMeters = Σ referenceDistanceMeters / Σ decodedSteps
```

Não usar média simples dos comprimentos observados, média móvel, EMA, regressão
ou peso oculto.

A escolha torna o cálculo reproduzível, auditável e naturalmente ponderado pelo
número de passos de cada treino.

### 5. Uma evidência ativa por atividade

Uma mesma atividade não pode ser contada duas vezes no aprendizado.

A persistência deve garantir no máximo uma calibração ativa por `activityId`.
Uma correção explícita substitui a evidência anterior; uma remoção explícita a
retira do cálculo futuro.

### 6. Resolução do comprimento do passo

Para novos cálculos:

```text
WorkoutType.RUNNING
    -> RUNNING aprendido, se disponível
    -> senão BASE

WorkoutType.WALKING
    -> WALKING aprendido, se disponível
    -> senão BASE

WorkoutType.TREADMILL
    -> BASE nesta fase

DailySummary
    -> BASE

WorkoutType.CYCLING
    -> política da ADR-021 / Spec 022; passos não geram distância
```

A Fase 16 não tenta inferir se `TREADMILL` foi caminhada ou corrida.

### 7. Atividade calibrada usa a própria evidência

Quando a atividade ainda pode ser rederivada sem conflito remoto, a distância de
referência pode ser aplicada à própria atividade.

O snapshot novo congela:

```text
stepLengthMetersUsed = referenceDistanceMeters / Σ decodedSteps
stepLengthSource = REFERENCE_DISTANCE_CALIBRATION
```

A soma das distâncias dos intervalos deve fechar na distância de referência,
sujeita apenas à precisão numérica do cálculo.

A distribuição temporal continua baseada nos passos D4 e na timeline normalizada
vigente; não são inventadas amostras GPS.

### 8. Atualização de perfil não reescreve histórico

Adicionar, substituir ou remover uma evidência pode mudar o perfil aprendido
usado por treinos futuros.

Isso nunca altera automaticamente:

- snapshots de derivação já persistidos;
- atividades antigas;
- filhos Health Connect já preparados/enviados;
- `clientRecordVersion` de atividades históricas.

Reprocessamento de atividade antiga continua sendo ação explícita e sujeito aos
guards existentes.

### 9. Atividades sincronizadas

Atividade `SYNCED`, `SYNCING` ou com efeitos remotos parciais não é rederivada
silenciosamente pela calibração.

A Spec 023 não cria reconciliação pós-sync genérica. Se futuramente houver
necessidade de corrigir records já enviados, isso exige trabalho separado.

### 10. Versionamento de derivação

A implementação deve introduzir uma nova versão de derivação para tornar
identificável a resolução contextual de perfil e o uso de distância de
referência.

Direção prevista:

```text
watch-d3-d4-v3
```

O parser D4 não muda por causa desta decisão.

### 11. Proveniência das projeções

Sessão, BPM e calorias continuam com a proveniência já definida pelo treino do
relógio.

Distância e velocidade de uma atividade cuja derivação foi diretamente
calibrada por uma distância digitada pelo usuário devem preservar essa origem de
forma honesta no payload/projeção. A implementação deve auditar o menor ajuste
compatível ao contrato congelado antes de mudar versão de payload.

Treinos futuros que apenas usam um perfil aprendido não são tratados como se a
distância daquele treino tivesse sido digitada manualmente; eles continuam sendo
cálculos automáticos sobre passos do relógio e perfil pessoal congelado.

## Persistência

A direção preferida é uma nova tabela de evidências, sem substituir os campos de
perfil base existentes:

```text
step_length_calibrations
- id
- profileId
- activityId (unique)
- workoutType
- referenceDistanceMeters
- decodedSteps
- acceptedAt
- updatedAt
```

`observedStepLengthMeters` pode ser calculado deterministicamente a partir dos
dois fatos persistidos; se for armazenado por auditabilidade, deve ser validado
contra a razão dos fatos de origem.

O perfil aprendido pode ser calculado a partir dessas evidências em vez de ser
mantido como segundo estado autoritativo duplicado.

## Consequências

- corrida e caminhada evoluem de forma independente;
- um treino longo tem peso proporcional à quantidade de passos sem regra extra;
- uma calibração ruim pode ser corrigida/removida sem apagar histórico bruto;
- o perfil base continua compatível com o comportamento existente;
- não há necessidade de GPS no Champion Companion;
- atividades antigas permanecem reproduzíveis pelos snapshots congelados;
- a derivação precisa resolver contexto antes de selecionar o perfil efetivo.

## Alternativas rejeitadas

### Um único comprimento de passo aprendido

Rejeitado porque mistura corrida e caminhada e reduz a precisão dos dois usos.

### Média simples das passadas observadas

Rejeitada porque daria o mesmo peso a um treino curto e a um treino longo.

### Atualizar apenas o último valor

Rejeitado porque descarta evidências anteriores e torna o resultado sensível ao
último treino.

### GPS nativo no Champion Companion

Rejeitado porque GPS/rota foram removidos da direção do produto.

### Reprocessar todas as atividades ao mudar o perfil

Rejeitado porque viola snapshots imutáveis e pode alterar dados já sincronizados.

### Aprender por pace/cadência desde a primeira versão

Rejeitado porque adicionaria complexidade antes de acumular evidência suficiente
para demonstrar benefício real.

## Relação com decisões anteriores

- ADR-017 mantém a direção watch-first;
- ADR-018 mantém separação raw/measured/derived/projection;
- ADR-019 mantém duração oficial e timeline normalizada D4;
- ADR-021 mantém a política de bicicleta, que não usa passos para distância;
- esta ADR adiciona somente calibração e seleção contextual de comprimento do
  passo para corrida/caminhada.

## Complemento da auditoria `023-01`

A auditoria de integração em
`docs/integration/023-01-personal-metrics-and-calibration-audit.md` detalha os
contratos executáveis desta decisão. Ficam adicionalmente congelados:

- `step_length_calibrations` usa FK para `user_profiles`, não exige FK rígida
  para `workout_activities` e garante `UNIQUE(activityId)`; correção atualiza a
  evidência e remoção a exclui do cálculo futuro;
- a evidência e o snapshot diretamente calibrado são persistidos na mesma
  transação local, sem enqueue ou efeito remoto;
- rederivação da própria atividade usa whitelist fail-closed: somente
  `PENDING_CALIBRATION` e `READY_TO_SYNC`, e `SYNC_FAILED` sem filhos aceitos ou
  efeitos remotos parciais; `PENDING_REVIEW`, `SYNCING`, `SYNCED` e `DISCARDED`
  são bloqueados;
- a identidade de `HealthProjectionOrigin` permanece compatível com o código
  vigente: `originId = activity.id` e `originVersion` é a versão do snapshot;
- provenance de distância/velocidade diretamente calibrada será
  `metricProvenance: String?` opcional no payload congelado, com valores
  `WATCH_DERIVED` e `EXTERNAL_REFERENCE_CALIBRATION`. Payload v1 sem campo
  significa derivação automática; valor desconhecido falha fechado;
  `ManualDistance` continua exclusivo de `CYCLING`;
- `DailySummary` permanece na semântica `BASE` vigente, sem usar perfis
  aprendidos nem reescrever resumos históricos.
