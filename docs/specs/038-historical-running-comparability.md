# Spec 038 — Recuperação de comparabilidade histórica de corridas

- **Ticker:** `038`
- **Status:** `pending`
- **Branch de planejamento:** `feat/038-historical-running-comparability`
- **Base auditada:** `e2d141276ef30ee846d8aafba2c69ad425dd3e99` (`main` e `origin/main`)
- **ADR:** [ADR-038 — Recuperação de comparabilidade histórica](../decisions/adr-038-historical-running-comparability.md)
- **Overview:** [038-00-overview.md](../tasks/038-historical-running-comparability/038-00-overview.md)

## Contexto e objetivo

A validação física da Spec 037 encontrou, em janela de 56 dias, 21 corridas
confirmadas `RUNNING`, todas com snapshot ativo, escalares canônicos positivos e
temporalidade `ELIGIBLE`. Somente 5 eram `WATCH_NATIVE` ou
`WATCH_NATIVE_CONTEXT_CALIBRATED` com derivação `watch-d3-d4-v4`; 16 eram
`STEP_LENGTH_FALLBACK` e permaneceram fora dos gráficos comparáveis. A
evidência não contém composição por `parserVersion`, payload diagnóstico,
header ou estado de workflow dessas 16 corridas.

038 deve responder quanto desse fallback pode ser recuperado de forma
determinística usando fatos locais já preservados. Recuperação pode resultar em
nenhuma, algumas ou todas as corridas. O número de pontos da Spec 037 não é
meta de otimização.

Esta execução é somente de planejamento e documentação. Não implementa código,
não executa reprocessamento, não altera Room, schema, migration, Health Connect,
parser, BLE, banco físico ou dados, e não cria nem executa testes automatizados.

038 é spec independente pós-roadmap. Não reabre Fases 17–25.

## Escopo

### Incluído

- Auditoria da população `activityOrigin == WATCH_D3_D4` e
  `confirmedType == RUNNING` relevante à janela de 56 dias da Spec 037.
- Definição verificável de “recuperável para comparabilidade nativa”.
- Taxonomia de fonte, integridade, elegibilidade e bloqueio de mutação.
- Auditoria read-only de `d4-v1`, `d4-v2`, `d4-v3` e `d4-v4`.
- Investigação conservadora de diagnostic payload sem alterar
  `importRecord.parserVersion`.
- Política explícita para `SYNCING`, `SYNCED` e projeções Health Connect.
- Preservação de snapshots, facts source/measured, calibração e workflow.
- Gate A físico com agregados sanitizados e decisão `GO`, `PARTIAL-GO` ou
  `NO-GO`.
- Planejamento condicional de preflight, recuperação local, integração com
  Exercícios/037 e validação física.

### Excluído

- Tornar todo histórico nativo ou atingir percentual mínimo de recuperação.
- Conversão por edição de enum, distância “na mão” ou uso de
  `steps × stepLengthMeters` como distância nativa.
- Importação externa, nova busca no relógio, reimportação obrigatória ou
  criação de dados artificiais.
- Mudança de `parserVersion`, CRC, identidade de importação ou bytes físicos.
- Alteração de `RunningPerformanceHistoryPolicy` para incluir fallback.
- Leitura, republicação, delete, retry ou reconciliação no Health Connect.
- Alteração de `clientRecordId`, `clientRecordVersion` ou `frozenPayload`.
- Score, pace/BPM novo, volume, recordes, cadência ou Specs 039–041.
- `WALKING`, `TREADMILL`, `FREE_WORKOUT`, `CYCLING` e treinos phone.
- Nova coluna, tabela, índice, cache analítico ou migration Room.

## Requisitos e critérios de aceite

### Requisitos funcionais e de contrato

1. A classificação deve separar `NATIVO_ATUAL`,
   `V4_RECUPERAVEL_LOCALMENTE`,
   `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`,
   `LEGACY_SEM_FONTE_NATIVA`, `ESTRUTURALMENTE_INELEGIVEL` e
   `BLOQUEADO_POR_ESTADO_REMOTO`.
