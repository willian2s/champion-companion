# 014 — Refinamento, derivação e sincronização dos treinos do relógio

- **Ticker:** `014`
- **Status:** pending
- **Base:** `main`, Fase 013 encerrada como `closed` / `partially_delivered`
- **Decisões relacionadas:** ADR-012, ADR-016, ADR-017 e ADR-018

## Contexto

A Fase 013 foi encerrada por `product_reorientation`, sem reabrir treinos
iniciados pelo celular e sem remover migrations, schemas, dados ou infraestrutura
histórica. A direção vigente prioriza treinos iniciados e encerrados pelo
Champion C001:

```text
Champion C001 inicia e encerra
        ↓
importação D3/D4 explícita
        ↓
derivação de métricas
        ↓
revisão local
        ↓
sincronização Health Connect
```

O parser atual importa passos, distância, calorias e BPM por pacote temporal,
mas interpreta os 24 bits em `bytes[7..9]` como um único contador de passos,
fixa todos os intervalos em 60 segundos e calcula ritmo/velocidade usando a
distância original do relógio. Evidência física posterior confirmou que o valor
é empacotado: os passos estão no byte superior e os 16 bits inferiores seguem
sem significado comprovado.

## Objetivo

Entregar planejamento executável para corrigir a leitura temporal D4, persistir
fatos medidos sem perder valores brutos, configurar comprimento pessoal do
passo, derivar uma série canônica reproduzível, apresentar métricas e splits,
reprocessar atividades existentes somente por ação explícita e sincronizar a
projeção granular correta com Health Connect.

## Terminologia normativa

- Contratos novos usam `stepLengthMeters` e texto de interface **Comprimento do
  passo**.
- Um passo é uma passada individual. Não multiplicar o total de passos por uma
  passada completa de dois passos.
- `raw`: valor recebido sem semântica inventada.
- `measured`: fato de origem com semântica confirmada.
- `derived`: valor calculado por algoritmo versionado.
- `canonical`: única métrica derivada aceita por consumidores funcionais.

## Requisitos

### R1 — Origem e controle

1. Somente atividades `WATCH_D3_D4` entram na nova derivação.
2. Importação e sincronização permanecem explícitas e foreground.
3. Alteração de perfil não recalcula nem ressincroniza atividades antigas.
4. Fase 013 permanece fechada; infraestrutura histórica permanece preservada.

### R2 — Comprimento pessoal do passo

1. V1 mantém um valor pessoal, fixo e positivo de `stepLengthMeters`.
2. Origens previstas: `MANUAL`, `HEIGHT_SUGGESTION_ACCEPTED` e
   `REFERENCE_DISTANCE_CALIBRATION`.
3. Entrega inicial implementa entrada manual e sugestão baseada na altura,
   sempre com confirmação explícita antes de persistir.
4. Ausência ou invalidade mantém distância canônica ausente.
5. Evidência combinada de `7.201` passos e `6.350 m` produz aproximadamente
   `0,8818 m`, mas não vira constante global, default ou fallback.
6. A fórmula exata e a apresentação da sugestão por altura devem reutilizar ou
   explicitar política existente sem persistir sugestão não aceita.

### R3 — Decodificação e preservação D4

Para cada pacote temporal:

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

1. Preservar `rawMotionValue` integral.
2. Usar somente `decodedSteps` como passos confirmados do intervalo.
3. Preservar os 16 bits inferiores como componente desconhecido, sem nome
   semântico ou cálculo funcional.
4. Preservar distância e calorias temporais atuais como valores brutos. Não as
   tratar como métricas semânticas enquanto não fecharem com totais e unidades.
5. A soma observada de 28 intervalos deve permanecer `3.737` passos no caso de
   referência.

### R4 — Duração efetiva dos intervalos

Sejam `T` a duração total positiva, `N` a quantidade de intervalos existentes e
`Q = 60 s`:

1. `T == N × Q`: todos os `N` intervalos duram 60 segundos.
2. `(N−1) × Q < T < N × Q`: os primeiros `N−1` duram 60 segundos e o último
   dura `T − (N−1) × Q`.
3. `T <= (N−1) × Q`: não existe duração positiva compatível para o último
   intervalo; preservar fatos brutos, marcar série temporal como inelegível e
   não truncar, redistribuir ou sintetizar intervalos.
