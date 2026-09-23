# 036 — Histórico de zonas de frequência cardíaca e distribuição de intensidade

- **Ticker:** `036`
- **Status:** `completed`
- **Tipo:** evolução local de read model e apresentação histórica
- **Base auditada:** `main` em `91bbaca` (`fix(exercises): decouple cardiovascular history`), com Spec 034 e Spec 035 concluídas, Room v20
- **Branch de trabalho:** `feat/036-heart-rate-zone-history`
- **ADR:** [ADR-036](../decisions/adr-036-heart-rate-zone-history.md)

## Contexto

A Spec 034 responde como foi a intensidade cardiovascular de um treino. Ela já
possui `HeartRateZone`, `HeartRateZoneSummary`,
`WorkoutHeartRateAnalysisPolicy` e `WorkoutHeartRateAnalysisResult`, com zonas
`BELOW_ZONE_1`, `Z1`, `Z2`, `Z3`, `Z4` e `Z5`, sample-and-hold, cobertura e carga
Edwards.

A Spec 035 agrega a mesma análise em histórico de carga cardiovascular. Ela já
possui as janelas `current7`, `previous7`, `current28` e `previous28`, buckets
diários, estados tipados e a leitura bounded corrigida posteriormente em
`035-08`. A correção removeu dependências de derivação, snapshots, splits,
headers e distância da fonte histórica.

A necessidade seguinte é responder: **em quais zonas de frequência cardíaca o
usuário passou tempo de treino?** A resposta deve ser uma nova projeção da
análise individual já validada, não uma nova interpretação da frequência
cardíaca.

### Auditoria do estado atual

- `WorkoutHeartRateAnalysisPolicy` classifica diretamente pela razão
  `bpm / configuredMaximumBpm`, usa a janela canônica da sessão e expõe todas as
  seis zonas quando há análise `AVAILABLE`.
- `CardiovascularLoadWorkoutInput` contém todos os fatos necessários:
  `activityOrigin`, `workflowState`, `startedAt`, `duration`, tipo confirmado ou
  sugerido e `heartRateSamples`.
- `observeCardiovascularLoadWorkoutsBetween()` é bounded, batched, limitado a
  56 dias, filtrado por `WATCH_D3_D4` e não descartado, e não depende de
  derivação, snapshots, splits, headers ou distância.
- `CardiovascularLoadHistoryPolicy` calcula os quatro períodos a partir de
  `HistoryWindows.from(asOf)` e chama a policy 034 uma vez por workout elegível.
  A 036 estenderá essa mesma agregação; não haverá segunda policy nem segunda
  implementação das janelas.
- `ExercisesViewModel` combina a leitura bounded com
  `PersonalMetricsRepository.observeSnapshot()`. `maxHeartRateBpm`, `asOf` e
  `ZoneId` são capturados de modo estável para a composição.
- `CardiovascularLoadHistoryScreen` já é página própria contextual de Exercícios
  e reutiliza `CardiovascularLoadHistorySection`. Não será criada nova aba ou
  destino.
- `ChampionBarChart` usa `ColumnCartesianLayer.MergeMode.Grouped`. O contrato
  `ChampionCartesianChartData` e o tema atual não possuem empilhamento nem
  paleta estável de seis séries. Não é seguro simular stacked bars passando seis
  séries ao renderer atual.
- As cores visuais das zonas já aparecem no detalhe da Spec 034: `Z5` usa
  `error`, `Z4` `tertiary`, `Z3` `primary`, `Z2` `secondary`, `Z1`
  `onSurfaceVariant` e `BELOW_ZONE_1` `outline`. Essa associação será
  reutilizada, sem segunda paleta.
- Não existia implementação ou documentação da Spec 036 no checkout auditado.
  A branch de trabalho foi criada a partir do `main` auditado e não deve ser
  recriada.

## Objetivo

Adicionar distribuição histórica de tempo por zona para `current7` e
`current28`, com distribuição diária dos últimos 28 dias, usando exatamente os
resultados individuais da Spec 034 e a população/janelas da Spec 035.

A entrega deve mostrar duração e percentual do tempo de frequência cardíaca
analisado, cobertura geral separada, contadores de treinos e parcialidade. Deve
recalcular quando `maxHeartRateBpm` mudar, sem alterar fatos persistidos,
workflows, snapshots ou Health Connect.

## Abordagem escolhida

### 1. Estender o read model cardiovascular existente

