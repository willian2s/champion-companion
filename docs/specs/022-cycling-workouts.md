# 022 — Suporte a treinos de bicicleta

- **Ticker:** `022`
- **Status:** `pending`
- **Base:** `main`
- **Branch:** `feat/022-cycling-workouts`
- **Progresso planejado:** `0/5`

## Contexto

O Champion Companion já importa treinos D3/D4 iniciados pelo Champion C001,
permite revisão local, deriva métricas e sincroniza projeções granulares com o
Health Connect.

O domínio atual suporta `WALKING`, `RUNNING`, `FREE_WORKOUT`, `TREADMILL` e
`UNKNOWN`. Não existe tipo próprio para bicicleta.

A nova necessidade é permitir que uma atividade D3/D4 seja revisada como
**Bicicleta**, sem distinguir bicicleta de academia e bicicleta de rua. O
relógio continua sendo a fonte da janela temporal, BPM e demais fatos D4; a
distância pode ser informada manualmente pelo usuário quando conhecida, mas é
opcional.

Esta spec é uma unidade de trabalho independente e não representa a Fase 22 do
roadmap.

## Objetivo

Adicionar suporte a treinos de bicicleta importados do relógio, preservando o
fluxo watch-first vigente e evitando derivar distância, velocidade ou ritmo de
bicicleta a partir de passos.

Fluxo esperado:

```text
Champion C001 inicia/encerra treino
        ↓
importação D3/D4 explícita
        ↓
revisão local
        ↓
Tipo = Bicicleta
        ↓
Distância manual opcional
        ↓
READY_TO_SYNC
        ↓
Health Connect
```

## Decisões de domínio

### 1. Tipo único de bicicleta

Adicionar ao domínio:

```text
WorkoutType.CYCLING
```

Rótulo de UI:

```text
Bicicleta
```

Não criar subtipos `INDOOR`, `OUTDOOR`, `STATIONARY_BIKING` ou equivalentes.
O mesmo tipo deve servir para bicicleta de academia e bicicleta de rua.

O tipo não deve ser inferido automaticamente por quantidade de passos. O usuário
o escolhe durante a revisão.

### 2. Fontes de verdade

Para atividade confirmada como `CYCLING`:

```text
startedAt / endedAt / duração
    → treino D3/D4

BPM granular
    → samples D4

calorias
    → relógio, quando válidas

passos/raw D4
    → preservados como fatos/diagnóstico
    → não definem distância da bicicleta

manualDistanceMeters
    → usuário, opcional
```

A presença ou ausência de distância não altera a validade temporal da atividade.

### 3. Distância manual opcional

A revisão deve permitir informar a distância total da bicicleta em quilômetros,
armazenada internamente em metros.

Regras:

- campo opcional;
- valor, quando presente, deve ser positivo e finito;
- usuário pode limpar o valor antes da sincronização;
- ausência de distância não envia a atividade para calibração;
- ausência de distância não impede `READY_TO_SYNC`;
- não reutilizar `calibratedDistanceMeters` nem o fluxo de calibração de esteira;
- persistir a distância em campo próprio, com semântica de entrada manual;
- linhas existentes recebem `NULL` na migration.

A implementação deve avaliar `manualDistanceMeters` como nome canônico do campo,
sem criar enum de origem se o próprio campo já expressar a proveniência de forma
inequívoca.

### 4. Política de métricas para bicicleta

Quando `confirmedType == CYCLING`:

- nunca usar `decodedSteps × stepLengthMeters` como distância apresentada;
- nunca usar a série derivada por passos como velocidade de bicicleta;
- não apresentar pace por quilômetro;
- não apresentar cadência de passos como cadência de bicicleta;
- não materializar splits por quilômetro de bicicleta a partir da série de passos;
- preservar snapshots e intervalos D4 existentes como evidência/proveniência;
- não apagar derivação anterior apenas por mudança do tipo confirmado.

Se houver distância manual válida:

```text
averageSpeedMetersPerSecond = manualDistanceMeters / officialDurationSeconds
```

A velocidade média é um derivado local de resumo. Ela não autoriza criar uma
série granular de velocidade.

Se não houver distância manual:

```text
distance = ausente
averageSpeed = ausente
```