4. `T > N × Q`: os `N` intervalos duram 60 segundos; a cauda sem pacote
   permanece parte da sessão, sem intervalo sintético e sem métricas inventadas.
5. Índices ausentes permanecem lacunas. Não criar buckets para preenchê-los.
6. Janelas válidas são positivas, ordenadas, não sobrepostas e contidas na
   sessão.

Exemplo confirmado: `T = 1.637 s`, 27 intervalos completos de 60 segundos e
último intervalo de 17 segundos.

### R5 — Snapshot e derivação V1

Cada atividade derivada congela:

```text
stepLengthMetersUsed
stepLengthSource
derivationMethod = FIXED_PERSONAL_STEP_LENGTH
derivationVersion
derivedAt
```

Com mesma passada congelada:

```text
distância = passos × stepLengthMeters
cadência = passos ÷ duração em minutos
velocidade = distância ÷ duração
ritmo = duração ÷ distância em quilômetros
```

Intervalo com zero passos produz distância, cadência e velocidade iguais a zero
e ritmo ausente. V1 não suaviza, redistribui nem normaliza a série. A distância
canônica total usa passos totais válidos; a série usa passos decodificados dos
intervalos. Divergência entre total e soma temporal fica diagnóstica e torna a
projeção temporal inelegível, sem residual artificial.

### R6 — Consumidores canônicos

Resumo, ritmo, velocidade, distância acumulada, splits, BPM por quilômetro,
gráficos e Health Connect devem usar exclusivamente distância canônica. Distância
original do relógio permanece visível somente em contexto diagnóstico e nunca
é fallback silencioso. Atividade sem distância canônica preserva ausência segura.

### R7 — Splits

1. Produzir limites cumulativos em `1.000 m`, `2.000 m`, `3.000 m` e assim por
   diante.
2. Quando limite cair dentro de intervalo, interpolar tempo linearmente pela
   fração da distância daquele intervalo.
3. Calcular BPM do split pela sobreposição temporal entre amostras reais e janela
   do split; não sintetizar média quando não houver amostra elegível.
4. Série inelegível não produz splits.

### R8 — Health Connect

Projetar, após ação explícita:

- um `ExerciseSessionRecord`;
- `DistanceRecord` incremental para cada intervalo elegível com distância
  positiva;
- `SpeedRecord` com amostras derivadas;
- `HeartRateRecord` com amostras reais;
- `TotalCaloriesBurnedRecord` com total elegível.

Adicionar `HealthSyncRecordType.SPEED`. Operação `DISTANCE` deve suportar
múltiplos records. Distância original não pode ser enviada. Agregado legado e
série granular não podem coexistir.

Cada filho usa ID determinístico por atividade, tipo e índice estável da fonte;
IDs não incluem valores de saúde. Versão é monotônica por `clientRecordId`,
permanece igual em retry idêntico e aumenta quando payload congelado muda.
Persistir manifesto/projeção enviada, remover filhos próprios obsoletos por ID
exato, reconciliar agregado legado conhecido e retomar falha parcial de forma
idempotente. Sem ID legado conhecido, falhar fechado em vez de apagar por janela.

### R9 — Reprocessamento

1. Nova importação elegível pode criar derivação inicial.
2. Atividade existente só é reprocessada por ação explícita.
3. Reprocessamento preserva `recordedAt`, revisão, tipo, título, observações,
   calibração, dados brutos e workflow compatível.
4. Reprocessamento não dispara sincronização Health Connect automaticamente.
5. Atividade já sincronizada exige confirmação explícita antes de criar nova
   projeção; nenhuma migration executa derivação ou efeito remoto.

## Critérios de aceite

1. Perfil aceita valor manual e sugestão por altura somente após confirmação.
2. Sem `stepLengthMeters` válido, distância canônica e dependentes permanecem
   ausentes.
3. Parser preserva `rawMotionValue` e decodifica sequência de referência com
   soma `3.737`.
4. Bits inferiores, distância raw e calorias raw não recebem significado não
   comprovado.
5. Quatro relações entre `T`, `N` e 60 segundos seguem R4 sem intervalo
   sintético.