Escolha **A** da auditoria: estender `CardiovascularLoadHistory` e
`CardiovascularLoadHistoryPolicy`, em vez de criar policy/read model paralelo.

O resumo de período e o bucket diário receberão uma coleção opcional de
`HeartRateZoneSummary` (nome final pode ser `zones` ou equivalente). O tipo já
possui `zone`, `duration` e `percentage`; nenhum tipo paralelo de zona será
criado.

- Em período ou dia com ao menos uma análise `AVAILABLE`, a coleção conterá
  exatamente as seis zonas, inclusive entradas com duração zero.
- Em período com população sem análise `AVAILABLE`, a coleção ficará ausente de
  forma tipada; zero não representará ausência.
- Em dia sem workout, contadores continuarão distinguindo o zero real de um dia
  com workout sem análise. A representação poderá expor seis durações zero para
  o zero real, mas nunca poderá apagar `workoutCount > 0` e
  `analyzedWorkoutCount == 0`.

Uma chamada de `WorkoutHeartRateAnalysisPolicy` por workout elegível será
reutilizada para todos os períodos, buckets e projeções. A política não será
executada novamente por zona, período ou dia.

### 2. Reutilizar janelas sem divergência

`HistoryWindows.from(asOf)` continuará sendo a única origem de:

```text
current7:  [asOf - 7 dias,  asOf)
previous7: [asOf - 14 dias, asOf - 7 dias)
current28: [asOf - 28 dias, asOf)
previous28:[asOf - 56 dias, asOf - 28 dias)
```

Não será criada segunda implementação temporal nem nova leitura. A extração de
`HistoryWindows` para um contrato compartilhado somente será feita se a
implementação demonstrar necessidade real; nesse caso deve ser pequena,
behavior-preserving e validada contra as saídas atuais da 035. A opção primária
é manter a lógica privada na policy existente e adicionar a projeção de zonas no
mesmo fluxo.

### 3. Fallback visual temporal

Stacked bars não serão implementadas no renderer compartilhado nesta spec. A
distribuição temporal será uma barra de uma única série por vez, usando o
`ChampionBarChart` já suportado, com seleção local entre as seis zonas. A série
temporal usará cor neutra do renderer; a identidade da zona virá do label,
semantics e rows que reutilizam exatamente as cores da Spec 034. Assim, cada
zona pode ser inspecionada sem alterar Vico, sem inventar uma abstração gráfica
ampla e sem confundir barras agrupadas com empilhadas.

O gráfico atual de carga diária da 035 permanece. A nova visualização terá
labels, descrição agregada, duração/percentual nos resumos e indicação textual
de dias sem treino, workout sem análise e cobertura parcial. A seleção 7/28 e a
seleção da zona serão somente estado de apresentação; não mudarão consulta,
policy ou população.

## Requisitos funcionais

### R1 — Fonte única da análise

1. Cada workout deve ser analisado diretamente por
   `WorkoutHeartRateAnalysisPolicy`.
2. A 036 deve consumir `WorkoutHeartRateAnalysisResult.Available.analysis.zones`
   para zonas agregáveis.
3. Não duplicar classificação, thresholds, sample-and-hold, validação de BPM,
   cobertura, tratamento de BPM acima da máxima ou cálculo temporal.
4. Não arredondar BPM para decidir zona.
5. Não criar fórmula ou interpretação alternativa de intensidade.

### R2 — População

Usar exatamente a população da 035:

- `origin = WATCH_D3_D4`;
- `workflowState != DISCARDED`;
- `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`
  e `SYNC_FAILED` permanecem elegíveis;
- estado Health Connect não interfere;
- pertencimento ao período é definido por `startedAt`;
- workout não é dividido por duração ou mudança de dia.

Reutilizar `CardiovascularLoadWorkoutInput` e
`observeCardiovascularLoadWorkoutsBetween()`. Não criar query, DAO, repository,
Flow por activity ou hidratação paralela.

### R3 — Janelas

No mínimo, expor `current7` e `current28` com a mesma semântica temporal da
035. A leitura máxima continua `[asOf - 56 dias, asOf)`. `asOf` e `ZoneId`
continuam explícitos e estáveis, sem `Instant.now()` dentro da policy.

Buckets diários usam a janela `current28`, convertem `startedAt` com o mesmo
`ZoneId` e atribuem o workout inteiro ao dia local de início. A sessão não é
dividida entre dias. O número de datas segue os buckets civis já definidos pela
035: normalmente 28, podendo incluir datas locais tocadas nas duas bordas
quando `asOf` não está alinhado ao início de um dia ou atravessa DST. A policy
não deve cortar datas para forçar 28 pontos.

