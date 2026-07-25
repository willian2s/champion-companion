# ADR-012 — Health Connect write-only, local-first e granular na Fase 12

## Status

Aceita

## Contexto

Ao fim da Fase 11, Champion Companion já entrega backlog local relevante para integração nativa com Health Connect:

- resumos diários persistidos localmente;
- medições manuais persistidas localmente;
- histórico D1 persistido com operações locais em `health_sync_operations` para `HEART_RATE`;
- atividades importadas revisadas e calibradas até `READY_TO_SYNC`;
- `AppPreferences.healthSyncEnabled` já persistida em DataStore;
- Room como fonte operacional e visual primária.

Ao mesmo tempo, o app ainda não possui:

- SDK Health Connect integrado;
- provider detection, permissões ou rationale;
- fila persistida granular para passos, atividade, peso ou altura;
- política formal para `clientRecordId`, `clientRecordVersion`, falha parcial e retry foreground.

A Fase 12 precisa consolidar essas decisões antes da implementação funcional para evitar três riscos principais:

1. acoplamento indevido entre UI local e provider externo;
2. duplicação remota por identidade instável;
3. perda de rastreabilidade local em falhas parciais de atividade.

## Decisão

### 1. Health Connect será write-only na Fase 12

O app escreverá dados locais próprios no Health Connect, mas não lerá dados produzidos por outros apps nem usará o provider como fonte visual primária.

Consequências imediatas:

- Home, Saúde e Exercícios continuam derivados de repositories locais;
- escrita no provider não redefine estado visual principal de sucesso local;
- leitura, `ChangeLogs`, reconciliação remota e importação de terceiros permanecem fora do escopo da fase.

### 2. Room permanece fonte operacional e visual primária

Persistência local válida nunca é desfeita por falha no provider.

Regras:

- falha no Health Connect não apaga resumo diário;
- falha no Health Connect não apaga BPM manual ou D1;
- falha no Health Connect não apaga atividade revisada ou calibração;
- falha no Health Connect não bloqueia uso offline do app.

### 3. A Fase 12 adota fila persistida granular por agregado e record type

Sincronização será controlada por operação persistida com chave natural por agregado e tipo de record, sem duplicação local de operações.

Tipos de record planejados:

- `STEPS`
- `HEART_RATE`
- `EXERCISE_SESSION`
- `DISTANCE`
- `TOTAL_CALORIES_BURNED`
- `WEIGHT`
- `HEIGHT`

Aggregates mínimos planejados:

- `DailySummary`
- `HeartRateMeasurement`
- `WorkoutActivity`
- `WeightMeasurement`
- `UserProfileHeight`

### 4. IDs e versões serão determinísticos e monotônicos

Cada record deve usar identidade estável derivada de:

```text
applicationId + aggregateType + aggregateId + recordType
```

Regras:

- retry reutiliza o mesmo `clientRecordId`;
- debug e release ficam separados pelo `applicationId`;
- IDs não incluem valores de saúde;
- updates usam `clientRecordVersion` monotônico por agregado mutável.

### 5. `insertRecords` será caminho preferencial de upsert idempotente

A Fase 12 usará `insertRecords` com `clientRecordId` e `clientRecordVersion` como mecanismo de escrita idempotente, evitando dependência de leitura prévia remota.

Regras:

- não apagar `Steps` por intervalo antes de inserir;
- não excluir records de outros apps;
- retry não cria novo ID;
- mudança local relevante reabre operação com versão maior.

### 6. Triggers serão controlados e assimétricos por tipo

Triggers automáticos permitidos na Fase 12:

- passos: persistir -> liberar BLE -> tentar sync;
- BPM manual: persistir -> liberar BLE -> tentar sync;
- D1: persistir lote -> liberar BLE -> processar backlog.

Atividade não sincroniza automaticamente ao atingir `READY_TO_SYNC`.

Regras para atividade:

- exige ação explícita do usuário;
- pode existir ação explícita de lote, desde que consciente;
- reconciliação genérica de backlog não enfileira automaticamente atividades `READY_TO_SYNC`.

### 7. Permissões serão agrupadas por capacidade e só de escrita

Permissões planejadas na fase serão write-only e agrupadas por domínio amigável ao usuário:

- Passos
- Frequência cardíaca
- Exercícios
- Distância
- Calorias
- Peso
- Altura

Regras:

- ausência de permissão complementar não bloqueia automaticamente a sessão de exercício;
- peso e altura não são exigidos para passos ou BPM;
- peso e altura só serão pedidos quando houver dado local elegível.

### 8. Atividade terá granularidade real e suportará falha parcial

Uma atividade pode produzir operações independentes:

- `EXERCISE_SESSION`
- `DISTANCE`
- `TOTAL_CALORIES_BURNED`
- `HEART_RATE`

Regras:

- `EXERCISE_SESSION` é obrigatória;
- complementos só existem quando há dado local elegível;
- sessão já sincronizada não é apagada se complemento falhar;
- retry processa somente pendentes ou falhos;
- atividade só vira `SYNCED` quando todas as operações elegíveis concluírem.

### 9. Atividade não criará `StepsRecord` separado

Atividades importadas não gerarão `StepsRecord` independente.

Razão:

- passos da atividade já participam do pipeline do resumo diário do relógio;
- novo `StepsRecord` por atividade aumentaria risco de dupla contagem.

### 10. A Fase 12 não enviará rota nem dados de localização

Continuam proibidos:

- `ExerciseRoute`;
- latitude;
- longitude;
- altitude;
- polyline;
- GPX;
- TCX;
- FIT.

### 11. A Fase 12 não usará `WorkManager`

Sincronização desta fase é foreground, controlada e manual/derivada de ações explícitas já existentes. `WorkManager`, retry em background, foreground service e sincronização periódica ficam explicitamente adiados para a Fase 14.

## Consequências

### Positivas

- protege Room como fonte operacional primária;
- reduz risco de duplicação remota por identidade instável;
- permite retry seguro por record granular;
- impede dupla contagem de passos por atividade;
- mantém escopo da fase controlado e compatível com backlog local já existente.

### Negativas

- exige migration da fila atual limitada a D1;
- aumenta coordenação entre domínio, Room, gateway, coordinator e UI;
- deixa recuperação em background fora desta fase;
- exige Gate C físico para validar permissões, provider, upsert e falha parcial.

## Alternativas consideradas

- usar Health Connect como fonte visual primária;
- ler provider antes de cada escrita;
- sincronizar atividade automaticamente ao atingir `READY_TO_SYNC`;
- criar `StepsRecord` separado por atividade;
- agrupar records da atividade em operação única indivisível;
- introduzir `WorkManager` já na Fase 12.

Todas rejeitadas por aumentarem acoplamento ao provider, risco de duplicação, imprevisibilidade de UX ou escopo estrutural além da fronteira aprovada para a fase.