2. `BLOQUEADO_POR_ESTADO_REMOTO` só pode ser atribuído depois de provar que a
   fonte seria recuperável; ausência de fonte não pode ser mascarada por
   `SYNCED`.
3. “Recuperável para comparabilidade nativa” exige, sem escrita, fonte física
   suficiente, identidade única, semântica determinística, temporalidade
   elegível, distância nativa positiva e plausibilidade sem bloqueio.
4. A avaliação deve considerar, no mínimo, `parserVersion`, diagnostic payload,
   fatos nativos persistidos, header v4, consistência header/série, CRC,
   identidade de importação, fatos raw/measured, índices, ordem temporal,
   `confirmedType`, snapshot ativo, método/versão de derivação, plausibilidade,
   calibration evidence, `workflowState` e projeções Health Connect.
5. A interpretação legacy, se tecnicamente comprovada em uma execução futura,
   deve preservar o parser histórico e possuir provenance/version de
   interpretação separada. Trocar `d4-v1/v2/v3` por `d4-v4` é proibido.
6. Nenhuma classificação ou preflight escreve snapshot, intervalo, split,
   workflow, operação Health Connect ou payload.
7. Uma recuperação futura autorizada deve preservar snapshot histórico e fatos
   source/measured, materializar nova derivação somente em transação idempotente
   e não mudar workflow automaticamente.
8. A 037 deve continuar usando exatamente os critérios atuais:
   `WATCH_NATIVE` ou `WATCH_NATIVE_CONTEXT_CALIBRATED`,
   `watch-d3-d4-v4`, escalares positivos, temporalidade válida e ausência de
   indicador de plausibilidade bloqueador.
9. Health Connect permanece write-only, explícito e sem qualquer efeito causado
   por auditoria, preflight ou recuperação local.
10. Room permanece na versão 20, sem nova migration nesta spec. Se provenance
    nova não couber semanticamente no modelo atual, isso será blocker para
    implementação, não justificativa para antecipar schema.

### Definição objetiva de recuperável

Uma corrida pode ser considerada `V4_RECUPERAVEL_LOCALMENTE` somente se todos os
predicados abaixo fecharem:

- pertence à população da 037: origem `WATCH_D3_D4`, tipo confirmado `RUNNING`,
  não descartada e `startedAt` em `[asOf - 56 dias, asOf)`;
- para esta classe, `importRecord.parserVersion == d4-v4` e os facts nativos v4
  persistidos fecham; uma interpretação de bytes legacy só pode entrar nessa
  classe depois de decisão explícita de provenance distinta no Gate A;
- bytes diagnósticos completos, por si só, não satisfazem este predicado e
  mantêm `d4-v1/v2/v3` em `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`;
- a associação entre atividade, device, importação e bloco é única e fecha
  `deviceId`, CRC, `externalBlockIndex` e `startedAt`;
- header v4 existe quando a fonte exige header, é estruturalmente válido e sua
  `recordCount` coincide com a série;
- intervalos têm quantidade, índices, ordem e unicidade comprováveis;
- passos, BPM disponível, duração e facts raw/measured coincidem com a fonte
  preservada; divergência não é corrigida por heurística;
- CRC, identidade do bloco e header/série fecham, sem aceitar apenas total
  agregado como substituto de linha ausente;
- `WorkoutDurationResolver` e validação temporal atual resultam em
  `ELIGIBLE`, com janelas positivas e contidas na sessão;
- distância é soma de `nativeDistanceMeters` finitos, não negativos e
  positivos no agregado, quando a política exigir distância positiva; nunca é
  obtida de comprimento de passo, distância legacy agregada ou pace;
- `WatchWorkoutNativeDistancePolicy` é satisfeita em semântica, incluindo
  validade do parser v4 interpretado, consistência, CRC, totais nativos e
  `startedAt`/`endedAt`;
- não existem indicadores `IMPLAUSIBLE_CADENCE` ou `IMPLAUSIBLE_SPEED` que
  bloqueiem a promoção;
- a classificação de fonte é independente de frequência cardíaca ausente:
  falta de FC pode manter pace comparável, mas não pode ser preenchida;