### R4 — Resumo por zona

Para cada período com ao menos um resultado `AVAILABLE`, expor todas as zonas:

```text
zone
duration
percentage
```

A duração da zona é a soma das durações da mesma zona em todas as análises
`AVAILABLE` do período. O denominador exato do percentual é:

```text
totalAnalyzedZoneDuration =
    soma das durações de todas as zonas AVAILABLE

zonePercentage = zoneDuration / totalAnalyzedZoneDuration
```

Esse denominador é a soma da duração de FC coberta pelas análises `AVAILABLE`,
não `totalWorkoutDuration`. Exemplo: 25 minutos em Z2 de 50 minutos analisados
resultam em 50%, ainda que o treino total tenha 60 minutos.

Invariantes:

- a soma das durações das seis zonas fecha
  `coveredHeartRateDuration` das análises `AVAILABLE`, dentro da precisão de
  `Duration`/nanos;
- a soma dos percentuais é aproximadamente 100% quando a duração analisada é
  positiva;
- cada zona existe, inclusive `BELOW_ZONE_1` e zonas com duração zero;
- carga cardiovascular `0.0` não remove zonas: um workout todo em
  `BELOW_ZONE_1` continua `AVAILABLE` e participa normalmente;
- percentuais de zona nunca usam a duração total do workout.

`coverageRatio` continua conceito separado:

```text
coverageRatio =
    coveredHeartRateDuration / totalWorkoutDuration
```

Essa cobertura pode ser exibida simultaneamente com o percentual dentro do
tempo analisado.

### R5 — Estados e precedência

O resultado histórico mantém estados equivalentes a:

- `AVAILABLE`;
- `CONFIGURATION_REQUIRED`;
- `NO_WORKOUTS`;
- `NO_ANALYZABLE_WORKOUTS`;
- `UNAVAILABLE`.

Precedência:

1. falha estrutural de fonte/composição: `UNAVAILABLE`;
2. população vazia: `NO_WORKOUTS`;
3. população existente sem `maxHeartRateBpm`: `CONFIGURATION_REQUIRED`;
4. população existente com máxima, mas sem workout `AVAILABLE`:
   `NO_ANALYZABLE_WORKOUTS`;
5. ao menos um workout `AVAILABLE`: `AVAILABLE`.

Se houver workouts `AVAILABLE` misturados com `NO_HEART_RATE_SAMPLES` ou
`WorkoutHeartRateAnalysisResult.Unavailable`, o período continua `AVAILABLE`,
com `analyzedWorkoutCount` contando somente `AVAILABLE`,
`totalWorkoutCount` contando toda a população e `partial = true`.

Não transformar ausência em lista vazia, carga zero ou dia zero indistinguível.

### R6 — Cobertura e parcialidade

O resumo expõe pelo menos:

- `totalWorkoutCount`;
- `analyzedWorkoutCount`;
- `totalWorkoutDuration`;
- `coveredHeartRateDuration`;
- `coverageRatio`;
- `partial`;
- distribuição das seis zonas quando houver `AVAILABLE`.

`partial` é verdadeiro quando algum workout elegível não é `AVAILABLE` ou algum
workout `AVAILABLE` possui `coverageRatio < 1`. Não haverá threshold arbitrário.
Cobertura 98,4% continua válida e contribui somente com a duração efetivamente
coberta.

### R7 — Buckets diários

Cada bucket de `current28` expõe pelo menos:

- `localDate`;
- durações das seis zonas, ou coleção equivalente;
- `workoutCount`;
- `analyzedWorkoutCount`;
- `coveredHeartRateDuration`;
- `totalWorkoutDuration`;
- `coverageRatio`;
- `partial`.

O bucket deve distinguir:

1. nenhum workout: `workoutCount == 0`, zero real permitido;
2. workout analisado: coleção de zonas presente, inclusive zeros;
3. workout existente sem análise: `workoutCount > 0`,
   `analyzedWorkoutCount == 0`, sem coleção numérica sintética;
4. cobertura parcial: zonas presentes quando houver análise, `partial == true`.

A soma das durações de cada zona em todos os buckets deve fechar a duração da
mesma zona em `current28`. A soma das seis zonas de todos os buckets deve fechar
a duração analisada de `current28`.

### R8 — Interface

