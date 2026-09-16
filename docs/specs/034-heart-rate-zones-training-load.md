# 034 — Zonas de frequência cardíaca e carga cardiovascular

- **Ticker:** `034`
- **Status:** `planned`
- **Tipo:** evolução local de read model e métricas pessoais
- **Base:** `main` com Room v19 e estado documental da Spec 033
- **Branch de trabalho:** `feat/034-heart-rate-zones-training-load`
- **ADR:** [ADR-034](../decisions/adr-034-heart-rate-zones-training-load.md)

## Contexto

Os detalhes de workouts já exibem uma série temporal de frequência cardíaca
persistida em `WorkoutHeartRateSample`, além de gráfico, média e máxima
observada. A série é fato local importado do relógio e não deve ser substituída
por agregados remotos, por parser novo ou por nova captura BLE.

A auditoria desta spec confirmou:

- `WorkoutHeartRateSample` contém `sampledAt`, `bpm` e índice opcional da fonte,
  com BPM persistido dentro dos limites existentes `30..220`;
- a série está em `workout_heart_rate_samples`, consultável por atividade e
  ordenada temporalmente;
- o cálculo existente de BPM ponderado em splits já usa sample-and-hold;
- ADR-018 define janelas semiabertas e o contrato temporal autoritativo;
- `UserProfile` ainda não possui FC máxima pessoal;
- `PersonalMetricsSnapshot`, `PersonalMetricsRepository`, `ProfileScreen` e
  `ProfileViewModel` ainda não expõem esse campo;
- `ExercisesScreen` usa `selectedActivity` diretamente e calcula apresentação
  de FC na camada visual;
- Room está na versão `19`, com `MIGRATION_18_19` como última migration e
  schema exportado `19.json`;
- `main`, `origin/main` e a branch de trabalho existente apontam para o mesmo
  estado de base no início do planejamento.

Fontes temporais principais:

- [ADR-018](../decisions/adr-018-canonical-workout-time-series-and-health-projection.md), especialmente a seção de splits e frequência cardíaca;
- `core/model/WatchWorkoutDerivedMetrics.kt`, cálculo `weightedBpm`;
- `core/model/WatchWorkoutActivity.kt`, contrato de `WorkoutHeartRateSample`;
- `data/RoomRepositories.kt` e `core/database/ChampionDaos.kt`, leitura e
  persistência da série.

## Objetivo

Adicionar zonas de frequência cardíaca, carga cardiovascular, cobertura temporal
e edição manual de FC máxima aos workouts existentes, usando exclusivamente os
samples de FC já persistidos e a FC máxima pessoal atual.

A entrega deve ser local, derivada sob demanda e sem alterar BLE, parser D1/D4,
workflow de workouts, snapshots de derivação, Health Connect ou projeções
remotas.

## Escopo

### Incluído

- adicionar `maxHeartRateBpm: Int?` a `UserProfile`;
- expor o campo em `PersonalMetricsSnapshot` e no repository de métricas
  pessoais;
- editar FC máxima no Perfil, com explicação de uso em zonas e carga;
- migration aditiva Room v19 -> v20 e schema `20.json`;
- criar domínio puro para zonas, cobertura e Edwards TRIMP;
- reutilizar exatamente sample-and-hold de ADR-018;
- combinar atividade selecionada, samples persistidos e perfil atual em read model
  não persistido;
- mostrar zonas, carga cardiovascular e cobertura no detalhe de Exercícios;
- preservar estados seguros para falta de configuração, falta de samples e
  cobertura parcial;
- validar fisicamente banco existente, atividade existente, atualização do
  perfil e ausência de efeitos remotos.

### Excluído

- idade, nascimento, sexo ou estimativa automática de FC máxima;
- fórmula `220 - idade` ou qualquer outra estimativa;
- DataStore/AppPreferences para FC máxima;
- nova captura BLE ou alteração de sessão BLE;
- alteração dos parsers D1, D3 ou D4;
- alteração da série temporal persistida;
- segunda política temporal de FC;
- fabricação, interpolação ou preenchimento de samples;
- threshold arbitrário para descartar carga parcial;
- persistência de zonas, duração por zona, carga ou snapshot analítico por
  atividade;
- reprocessamento de `WorkoutDerivationSnapshot`;
- alteração de `workflowState`, inclusive para atividades `SYNCED`;
- novas permissões, records, writes, payloads, IDs ou versões Health Connect;
- classificação de workout como baixa, moderada ou alta;
- baseline ou histórico pessoal para classificar carga;
- novos testes automatizados nesta execução;
- GPS, rota, localização ou reativação de treino iniciado pelo celular.