- a mutação local, se futuramente autorizada, não conflita com workflow ou
  projeção remota insegura.

A presença de diagnostic payload, header isolado, distância original do relógio,
passos medidos ou snapshot fallback é evidência insuficiente por si só.

### Taxonomia e precedência

| Classe | Condição congelada | Resultado operacional |
|---|---|---|
| `NATIVO_ATUAL` | Snapshot ativo já atende contrato nativo da 037. | Nenhuma recuperação. Continua elegível conforme 037. |
| `V4_RECUPERAVEL_LOCALMENTE` | Fonte v4 e predicado completo fecham, mas snapshot ativo ainda não é comparável. | Pode ser candidato a ação local explícita somente após Gate A e política de mutação. |
| `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL` | `d4-v1/v2/v3` possui payload candidato, mas sem prova completa de semântica, identidade ou fechamento v4. | Não comparável; requer decisão/prova adicional. |
| `LEGACY_SEM_FONTE_NATIVA` | Legacy sem bytes/fatos suficientes para reconstruir distância nativa. | Permanece fallback, sem erro. |
| `ESTRUTURALMENTE_INELEGIVEL` | Fonte examinada falha header, CRC, série, índices, ordem, duração, fatos nativos, temporalidade ou plausibilidade. | Não recuperar; motivo deve ser sanitizado e determinístico. |
| `BLOQUEADO_POR_ESTADO_REMOTO` | Predicado de recuperação fecha, mas `SYNCED`, `SYNCING` ou projection child inseguro impede mutação canônica. | Não mutar. Registrar fonte recuperável e bloqueio separadamente. |

Workflow incompatível com a ação atual deve aparecer como motivo de preflight
separado, sem ser promovido artificialmente a falha estrutural. A taxonomia
deve sempre preservar `sourceAssessment` e `mutationDisposition` para não
confundir “não há fonte” com “há fonte, mas não é permitido mutar”.

## Comportamento atual encontrado

- `WorkoutActivityRepository.reprocessWatchWorkout(...)` delega a
  `RoomRepositories.reprocessWatchWorkoutInternal(...)`.
- O reprocessamento atual bloqueia `SYNCING` com
  `SYNC_IN_PROGRESS`, `SYNCED` com
  `SYNCED_REQUIRES_FUTURE_PROJECTION` e qualquer projection child inseguro.
- Estados aceitos atualmente são `PENDING_CALIBRATION`, `READY_TO_SYNC` e
  `SYNC_FAILED`, além dos guards de origem, importação, fatos e consistência.
- A operação é transacional: pode recuperar duração autoritativa usando
  diagnostic payload, recalcular temporalidade e materializar nova derivação;
  falha deve desfazer alterações parciais.
- Parser versions reconhecidas são `d4-v1`, `d4-v2`, `d4-v3` e `d4-v4`.
- Para `d4-v1/v2/v3`, o fluxo atual pode recuperar fatos de passos e derivar
  por comprimento de passo, mas não promove automaticamente distância nativa.
- Para `d4-v4`, a derivação nativa exige facts nativos por intervalo, header,
  consistência, CRC, índices, temporalidade e totais fechados.
- `WatchWorkoutNativeDistancePolicy.isEligible(...)` é a autoridade de
  elegibilidade nativa. Somente ela permite `WATCH_NATIVE` ou
  `WATCH_NATIVE_CONTEXT_CALIBRATED`.
- `requiresWatchWorkoutReprocessing` sinaliza somente workflows locais
  específicos; não autoriza reprocessamento de `SYNCED`.
- A ação de Exercícios é individual, explícita e separada de sync. O caminho de
  reprocessamento atual não chama `HealthConnectSyncCoordinator`.
- A 037 observa atividades/snapshots e samples em leitura bounded. Um snapshot
  novo, se futuramente materializado e ativo, tende a ser incorporado
  automaticamente pelo read model; a policy da 037 não deve ser relaxada.
- O código atual declara `ChampionDatabase` na versão 20; a evidência física
  037 confirma `user_version = 20`. Trechos históricos da baseline que citam
  Room 19 não alteram esse fato atual.