6. Snapshot torna derivação antiga imune à edição posterior do perfil.
7. Fórmulas V1, zero passos, acumulado e splits seguem R5/R7.
8. Consumidores não usam distância original como fallback.
9. Reprocessamento de legado ocorre somente após confirmação e não altera
   revisão nem sincroniza automaticamente.
10. Health Connect recebe sessão, série positiva de distância, velocidade,
    BPM real e calorias elegíveis, sem agregado concorrente.
11. Retry reutiliza IDs, versões e payloads; filhos obsoletos próprios são
    removidos por identidade exata.
12. Fase 013 continua `closed`; migrations e schemas históricos permanecem.
13. Validação física final registra comparação com atividade de referência e
    provider Health Connect real.

## Comportamento atual encontrado

- `ChampionD4WorkoutParser.parseSamplePacket` lê
  `bytes.readUInt24LittleEndian(7)` diretamente como passos.
- `WorkoutInterval` guarda 60 segundos, passos, distância e calorias
  interpretadas; não guarda valor de movimento bruto.
- Parser calcula ritmo e velocidade pela distância original.
- `WorkoutMetrics.endedAt` resolvido não é informado pelo parser e cai no default
  `startedAt + duration`.
- `ChampionDatabase` está em `v8`; não existe migration `8 → 9`.
- `UserProfile` persiste somente altura. `DailySummaryPolicy` possui conceito
  legado `strideLengthMeters` e sugestão `altura × 0,415`, sem confirmação
  persistida para treino.
- Calendário, detalhe e Health Connect priorizam distância calibrada/original
  antes da estimada; isso não atende distância canônica da Fase 014.
- Health Connect possui um `DISTANCE` agregado por atividade, sem `SPEED`.
- Fila já possui `operationSequence`, mas identidade natural e mapper de
  atividade ainda assumem uma operação sem payload congelado por tipo.
- Sync de atividade permanece explícito, write-only e foreground.
- Testes automatizados existem no repositório para parser, domínio, Room e
  coordenadores, mas não serão criados nem executados nesta fase sem pedido
  explícito.

## Abordagem escolhida

1. Auditar layout e consumidores antes de alterar contratos.
2. Separar fatos raw/medidos de snapshots derivados, conforme ADR-018.
3. Fazer evolução Room aditiva, preservando todas as estruturas históricas.
4. Corrigir parser e resolver duração antes da derivação.
5. Persistir comprimento pessoal confirmado e snapshot por atividade.
6. Derivar total e série de forma determinística, depois splits e UI.
7. Reprocessar legado somente por comando explícito.
8. Congelar projeções Health Connect e reconciliar legado antes da validação
   física final.

## Alternativas descartadas

- Usar distância original como canônica ou fallback: viola ADR-017.
- Usar `0,8818 m` como constante global: evidência pessoal não generalizável.
- Interpretar 16 bits inferiores: não há evidência.
- Distribuir diferença entre total e intervalos: inventa métrica.
- Recalcular no mapper/UI em toda leitura: quebra reprodução e retry.
- Sobrescrever campos raw com derivados: perde diagnóstico.
- Reprocessar em migration, abertura do app ou mudança de perfil: mutação
  silenciosa.
- Manter único `DistanceRecord` agregado: impede série temporal.
- Inserir série antes de reconciliar agregado legado: causa dupla contagem.
- Usar leitura remota ou deleção ampla no Health Connect: viola write-only e
  pode afetar records de terceiros.

## Arquivos, módulos e contratos afetados

- Parser/protocolo: `ChampionD4WorkoutParser`, contratos D4 e documentação.
- Domínio: `WatchWorkoutActivity.kt`, `DeviceProfileDailySummary.kt`,
  `HeartRateAndSync.kt`, `RepositoryContracts.kt`.
- Persistência: `ChampionDatabase.kt`, `ChampionDaos.kt`,
  `ChampionMigrations.kt`, `DatabaseModule.kt`, `DatabaseMappers.kt`,
  `RoomRepositories.kt` e novo schema exportado.
- Perfil/UI: `feature/profile`, `feature/exercises` e consumidores de calendário.
- Health Connect: `HealthRecordMapper`, `HealthConnectSyncCoordinator`, gateway,
  permissões e fila/projeções.