## Requisitos funcionais

### R1 — FC máxima pessoal

1. `maxHeartRateBpm` é opcional, inteiro e pertence a `UserProfile`.
2. Valor configurado manualmente deve estar entre `30` e `220` BPM, inclusive,
   coerente com os limites de BPM já existentes.
3. Valor ausente significa “não configurado”; não existe valor default oculto.
4. A alteração deve ser persistida em `user_profiles`, preservando altura,
   comprimento de passo, peso, origem e timestamp dos demais dados.
5. A alteração não deve iniciar sync Health Connect, alterar fila remota ou
   chamar qualquer operação de Health Connect.
6. A migration v19 -> v20 deve ser aditiva, nullable, sem backfill, sem wipe,
   sem snapshots novos e sem efeitos remotos.
7. Alterar FC máxima deve recalcular apenas o read model visual derivado quando
   o detalhe estiver aberto.

### R2 — Classificação de zonas

Para cada janela de sample-and-hold, classificar usando diretamente a razão:

```text
ratio = bpm.toDouble() / configuredMaximumBpm.toDouble()
```

Limites:

| Bucket | Condição pela razão | Peso |
| --- | --- | ---: |
| `BELOW_ZONE_1` | `< 0.50` | 0 |
| `Z1` | `>= 0.50` e `< 0.60` | 1 |
| `Z2` | `>= 0.60` e `< 0.70` | 2 |
| `Z3` | `>= 0.70` e `< 0.80` | 3 |
| `Z4` | `>= 0.80` e `< 0.90` | 4 |
| `Z5` | `>= 0.90` | 5 |

Os limites BPM arredondados podem aparecer somente como apoio de apresentação.
Eles nunca podem decidir a zona, evitando erro de arredondamento nas fronteiras.

Sample acima da FC máxima configurada permanece válido, não é truncado nem
descartado e classifica como `Z5`. A análise deve expor
`observedAboveConfiguredMaximum = true` quando isso ocorrer.

### R3 — Carga cardiovascular

Calcular carga local pelo somatório de zonas:

```text
load =
    Z1_minutes * 1 +
    Z2_minutes * 2 +
    Z3_minutes * 3 +
    Z4_minutes * 4 +
    Z5_minutes * 5
```

`BELOW_ZONE_1` participa da cobertura, mas possui peso zero. Duração e carga
permanecem `Double` no domínio. Arredondamento ocorre somente na UI.

O nome visível é **Carga cardiovascular**. A documentação e a UI devem deixar
claro que é métrica local do Champion Companion, baseada em Edwards TRIMP /
summated-heart-rate-zones, e não uma tentativa de reproduzir escalas Google,
Fitbit ou Health Connect.

### R4 — Semântica temporal e cobertura

1. A política deve reutilizar exatamente o sample-and-hold de ADR-018:
   janelas semiabertas `[sampledAt_i, sampledAt_{i+1})`, com o último sample
   mantido até o final da janela da sessão.
2. A janela canônica da sessão é `[startedAt, startedAt + duration)`, já usada
   pela série derivada e pelas projeções existentes. `endedAt` não cria uma
   segunda janela quando divergir por pausas.
3. Somente janelas de samples contidas na sessão participam da análise; samples
   fora da janela não criam janelas analíticas nem estendem a sessão.
4. A análise não cria sample, não interpola, não redistribui residual e não
   inventa FC antes do primeiro sample válido.
5. Não criar timeout ou threshold de “sample stale”. Um intervalo longo continua
   obedecendo ao contrato congelado de sample-and-hold; eventual lacuna visual
   do gráfico não cria outra política analítica.
6. Para atividade com samples em janela válida, produzir:
   - `sessionDuration`;
   - `coveredDuration`;
   - `coverageRatio = coveredDuration / sessionDuration`.
7. `coveredDuration` é a soma das sobreposições positivas das janelas de
   sample-and-hold com a sessão. O trecho anterior ao primeiro sample fica sem
   cobertura.
8. `coverageRatio` permanece `Double` no domínio, limitado a `0.0..1.0` por
   invariantes da janela. UI arredonda apenas a representação percentual.
9. Não descartar carga parcial por threshold arbitrário. Com cobertura parcial,
   retornar análise parcial e permitir que UI informe a cobertura.