Evoluir `CardiovascularLoadHistoryScreen` e o conteúdo reutilizado da página
contextual de Exercícios. Não criar nova aba principal, destino ou entrada em
Home/Saúde.

A página deve preservar a carga histórica da 035 e acrescentar:

- seleção simples `7 dias`/`28 dias`, se reduzir duplicação visual;
- resumo selecionado com seis zonas, duração e percentual do tempo analisado;
- `X de Y treinos analisados`, cobertura de FC e duração coberta;
- `Abaixo de Z1` explicitamente;
- distribuição diária de zonas de `current28`;
- estados de configuração, vazio, ausência de análise, parcial e falha.

Não usar textos que avaliem o treino como ideal, excessivo, insuficiente,
produtivo, perigoso ou que recomendem recuperação/treino.

### R9 — Cores e acessibilidade

Reutilizar exatamente a associação visual da Spec 034. Se necessário, extrair
um helper pequeno de apresentação para compartilhar label, ordem e cor; não
criar paleta paralela nem usar cor como único canal.

Cada zona deve ter label textual, duração, percentual e semantics/content
description. A distribuição diária deve anunciar período, zona selecionada,
duração, dias sem workout, dias sem análise e parcialidade quando aplicável.

### R10 — Performance e efeitos proibidos

- manter uma leitura bounded de no máximo 56 dias;
- não carregar histórico completo;
- não criar N+1 ou Flow por activity;
- não acessar DAO no ViewModel;
- não persistir resultado, cache, snapshot, tabela ou coluna;
- não criar migration nem alterar Room v20;
- não alterar `WorkoutActivity`, `workflowState`, snapshots, derivação, splits ou
  `health_sync_operations`;
- não alterar permissões, records, payloads, IDs, versões, fila, retry,
  reconciliação ou readback do Health Connect;
- abrir/recompor a tela somente recompõe analytics em memória;
- mudar `maxHeartRateBpm` recalcula a distribuição histórica e restaurar o valor
  restaura a saída anterior.

## Critérios de aceite

1. `current7` e `current28` usam a população e as janelas já validadas pela 035.
2. A fonte bounded da 035 é reutilizada sem depender de derivação, snapshots,
   splits, headers ou distância.
3. Cada análise individual usa `WorkoutHeartRateAnalysisPolicy`, sem fórmula
   ou classificação duplicada.
4. Períodos `AVAILABLE` contêm `BELOW_ZONE_1`, `Z1`–`Z5`, inclusive durações
   zero.
5. Soma de durações por zona fecha a duração de FC analisada.
6. Percentuais usam somente a duração analisada como denominador e fecham
   aproximadamente 100% quando positiva.
7. `coverageRatio` permanece independente dos percentuais de zona.
8. Carga zero continua sendo análise válida e não remove zonas.
9. Workout sem análise não derruba período que possui workouts `AVAILABLE` e
   produz `partial = true`.
10. Precedência dos cinco estados permanece explícita e ausência não vira zero
    semântico.
11. Buckets diários distinguem dia sem workout de workout sem análise e fecham
    `current28` por zona.
12. A tela contextual mostra resumos de 7/28 dias, distribuição factual,
    cobertura, contadores e `Abaixo de Z1`.
13. Gráfico temporal não simula stacked bars com renderer grouped; usa a opção
    de zona única selecionável ou outra opção suportada documentada nesta spec.
14. Cores da Spec 034 são reutilizadas exatamente nas rows de zona; o gráfico
    temporal pode usar cor neutra do renderer, desde que label e semantics
    identifiquem a zona e informação não dependa somente de cor.
15. Alterar e restaurar `maxHeartRateBpm` recompõe e restaura resultados sem
    reescrever fatos persistidos.
16. Room permanece v20 sem migration, tabela, coluna, cache ou backfill.
17. Nenhum caminho da 036 cria ou altera operação Health Connect; eventual ruído
    preexistente de inicialização é separado da causalidade da feature e
    registrado como limitação, sem declarar assinatura absoluta quando ela não
    puder ser isolada.
18. Validação física usa workouts reais existentes, registra N/A quando cenário
    não existir naturalmente e não fabrica treino/sample.
19. Screenshot sanitizada não expõe BPM individual, activity ID, MAC, payload
    BLE ou dados pessoais desnecessários.

## Alternativas descartadas

- **Criar policy/read model paralelo de zonas:** duplicaria população, janelas,
  estados, leitura e chamadas da policy 034.