Referências auditadas: `WatchWorkoutActivity.kt`,
`WatchWorkoutDerivedMetrics.kt`, `RepositoryContracts.kt`, `RoomRepositories.kt`,
`DatabaseMappers.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`,
`ExercisesViewModel.kt`, `ExercisesScreen.kt`, `RunningPerformanceHistory.kt`,
`RunningPerformanceHistoryScreen.kt`, `HealthConnectSyncCoordinator.kt`,
`HealthRecordMapper.kt` e `HealthProjectionContracts.kt`.

## Abordagem escolhida

038 começa por auditoria física e contrato, não por código de recuperação.
`038-01` deve executar Gate A em banco físico real, read-only, se dump/ambiente
estiver disponível. Nesta criação documental, a composição adicional não foi
inventada: somente 21/5/16 observados em 037 são conhecidos.

O Gate A deve produzir somente agregados sanitizados:

- quantidade por `parserVersion`;
- quantidade por `derivationMethod`/`derivationVersion`;
- quantidade por `workflowState`;
- quantidade com/sem diagnostic payload;
- quantidade com/sem header v4;
- quantidade com facts nativos completos;
- quantidade potencialmente recuperável;
- quantidade definitivamente fallback;
- quantidade bloqueada somente por estado remoto;
- quantidade estruturalmente inelegível;
- quantidade em cada classe da taxonomia e motivos agregados.

Não registrar activity ID, MAC, CRC individual, payload, bytes, BPM individual,
timestamp completo, título, nota ou localização. Ausência natural deve ser
`N/A`, não cenário fabricado.

### Decisão para diagnostic payload legacy

O payload pode ser candidato a fonte física original para `d4-v1/v2/v3`, mas
não é prova automática de v4. Uma interpretação só pode avançar se fechar
identidade do device/import, CRC, `externalBlockIndex`, `startedAt`, quantidade
de registros, índices, passos, BPM compatível, duração/header e comparação com
facts persistidos, sem alterar bytes.

A política escolhida é preservar `importRecord.parserVersion` como fato
histórico e, se o Gate algum dia autorizar interpretação legacy, registrá-la
como provenance/version de interpretação distinta da versão do parser. O
nome, campo ou representação final dessa provenance não é inventado nesta
spec. Se as colunas e contratos atuais não conseguirem representá-la sem
ambiguidade, a implementação fica bloqueada e exige nova decisão/migration;
Room não será incrementado dentro de 038 por antecipação.

Até prova integral, legacy com payload fica
`LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`, não entra nos gráficos e
não recebe snapshot nativo.

### Decisão para atividades `SYNCED`

Adota-se a alternativa A: continuar bloqueando recuperação canônica de
`SYNCED` e de atividades com projection child inseguro. A alternativa B,
derivação local divergente do payload remoto, é rejeitada porque Health Connect
é write-only e os IDs, versões e payloads aceitos ficam congelados. A alternativa
C, leitura efêmera user-facing ou segunda pipeline para gráfico, também é
rejeitada nesta spec por duplicar a verdade e poder depender da retenção do
diagnostic payload.

Uma leitura em memória no próprio Gate A é permitida somente para provar
invariantes; não é snapshot, não substitui derivação ativa e não alimenta 037.

### Decisão para calibração

Recuperar facts nativos é separado de aplicar calibração contextual. A direção
conservadora é materializar `WATCH_NATIVE` quando a fonte nativa fechar. Não
aplicar perfil atual nem aprender perfil durante recuperação. Um futuro caminho
só pode produzir `WATCH_NATIVE_CONTEXT_CALIBRATED` se existir evidence válida,
já associada e autorizada pelo contrato da atividade; não pode reescrever
evidence antiga nem recalibrar outra atividade.

### Decisão para snapshots