10. A soma das durações dos buckets, incluindo `BELOW_ZONE_1`, deve ser igual a
    `coveredDuration` dentro da precisão temporal do domínio.
11. Percentuais de zona usam `coveredDuration` como denominador, não a duração
    total da sessão. A soma dos percentuais corresponde à cobertura.

### R5 — Modelo derivado

Criar contratos equivalentes a:

- `HeartRateZone`;
- `HeartRateZoneSummary`;
- `WorkoutHeartRateAnalysis`;
- `WorkoutHeartRateAnalysisPolicy`.

`WorkoutHeartRateAnalysis` deve expor, no mínimo:

- `configuredMaximumBpm: Int?`;
- `observedMaximumBpm`;
- `sessionDuration`;
- `coveredDuration`;
- `coverageRatio`;
- `zones`;
- `cardiovascularLoad`;
- `observedAboveConfiguredMaximum`.

Quando samples existem e FC máxima não está configurada, a análise pode expor
observação e cobertura, mas `zones` e `cardiovascularLoad` devem permanecer
ausentes de forma tipada, nunca lista vazia ou zero interpretável como resultado.
Quando não existem samples válidos na janela, não criar análise numérica para a
UI; retornar estado de indisponibilidade `NO_HEART_RATE_SAMPLES`.

O read model deve distinguir pelo menos:

- `AVAILABLE` — configuração, samples e análise disponíveis;
- `CONFIGURATION_REQUIRED` — samples disponíveis, FC máxima ausente;
- `NO_HEART_RATE_SAMPLES` — não há samples utilizáveis na janela;
- `UNAVAILABLE` — falha ou inconsistência de leitura/composição.

Esses estados não pertencem a `ActivityWorkflowState` e não alteram o workflow.

### R6 — Detalhe de Exercícios

Depois da área/gráfico de **Frequência cardíaca** e antes de **Cadência**, o
detalhe deve inserir:

1. **Zonas de frequência cardíaca**;
2. linhas de `Z5` até `Z1`, com duração e percentual da cobertura;
3. indicação de `Abaixo de Z1` para o bucket de peso zero;
4. **Carga cardiovascular** com valor arredondado somente para apresentação;
5. **Cobertura de FC** com percentual, sobretudo quando parcial;
6. indicador textual de FC acima da máxima configurada quando a flag estiver
   ativa.

Estados obrigatórios:

- sem FC máxima: solicitar configuração em Perfil e não mostrar carga/zona como
  zero;
- sem `heartRateSamples`: informar indisponibilidade e não fabricar valor;
- cobertura parcial: mostrar análise e cobertura, sem bloquear ou invalidar a
  carga;
- cobertura completa: mostrar percentual correspondente;
- falha de leitura: estado de erro seguro, sem detalhes sensíveis.

A UI deve preservar gráficos e métricas de FC existentes, usar cabeçalhos
semânticos, descrições agregadas acessíveis para duração/percentual/carga e
contraste/layout compatíveis com o design system atual.

### R7 — Health Connect e workflows

O comportamento de Health Connect deve permanecer exatamente igual:

- nenhuma permissão nova;
- nenhum record novo;
- nenhuma escrita de carga ou zona;
- nenhum payload existente alterado;
- nenhum `clientRecordId` ou `clientRecordVersion` alterado;
- nenhuma atividade `SYNCED` alterada;
- nenhuma alteração em retry, reconciliação ou fila;
- salvar FC máxima não dispara sync;
- read model nunca usa Health Connect como fonte.

Parser D1/D4, BLE, importação, derivação congelada, `workflowState` e
`WorkoutDerivationSnapshot` permanecem fora da mudança.

## Critérios de aceite

1. Perfil permite configurar, alterar, limpar e persistir FC máxima dentro de
   `30..220`, sem idade, estimativa ou AppPreferences.
2. Migration aditiva leva banco v19 a v20, preservando dados existentes e
   inicializando perfis antigos com `NULL`.
3. Schema exportado v20 contém `maxHeartRateBpm INTEGER` nullable.
4. Usuário vê supporting text explicando que FC máxima é usada para zonas e
   carga cardiovascular.
5. Domínio classifica fronteiras por razão BPM/FC máxima, não por BPM
   arredondado.
6. BPM acima da máxima permanece observado, fica em Z5 e ativa a flag.
7. Edwards TRIMP independente bate com cálculo externo, mantendo `Double` até
   a UI.