- **Criar nova query ou repository:** a fonte bounded atual já contém todos os
  fatos necessários e foi corrigida justamente para não depender de derivação.
- **Reidratar `WorkoutActivity` completo:** reabriria a dependência de snapshots,
  splits e derivação que causou a regressão Release da 035.
- **Reimplementar classificação ou sample-and-hold:** permitiria divergência da
  Spec 034 em fronteiras, BPM acima da máxima e cobertura.
- **Usar `totalWorkoutDuration` no percentual:** confundiria percentual do tempo
  analisado com cobertura do treino.
- **Omitir `BELOW_ZONE_1` ou zonas zero:** quebraria fechamento e confundiria
  carga zero com ausência.
- **Implementar stacked bars no renderer atual:** `ChampionBarChart` usa
  `MergeMode.Grouped`; seis séries não seriam empilhadas e exigiriam alteração
  desproporcional do design system.
- **Criar nova abstração gráfica compartilhada:** não há requisito para ampliar
  o renderer fora desta feature; a barra de zona única usa infraestrutura
  existente.
- **Persistir distribuição:** impediria recomputação deliberada após mudança da
  FC máxima e exigiria migration/versionamento sem necessidade.
- **Comparar percentuais de zona com período anterior:** fica como evolução
  posterior; a primeira versão permanece factual.

## Arquivos, módulos e contratos afetados