Snapshot anterior permanece histórico. Recuperação futura, se autorizada,
cria snapshot novo append-only, com derived intervals e splits pertencentes ao
novo snapshot, e só o torna ativo dentro de transação explícita e idempotente.
Facts source/measured, `parserVersion`, import identity, workflow e operations
Health Connect não são editados para produzir aparência nativa. Nunca converter
`STEP_LENGTH_FALLBACK` para `WATCH_NATIVE` apenas trocando enum/version.

## Alternativas descartadas

- Promover todas as 16 corridas para aumentar gráficos: mistura provenance e
  contradiz a 037.
- Trocar `d4-v1/v2/v3` por `d4-v4`: falsifica lineage histórica.
- Usar header isolado, CRC, total agregado, distância original ou
  `steps × stepLengthMeters`: não prova série nativa completa.
- Permitir derivação canônica em `SYNCED`: cria divergência local/remota sem
  readback e sem contrato de republicação.
- Criar leitura efêmera como tendência: cria segunda verdade e reintroduz risco
  de reidratação histórica bounded/N+1.
- Fazer backfill na abertura, batch implícito ou reimportação para elevar
  cobertura: viola ação explícita e escopo.
- Criar coluna `comparable`, cache ou migration antes de provar necessidade:
  persistência analítica não é requisito e aumenta custo de rollback.
- Usar Health Connect como fonte de confirmação: leitura/remota/readback não
  faz parte do contrato vigente.

## Arquivos, módulos e contratos afetados

### Auditoria e documentação

- `docs/specs/014-watch-workout-derived-metrics.md` e task `014-11`;
  `docs/specs/021-*`; ADR-018, ADR-019, ADR-022, ADR-029 e ADR-037.
- Specs 023, 032 e 037, suas tasks relevantes e evidências físicas 032, 037.
- `docs/baseline/current-baseline.md`,
  `docs/roadmap/post-phase-15-direction.md` e roadmap pós-037 existente.

### Código a auditar ou potencialmente reutilizar somente após Gate

- Domínio: `WatchWorkoutActivity.kt`, `WatchWorkoutDerivedMetrics.kt`,
  `RepositoryContracts.kt`, `RunningPerformanceHistory.kt`.
- Persistência: `RoomRepositories.kt`, `DatabaseMappers.kt`, `ChampionDatabase.kt`,
  `ChampionDaos.kt` e entidades/tabelas de activities, imports, intervals,
  headers, snapshots, derived intervals, splits, samples, calibration evidence
  e `health_sync_operations`.
- Exercícios/037: `ExercisesViewModel.kt`, `ExercisesScreen.kt`,
  `RunningPerformanceHistoryScreen.kt`.
- Health Connect: `HealthConnectSyncCoordinator.kt`, `HealthRecordMapper.kt`,
  `HealthProjectionContracts.kt`.

Nenhum desses arquivos de código é alterado pelo planejamento. O eventual
contrato de preflight deve reutilizar queries bounded e evitar hidratação
completa/N+1. Uma nova provenance que não caiba semanticamente no modelo atual
é dependência de decisão futura, não alteração implícita.

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Payload diagnóstico ausente, truncado ou com semântica legacy incerta | Classificar como potencial ou sem fonte; exigir fechamento integral; não inferir. |
| Header fecha, mas série ou índices não fecham | `ESTRUTURALMENTE_INELEGIVEL`; header nunca preenche linha ausente. |
| Parser histórico confundido com interpretação v4 | Preservar `parserVersion`; exigir provenance distinta ou bloquear. |
| `SYNCED` local divergir do Health Connect | Guard canônico fail-closed; nenhuma derivação divergente user-facing. |
| Fator contextual atual alterar histórico | Recuperar nativo separadamente; não aprender nem recalibrar em cascata. |
| Nova derivação apagar histórico | Snapshot append-only, transação e validação before/after. |
| 037 incluir fallback por regressão | Manter policy e critérios exatos; validar população/método/version. |
| Gate físico não disponível | Registrar procedimento e estado inconclusivo; não inventar breakdown. |
| Query histórica gerar N+1 ou reidratação ampla | Preflight read-only bounded, batch e seleção mínima de fatos. |
| Evidência expor dados sensíveis | Somente agregados sanitizados; nenhum payload, ID, CRC individual ou BPM. |