8. A política temporal usa o mesmo sample-and-hold de ADR-018, sem segunda
   política, interpolação, residual ou timeout inventado.
9. Análise limita-se à janela canônica real da sessão e expõe duração total,
   duração coberta e razão de cobertura.
10. Cobertura parcial retorna análise parcial; não há threshold para descartá-la.
11. Soma das durações de todos os buckets corresponde à cobertura; percentuais
    usam cobertura como denominador.
12. Sem FC máxima, UI solicita configuração e não exibe carga/zona sintética.
13. Sem samples, UI informa indisponibilidade e não exibe zero ou FC fabricada.
14. Detalhe renderiza zonas, duração, percentuais, carga, cobertura e estado de
    amostra acima da máxima entre FC e Cadência, preservando os gráficos atuais.
15. Alterar FC máxima recalcula zonas/carga de atividades históricas na UI sem
    reprocessar snapshots, mudar workflow ou ressincronizar Health Connect.
16. Workout existente mantém a mesma atividade, estado de workflow e dados
    persistidos após migration e atualização da métrica pessoal.
17. `health_sync_operations` não ganha nem altera operação em consequência da
    feature.
18. Validação física usa banco real atualizado, atividade existente, persistência
    após reinício e screenshot do detalhe atualizado.
19. `git diff --check`, `./gradlew lint assembleDebug assembleRelease` passam;
    nenhum teste automatizado novo é criado.

## Comportamento atual encontrado

### Série temporal

`WorkoutHeartRateSample` já é persistido por atividade, com ordenação por
timestamp e precisão de segundo/nanosegundo quando disponível. O parser calcula
timestamps conforme a versão D4 vigente; esta spec não toca esse caminho.

O cálculo `weightedBpm` em `WatchWorkoutDerivedMetrics` já percorre samples
ordenados, limita cada janela à sessão e usa o próximo sample ou fim da sessão.
Esse helper é a referência para evitar duplicação de semântica temporal. A nova
policy deve extrair ou reutilizar a mesma lógica, sem mudar o comportamento dos
splits existentes.

Risco auditado: há consumidores remotos com verificações de fronteira distintas,
mas eles não são fonte para esta análise e não devem ser corrigidos nesta spec.

### Perfil e Room

`UserProfileEntity` contém altura, comprimento de passo, origem e atualização.
`UserProfileDao` usa leitura/escrita da linha atual, portanto não exige DAO novo
para uma coluna pessoal. `RoomRepositories.observeSnapshot()` compõe
`PersonalMetricsSnapshot` com perfil e último peso.

Atualizações de altura e comprimento de passo reconstruem o perfil. A implementação
da Spec 034 deve preservar `maxHeartRateBpm` nesses caminhos para evitar perda
silenciosa da nova métrica.

### Profile

`ProfileUiState`, `ProfileIntent`, `ProfileMessage`, `ProfileViewModel` e
`PersonalMetricsEditorCard` suportam altura e meta diária de passos. FC máxima
deve ser um editor separado dentro da mesma superfície, com validação inteira,
mensagens próprias e supporting text. Meta diária continua em DataStore.

O fluxo atual de altura pode preparar sync Health Connect. O novo fluxo de FC
máxima deve ser separado e não pode reutilizar esse gatilho remoto.

### Exercises

`ExercisesViewModel` já observa `selectedActivity` e `PersonalMetricsSnapshot`,
mas `ExercisesScreen` compõe parte das métricas diretamente a partir da atividade.
O novo cálculo deve ficar em `core.model` e ser exposto pelo estado do detalhe ou
por read model derivado, deixando a tela responsável apenas por apresentação.

O detalhe atual exibe gráfico de FC quando há pontos e renderiza Cadência depois.
Esse ponto de inserção deve preservar ordem, gráficos, ações de revisão,
reprocessamento, descarte e sync.

## Abordagem escolhida

1. **Domínio puro:** criar policy determinística em `core.model`, recebendo janela
   canônica, samples e FC máxima. Não acessar Room, Health Connect, ViewModel ou
   Compose.
2. **Reuso temporal:** compartilhar a primitiva temporal já usada por
   `weightedBpm`, aplicando antes a regra de janelas contidas na sessão. O
   comportamento incidental de um consumidor que faça clamp de sample externo
   não cria exceção para esta análise; ADR-018 continua sendo a única autoridade.
3. **Read model efêmero:** combinar `selectedActivity` com o snapshot pessoal
   atual no fluxo do detalhe. Não adicionar análise a `WorkoutActivity` como fato
   persistido e não criar tabela de análise.