### Planejados para implementação futura

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHeartRateLoadHistory.kt` — extensão aditiva dos resumos e buckets com zonas.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt` — somente se o estado de apresentação exigir adaptador; não duplicar read model.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt` — manter composição única e assegurar recomposição da extensão de zonas.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt` — resumos, rows de zonas, labels/semantics compartilhados e gráfico diário de zona única.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/CardiovascularLoadHistoryScreen.kt` — seleção local de período/zona e composição contextual.
- eventual helper de apresentação de zonas em `feature/exercises` — somente para reutilizar labels, ordem, cores e acessibilidade da Spec 034.

### Explicitamente não afetados

- `RepositoryContracts.kt`, `RoomRepositories.kt`, `DatabaseMappers.kt` e
  `ChampionDaos.kt`, salvo nenhuma alteração necessária comprovada na execução;
  a fonte bounded será reutilizada sem mudança.
- `ChampionDatabase.kt`, migrations e schemas exportados;
- `WorkoutActivity`, `WorkoutHeartRateSample`, snapshots, derived intervals e
  splits;
- parsers D1/D3/D4, BLE, workflow e calibração;
- `HealthProjectionContracts`, writer, fila, permissões, payloads, IDs,
  versões, retry, reconciliation e readback Health Connect;
- navegação principal, Home, Saúde, Perfil e Settings.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Regressão na carga 035 ao estender seus contratos | Preservar campos existentes, comparar saídas antes/depois e validar carga, status, buckets e tipos independentemente. |
| Segunda implementação temporal divergir | Manter `HistoryWindows.from(asOf)` como único dono e uma única execução por workout. |
| Percentual ser calculado sobre duração total | Congelar denominador como soma das zonas `AVAILABLE` e validar exemplo 25/50 contra 25/60. |
| Carga zero ser confundida com ausência | Exigir análise `AVAILABLE` com seis zonas, inclusive `BELOW_ZONE_1` e zeros. |
| Workout sem análise parecer dia vazio | Manter contadores, `partial` e coleção numérica ausente para população sem análise. |
| Gráfico grouped ser interpretado como stacked | Não passar seis séries ao `ChampionBarChart`; usar uma única série selecionável e documentar semantics. |
| Cores duplicarem ou avaliarem esforço | Reutilizar mapeamento da 034 e fornecer labels/duração/percentual fora da cor. |
| Custo bounded sem índice temporal composto | Não ampliar range nem criar migration especulativa; manter risco 035 documentado. |
| Alteração da FC máxima persistir analytics | Recalcular somente read model em memória e comparar tabelas/fatos antes e depois. |
| Evidência física expor dados de saúde | Sanitizar dumps/screenshots e não registrar BPM, samples, IDs, MAC ou payloads. |

## Estratégia de testes e validação

Conforme `AGENTS.md`, não criar testes automatizados novos nesta execução de
planejamento nem tratá-los como etapa padrão da implementação. A implementação
futura deve usar inspeção, build e validação física.

### Prova técnica futura

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- confirmar Room v20, ausência de migration/schema/tabela analítica;
- revisar que repository/DAO não mudaram e a fonte continua bounded em 56 dias;
- revisar uma única chamada de `WorkoutHeartRateAnalysisPolicy` por workout;
- revisar que ViewModel não acessa DAO nem calcula zonas em Compose;
- revisar que nenhum arquivo Health Connect, parser ou BLE mudou.

### Prova física futura

Usar banco/workouts reais já existentes, sem criar treino ou sample:

1. confirmar Room v20 e FC máxima atual reutilizada;
2. comparar zonas individuais com a saída da Spec 034;
3. conferir fechamentos de `current7` e `current28` por zona;
4. conferir percentuais e denominador de tempo analisado;
5. conferir `coverageRatio` independente dos percentuais;
6. conferir buckets diários fechando `current28` por zona;
7. conferir carga `0.0` válida quando existir naturalmente;
8. conferir workout sem análise produzindo `partial`, ou registrar `N/A`;
9. mudar temporariamente `maxHeartRateBpm`, confirmar recomposição e restaurar o
   valor original;
10. comparar `WorkoutActivity`, workflow, snapshots, derived intervals, splits
    e `health_sync_operations` antes/depois;
11. reabrir/recompor a tela e confirmar que analytics não foram persistidos;
12. capturar screenshot sanitizada.

Se um cenário não existir naturalmente, registrar `N/A`, motivo, evidência e
risco residual. Nunca fabricar workout/sample apenas para completar checklist.

## Ordem das subtarefas

1. `036-01` — auditar zonas históricas, fonte bounded, gráficos e congelar contrato.
2. `036-02` — implementar agregação histórica de zonas na policy existente.
3. `036-03` — integrar extensão de zonas no read model cardiovascular e na recomposição do ViewModel.
4. `036-04` — integrar resumos de zonas na página cardiovascular contextual.
5. `036-05` — renderizar distribuição temporal diária sem stacked bars não suportadas.
6. `036-06` — validar fisicamente, preservar efeitos e fechar documentação.

`036-02` depende de `036-01`. `036-03` depende do contrato e do domínio. As
subtarefas de UI dependem da extensão do read model. `036-05` depende dos
contratos de buckets e da decisão visual congelada. `036-06` fecha somente após
prova independente e registro dos cenários N/A.

## Premissas explícitas

- `main` auditada em `91bbacaf65f2fa0983d64b3552bb30c2a08690a4` contém Spec 034,
  Spec 035 e correção 035-08.
- A referência local de `origin/main` estava atrás do HEAD auditado; não fazer
  fetch ou merge como parte desta documentação. A base factual é a `main` local
  auditada.
- `CardiovascularLoadWorkoutInput` contém todos os fatos necessários à 036; não
  haverá extensão de repository sem descoberta concreta.
- `HeartRateZoneSummary.percentage` será interpretado no agregado como fração da
  duração coberta pelas análises `AVAILABLE`, e no bucket como fração da duração
  analisada daquele dia.
- A população é definida por `startedAt`, e sessões que atravessam meia-noite
  permanecem no dia de início.
- Alteração de FC máxima deve recompor históricos, mesmo que isso altere o
  resultado visual de atividades passadas.
- O produto permanece local-first; a 036 não reabre treino phone, GPS, rota ou
  Health Connect.
- Comparação entre zonas de períodos anteriores fica fora da primeira versão.

## Fechamento físico — 2026-09-23

A Spec 036 foi validada fisicamente no Champion C001 real (`SM-S901E`) usando
somente workouts existentes. Room permaneceu v20; não houve migration, tabela,
coluna, cache ou backfill analítico. O cálculo independente confirmou soma das
seis zonas, percentuais sobre duração analisada, `coverageRatio` separado e
fechamento de `current7`, `current28` e buckets civis. A página exibiu os
resumos, seis zonas, `BELOW_ZONE_1`, parcialidade e distribuição diária por uma
zona selecionada.

A alteração temporária da FC máxima recompôs o histórico e a restauração voltou
à saída anterior. Fatos de workouts, snapshots, derivados, splits e operações
Health Connect permaneceram estáveis no snapshot pós-inicialização durante essa
prova. Abertura do app revelou ruído preexistente não isolável entre snapshots
físicos inicial e pós-inicialização; ele não foi atribuído à 036. Carga zero e
workout sem análise ficaram N/A por ausência natural. Evidência completa:
[`036-06-physical-validation.md`](../integration/036-06-physical-validation.md).