- Documentação: ADR-018, roadmap, baselines e protocolo D3/D4.

## Migração prevista

Evolução provável `v8 → v9`, aditiva e manual. Contrato exato será fechado em
`014-02` antes de `014-03`, mas deve contemplar:

- `stepLengthMeters`, origem e instante de atualização no perfil;
- valores raw confirmados e duração efetiva por intervalo;
- snapshot de derivação e série derivada sem sobrescrever fatos importados;
- manifesto/projeções filhas Health Connect, discriminador estável, ação e
  payload suficiente para retry idempotente;
- índices e constraints para uma derivação ativa por atividade e filhos únicos.

Migration não deriva métricas, não acessa Health Connect, não altera workflow e
não remove tabela, coluna, migration ou schema histórico.

## Riscos e mitigação

- **Semântica D4 incompleta:** preservar raw e marcar desconhecido.
- **Duração incompatível:** falhar fechado apenas na série temporal, preservando
  atividade e fatos.
- **Passada ausente/desatualizada:** ausência explícita; snapshot impede mudança
  retroativa.
- **Conflito com calibração de esteira:** manter calibração histórica, mas
  distância canônica `WATCH_D3_D4` segue passos; decisão de produto prevalece.
- **Fan-out Health Connect:** processar conjunto finito e retomável, sem compactar
  em agregado.
- **Versão legada alta:** semear versão nova acima da maior versão persistida por
  ID; nunca regredir.
- **Agregado legado desconhecido:** não apagar por janela e não criar duplicata.
- **Falha entre delete e insert:** persistir checkpoint e retomar pelos mesmos
  IDs.
- **Reimportação:** preservar revisão e `recordedAt`, conforme ADR-016.
- **Privacidade:** valores raw, passo pessoal e dados de saúde não entram em logs,
  IDs, exportações ou backup.

## Estratégia de validação

Não criar nem executar testes automatizados sem pedido explícito. Cada subtarefa
usa revisão de diff, inspeção de schema/contratos e validação manual proporcional.
Mudanças futuras de código/Gradle devem seguir comandos de build permitidos em
suas tasks; esta execução documental usa somente:

```bash
git diff --check
git status --short
```

Gate final físico deve comparar passos, duração, distância, ritmo, splits,
gráficos e Health Connect com atividades de referência, sem GPS, rota ou treino
phone.

## Ordem das subtarefas

```text
014-01
   ↓
014-02
   ↓
014-03
   ├── 014-04 → 014-05 ─┐
   └── 014-06 ──────────┤
                        ↓
                      014-07
                        ↓
                      014-08
                     ┌──┴──┐
                     ↓     ↓
                   014-09  014-11
                     ↓
                   014-10
                     ↓
                   014-12
                     ↓
                   014-13
                     ↓
                   014-14
```

`014-14` exige todas as subtarefas anteriores concluídas, inclusive `014-11`,
mesmo quando o ramo não cruza o caminho de UI/Health Connect.

## Fora do escopo

- GPS, rota, mapa e `ExerciseRoute`;
- treino phone e BLE live;
- WorkManager como objetivo funcional;
- importação silenciosa;
- TCX, FIT ou GPX;
- passada variável por cadência;
- suavização de métricas;
- calorias derivadas por intervalo;
- remoção de migrations, schemas, tabelas ou dados históricos da Fase 013;
- leitura Health Connect e deleção por janela;
- testes automatizados sem pedido explícito.

## Premissas explícitas

- `014` é ticker explícito fornecido pelo usuário, apesar de existirem specs
  independentes com números maiores.
- ADR-018 está disponível e será usada para mecanismo durável, sem duplicar a
  decisão de produto da ADR-017.
- `REFERENCE_DISTANCE_CALIBRATION` entra no contrato, mas fluxo inicial exigido
  cobre manual e sugestão por altura.
- Distância total canônica pode existir sem série temporal elegível; splits,
  gráficos temporais e filhos granulares permanecem ausentes nesse caso.
- Calorias totais elegíveis continuam medidas da origem; calorias temporais raw
  não ganham semântica nesta fase.
- Reprocessamento de atividade `SYNCED` exige confirmação e nova ação de sync;
  nunca modifica Health Connect implicitamente.