4. **Estado tipado:** manter disponibilidade de análise separada de
   `ActivityWorkflowState`; falta de configuração e falta de samples não viram
   zero, lista vazia ou sucesso falso.
5. **Persistência mínima:** adicionar uma coluna nullable em `user_profiles`,
   mapeamentos, método de atualização, migration v19 -> v20 e schema exportado.
6. **UI local:** adicionar editor no Perfil e seção no detalhe de Exercícios,
   mantendo gráficos e contratos remotos intocados.
7. **Validação física:** atualizar instalação/banco existente, confirmar dados,
   cálculo independente, ausência de alterações remotas e screenshot.

## Alternativas descartadas

- **Usar idade ou `220 - idade`:** contradiz decisão de produto e cria estimativa
  não configurada.
- **Guardar FC máxima em DataStore:** mistura preferência operacional com métrica
  pessoal persistente e quebra o contrato de perfil.
- **Usar `maximumBpm` da atividade:** esse campo é observado no workout, não
  configuração pessoal.
- **Usar média/máxima agregada do header:** não preserva duração por zona nem
  atende uso exclusivo da série temporal.
- **Criar política temporal nova para zonas:** duplicaria e poderia divergir de
  ADR-018.
- **Usar limites BPM arredondados na classificação:** falha em fronteiras; razão
  é a regra autoritativa.
- **Descartar carga parcial por cobertura mínima:** não foi autorizado e ocultaria
  análise válida.
- **Persistir zonas/carga por atividade:** criaria snapshot/versionamento e
  impediria recálculo deliberado com FC máxima atual.
- **Adicionar cálculo à `ExercisesScreen`:** desloca regra de domínio para UI e
  dificulta validação independente.
- **Enviar carga ao Health Connect:** viola escopo e contratos write-only
  existentes.
- **Reprocessar snapshots históricos:** altera fatos congelados sem necessidade
  e contraria a consequência deliberada de read model dinâmico.

## Arquivos, módulos e contratos afetados

### Domínio e contratos

- `app/src/main/java/.../core/model/WatchWorkoutHeartRateAnalysis.kt` ou nome
  equivalente para zonas, summary, análise, estados e policy;
- `core/model/WatchWorkoutDerivedMetrics.kt`, somente para compartilhar helper
  temporal sem alterar resultados existentes;
- `core/model/DeviceProfileDailySummary.kt`, `UserProfile` e validação;
- `core/model/RepositoryContracts.kt`, `PersonalMetricsSnapshot`, input e método
  de atualização;
- contratos de `ExercisesUiState`/`ExerciseDetailState` para read model da seção.

### Persistência

- `core/database/ChampionDatabase.kt`, `UserProfileEntity` e versão 20;
- `core/database/ChampionMigrations.kt`, `MIGRATION_19_20`;
- `core/database/DatabaseModule.kt`, registro da migration;
- `data/DatabaseMappers.kt`, conversão bidirecional;
- `data/RoomRepositories.kt`, snapshot, update, preservação nos updates e
  composição do detalhe quando necessário;
- `app/schemas/.../20.json`.

### Perfil e Exercícios