## Estratégia de testes e validação

Esta execução não cria nem executa testes automatizados. A estratégia de
planejamento é documental e física:

1. Revisar contratos, código e ADR-038 independentemente.
2. Executar `git diff --check` somente sobre os artefatos documentais criados.
3. Quando houver banco físico autorizado, executar Gate A read-only, sem wipe,
   sem reimportação, sem reprocessamento e sem criação de dados.
4. Em implementação futura autorizada, validar por atividade antes/depois
   activities, imports, intervals, headers, snapshots, derived intervals,
   splits, samples, calibration evidence, workflow e operations HC.
5. Recalcular comparabilidade 037 independentemente a partir dos escalares do
   snapshot ativo, sem usar splits/pace agregado como fonte.
6. Em validação física futura, usar release incremental e registrar N/A para
   cenários que não existirem naturalmente. Build só será exigido se código ou
   Gradle mudarem numa execução posterior; não faz parte desta execução.

## Gate A e decisão GO / PARTIAL-GO / NO-GO

Gate A é decisão real, não métrica de cobertura.

- **GO:** existe ao menos uma classe de candidatos com fonte nativa
  deterministicamente comprovada e estratégia local compatível com Room v20,
  snapshots e Health Connect. O GO autoriza somente a implementação da classe
  aprovada, não todos os fallbacks.
- **PARTIAL-GO:** subconjunto comprovado existe, mas legacy, estados remotos,
  inconsistências ou provenance sem representação segura permanecem fora. O
  próximo trabalho implementa apenas o subconjunto explicitamente autorizado.
- **NO-GO:** nenhuma corrida pode ganhar comparabilidade nativa sem inventar
  facts, falsificar parser/provenance ou quebrar o contrato remoto. Preservar
  fallbacks e fechar documentalmente, sem feature artificial.
- **INCONCLUSIVO:** não há banco físico/dump read-only ou os fatos necessários
  não estão acessíveis. Não equivale a GO; congela procedimento e aguarda uma
  execução com evidência real.

O estado desta criação documental é `INCONCLUSIVO`: a evidência 037 comprova
21/5/16, mas não a composição sanitizada necessária para classificar cada
fallback. Nenhuma subtarefa posterior fica autorizada antes de uma decisão
explícita do Gate A.

## Ordem das subtarefas

1. `038-01` — auditar população, recuperabilidade, diagnostic payload,
   workflow, snapshots, calibração, Room, Health Connect e congelar Gate A.
2. `038-02` — implementar preflight somente para classe autorizada por Gate A;
   em `NO-GO`/`INCONCLUSIVO`, fechar como não aplicável sem código.
3. `038-03` — materializar recuperação local explícita somente para candidatos
   autorizados e não bloqueados; em `NO-GO`, não implementar.
4. `038-04` — integrar ação existente de Exercícios e confirmar incorporação
   automática pela 037; em `NO-GO`, documentar N/A.
5. `038-05` — validar preservação, idempotência e ganho somente por fonte
   válida; não criar teste automatizado por padrão.
6. `038-06` — validação física release incremental e fechamento documental;
   cenários ausentes permanecem N/A.

## Premissas explícitas

- `main` e `origin/main` estavam em
  `e2d141276ef30ee846d8aafba2c69ad425dd3e99` antes da criação da branch; ticker
  038 estava livre e não havia branch, spec, ADR ou task 038.
- O arquivo não rastreado `docs/roadmap/roadmap-pos-037.md` já existia e foi
  preservado sem alteração.
- Room v20 e Health Connect inalterado são alvos obrigatórios, não sugestões.
- Diagnostic payload é fonte potencial limitada; retenção, completude e
  semântica devem ser demonstradas, nunca presumidas.
- Falta de BPM não invalida pace nativo, mas BPM ausente não pode ser inventado.
- `SYNCED` não será reaberto, resetado ou republicado automaticamente.
- Histórico fallback pode permanecer fallback legitimamente.
- Nenhuma decisão desta spec autoriza GPS, BLE live, treino phone ou fases
  futuras.