### 5. Revisão e workflow

`CYCLING` deve aparecer junto aos tipos revisáveis existentes.

A revisão deve permitir:

- selecionar `Bicicleta`;
- informar ou limpar distância manual opcional;
- manter título e observações existentes;
- concluir revisão sem distância;
- seguir para `READY_TO_SYNC` quando os demais guards atuais forem satisfeitos.

`CYCLING` não entra em `PENDING_CALIBRATION` apenas por não possuir distância.

Ao confirmar outro tipo no lugar de `CYCLING`, a implementação deve impedir que
uma distância manual de bicicleta passe a ser consumida por walking/running/
treadmill. A forma preferida é limpar o valor quando a revisão for salva com tipo
não `CYCLING`; alternativamente, pode preservá-lo somente se todos os consumers
falharem fechados fora de `CYCLING`. A task de persistência deve escolher uma
única política e documentá-la.

Atividades `SYNCED` continuam protegidas pelos guards existentes. Esta spec não
abre edição pós-sync nem reconciliação nova para mudanças manuais após envio.

## Persistência

O schema atual está em Room v11. A implementação deve adicionar somente o que é
necessário para preservar distância manual da atividade.

Direção planejada:

```text
Room v11 → v12
workout_activities.manualDistanceMeters REAL NULL
```

Requisitos:

- migration aditiva;
- nenhum wipe;
- nenhuma recomputação de atividade na migration;
- nenhuma criação/alteração de operação Health Connect na migration;
- exportar schema Room v12;
- manter dados antigos intactos.

O modelo de domínio deve expor a distância manual separadamente das distâncias
`original`, `estimated` e `calibrated`.

## Consumidores locais

Todos os consumers que exibem ou agregam distância/velocidade/pace de treino
devem ser auditados antes da implementação.

Para `CYCLING`, pelo menos as superfícies abaixo devem respeitar a política
específica:

- detalhe da atividade;
- lista de exercícios;
- calendário/detalhe diário;
- históricos/aggregates locais que incluam distância de treino;
- status e resumo antes da sincronização.

UI esperada, quando houver distância:

```text
Bicicleta
Duração          45:12
Distância        16,4 km
Velocidade média 21,8 km/h
FC média         ...
FC máxima        ...
Calorias         ...
```

Sem distância:

```text
Bicicleta
Duração          45:12
Distância        —
Velocidade média —
FC média         ...
Calorias         ...
```

Passos podem continuar persistidos, mas não devem ser promovidos como métrica de
performance da bicicleta na UI principal.

## Health Connect

### Sessão

`WorkoutType.CYCLING` deve mapear para o tipo genérico de ciclismo do Health
Connect:

```text
ExerciseSessionRecord.EXERCISE_TYPE_BIKING
```

Não usar `EXERCISE_TYPE_BIKING_STATIONARY`, porque o modelo local não distingue
indoor/outdoor.

### Projeções do relógio

Para `CYCLING`, preservar quando elegíveis:

- `ExerciseSessionRecord`;
- `HeartRateRecord` granular D4;
- `TotalCaloriesBurnedRecord`.

### Distância manual

Quando houver `manualDistanceMeters` válido, criar exatamente uma projeção total
de distância para toda a sessão.

Adicionar uma chave filha estável própria, por exemplo:

```text
HealthProjectionChildKey.ManualDistance
stableValue = "manual-distance"
recordType = DISTANCE
```

A projeção deve gerar um único `DistanceRecord` cobrindo a janela da sessão.

Como a distância foi digitada pelo usuário, o metadata deve usar gravação manual
e dispositivo local/telefone, não `autoRecorded(watchDevice())`.

### Projeções proibidas para bicicleta

Não criar para `CYCLING`:

- `DistanceInterval` derivado de passos;
- `SpeedSeries` derivada de passos;
- `SpeedRecord` sintético a partir de distância total / duração;
- `StepsRecord` específico da atividade;
- `ExerciseRoute`.

O valor de velocidade média pode existir somente no resumo local.

### Retry e payload congelado

A nova projeção manual deve continuar usando:

- identidade determinística;
- versão monotônica do aggregate;
- payload congelado para retry;
- reconciliação local existente por filho.