- `feature/profile/ProfileFeatureContract.kt`;
- `feature/profile/ProfileViewModel.kt`;
- `feature/profile/ProfileScreen.kt`;
- `app/src/main/res/values/strings.xml`;
- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/ExercisesScreen.kt`.

### Explicitamente não afetados

- parser D1/D4 e código BLE;
- entidades/DAOs da série temporal, salvo leitura já existente;
- `WorkoutDerivationSnapshot` e `workflowState`;
- `HealthProjectionContracts`, `HealthRecordMapper`, coordinator e writer;
- permissões, manifests, IDs e payloads Health Connect.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Último sample cobrir até fim pode superestimar quando o sensor parou | Reutilizar ADR-018 sem timeout novo; exibir cobertura e documentar semântica. |
| `endedAt` divergir da duração canônica | Usar janela `[startedAt, startedAt + duration)` já usada pela derivação. |
| Atualização de altura/passada apagar FC máxima | Preservar campo em todo `UserProfile.create` e mapper/update. |
| FC máxima ser confundida com máxima observada | Nomes distintos, supporting text e validação específica. |
| Read model virar fato persistido | Manter policy e composição em memória, sem tabela/snapshot. |
| Carga parcial ser ocultada | Não aplicar threshold; mostrar `coverageRatio`. |
| UI exibir zero sintético | Estados tipados e `null`/indisponibilidade, sem listas vazias semânticas. |
| Alteração pessoal afetar Health Connect | Método de update sem coordinator; validação de operações antes/depois. |
| Escopo reabrir treino phone/GPS | Restringir análise a workouts watch com fatos locais persistidos. |
| Dados de saúde aparecerem em logs | Não registrar samples, BPM, FC máxima ou carga em logs diagnósticos. |

## Estratégia de testes e validação

Não criar testes automatizados novos nesta execução, conforme `AGENTS.md` e
pedido explícito. A implementação futura deve manter validação por inspeção,
build e validação física, usando cálculo independente para os gates numéricos.

### Prova técnica

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção do schema Room v20 e registro de `MIGRATION_19_20`;
- confirmação de que nenhum arquivo Health Connect ou parser foi alterado;
- revisão de que a policy recebe somente fatos locais e não persiste saída.

### Prova física obrigatória

Sobre banco real existente e instalação debug:

1. Confirmar upgrade v19 -> v20 sem wipe e `PRAGMA user_version = 20`.
2. Confirmar preservação de perfil, peso, atividades, snapshots, workflow e
   histórico de samples.
3. Configurar FC máxima, reiniciar o app e confirmar persistência.
4. Alterar FC máxima e confirmar recálculo visual em workout já existente.
5. Usar cálculo independente para confirmar zonas em todas as fronteiras,
   inclusive abaixo de 50%, exatamente em 50/60/70/80/90% e acima da máxima.
6. Confirmar `observedMaximumBpm` real e flag acima da máxima sem truncamento.
7. Confirmar que a soma das durações de zonas corresponde à cobertura e que os
   percentuais usam cobertura como denominador.
8. Confirmar Edwards TRIMP com cálculo independente e arredondamento somente na
   UI.
9. Confirmar atividade sem samples mostra indisponibilidade e não zero.
10. Confirmar perfil sem FC máxima solicita configuração e não exibe carga/zona
    sintética.
11. Confirmar workout existente mantém workflow e não exige novo treino.
12. Capturar screenshot do detalhe atualizado, sanitizada e sem dados pessoais
    desnecessários.
13. Comparar `health_sync_operations` antes/depois de migration, configuração,
    alteração e abertura do detalhe; não pode haver operação nova ou alterada.

Se cenário de interação física não puder ser reproduzido, registrar `N/A` com
motivo, evidência disponível e risco residual. `N/A` não libera critério
numérico obrigatório: sem prova independente de zona, cobertura ou carga, a
subtarefa de fechamento permanece bloqueada. Nunca fabricar resultado.

## Ordem das subtarefas

1. `034-01` — auditar timeline, perfil, UI e Room; congelar contrato.
2. `034-02` — implementar domínio puro de zonas, cobertura e carga.
3. `034-03` — persistir FC máxima pessoal e migrar v19 -> v20.
4. `034-04` — compor read model no detalhe a partir da atividade e perfil atual.
5. `034-05` — renderizar zonas, carga, cobertura e estados seguros.
6. `034-06` — validar banco real, workout existente, ausência de efeitos remotos
   e fechar documentação.

`034-02` e `034-03` podem ser implementadas em paralelo após `034-01`; `034-04`
depende de ambas. Nenhuma subtarefa deve alterar Health Connect, parser ou BLE.

## Premissas explícitas

- A entrega analisa somente atividades watch com série temporal local persistida;
  não amplia nem reativa workouts iniciados pelo celular.
- Todos os BPM persistidos que chegam ao domínio respeitam a validação existente
  `30..220`; amostras inválidas ou não reconstruíveis não são fabricadas como
  fatos válidos.
- A sessão canônica para cobertura é a janela usada pela derivação atual,
  `startedAt + duration`, conforme ADR-018 e o código de splits.
- `coveredDuration` mede janelas de sample-and-hold, não quantidade de samples e
  não uma estimativa por frequência nominal do sensor.
- `BELOW_ZONE_1` é bucket de cobertura e não uma das cinco zonas ponderadas,
  mas deve ser preservado na análise e na UI de detalhe.
- FC máxima atual é intencionalmente recalculável para históricos; nenhuma
  estabilidade histórica de zonas/carga é necessária nesta versão.
- A confirmação de que a branch de trabalho já apontava para a base de `main`
  substitui merge local; não há divergência de base a incorporar antes da
  documentação.