O payload congelado atual já possui `distanceMeters` e `projectionChildKey`; a
implementação deve preferir reutilizar o schema existente se a nova chave filha
for suficiente para reconstruir o `Metadata.manualEntry`. Não aumentar versão do
payload sem necessidade funcional real.

Operações antigas permanecem legíveis e não são reescritas automaticamente.

## Compatibilidade

- atividades existentes não recebem `CYCLING` automaticamente;
- nenhuma atividade antiga é reprocessada durante upgrade;
- não alterar parser D3/D4;
- não alterar `d4-v3` nem `watch-d3-d4-v2`;
- snapshots de distância por passos continuam preservados;
- confirmed type é quem decide se esses snapshots são semanticamente consumíveis;
- esteira mantém fluxo próprio de calibração;
- running/walking mantêm política atual.

## Fora do escopo

- GPS, localização, mapa ou rota;
- reativar treino iniciado pelo celular;
- distinguir bicicleta indoor/outdoor;
- integração Bluetooth com bicicleta/FTMS;
- importar RPM/cadência de pedal;
- potência/watts;
- sensores de velocidade/cadência externos;
- estimar distância por passos;
- estimar distância por calorias/BPM;
- gerar velocidade granular sintética;
- editar ou reconciliar atividade já `SYNCED` por novos campos;
- testes automatizados novos ou execução de suites, salvo pedido explícito.

## Subtarefas

```text
022-01
   ↓
022-02
   ↓
022-03
   ↓
022-04
   ↓
022-05
```

- `022-01` — auditar consumers e congelar contratos de bicicleta;
- `022-02` — adicionar tipo, persistência e edição de distância manual;
- `022-03` — adaptar métricas e UI locais para semântica de ciclismo;
- `022-04` — implementar projeções Health Connect específicas de bicicleta;
- `022-05` — validar banco preservado, fluxo real e fechar documentação.

## Critérios de aceite globais

1. `Bicicleta` aparece como tipo revisável e persiste como `WorkoutType.CYCLING`.
2. O mesmo tipo serve para academia e rua, sem subtipos.
3. Distância manual é opcional, persistida em metros e não reutiliza calibração de esteira.
4. Atividade de bicicleta sem distância pode alcançar `READY_TO_SYNC`.
5. Passos D4 nunca definem distância, velocidade, pace ou splits da bicicleta.
6. Distância manual válida gera velocidade média local, mas não `SpeedRecord` granular.
7. BPM e calorias do relógio continuam disponíveis normalmente.
8. Health Connect recebe `EXERCISE_TYPE_BIKING`.
9. Sem distância manual, nenhuma projeção `DISTANCE` é criada para bicicleta.
10. Com distância manual, exatamente um `DistanceRecord` total é criado com provenance manual.
11. `DistanceInterval` e `SpeedSeries` derivados por passos não são elegíveis para bicicleta.
12. Migration v11→v12 preserva banco existente sem wipe ou efeito remoto.
13. Running, walking e treadmill não sofrem regressão de política.
14. GPS, rota e treino phone permanecem ausentes da direção funcional.

## Validação planejada

Sem criar ou executar testes automatizados como etapa padrão.

Validação final deve incluir:

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug` sobre banco existente;
- confirmar `PRAGMA user_version = 12` e integridade do banco;
- revisar uma atividade D3/D4 como `Bicicleta` sem distância;
- revisar outra atividade com distância manual;
- validar UI sem pace/cadência de passos para bicicleta;
- validar velocidade média somente quando houver distância;
- sincronizar cenário sem distância e conferir sessão/BPM/calorias elegíveis;
- sincronizar cenário com distância e conferir uma única projeção total de distância;
- confirmar ausência de `DistanceInterval`, `SpeedSeries` e rota para bicicleta;
- confirmar que navegação/calendário não cria efeitos remotos.

## Relação com decisões existentes

- ADR-017 continua definindo direção watch-first.
- ADR-018 continua definindo raw/measured/derived/projection e projeções filhas.
- ADR-019 continua definindo duração oficial e timeline D4 normalizada.
- ADR-021 define a exceção semântica de ciclismo para distância por passos e a
  provenance da distância manual.
