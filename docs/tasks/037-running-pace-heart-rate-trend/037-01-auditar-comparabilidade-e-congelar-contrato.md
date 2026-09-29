# 037-01 — Auditar comparabilidade e congelar contrato

- **Ticker:** `037`
- **Número:** `01`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Concluir auditoria documental e de código da tendência de corrida, congelando
população, janela, fórmula, elegibilidade, provenance, FC, estados, read model,
UI, gráficos e limites antes de qualquer implementação funcional.

Resultado: Spec 037 e ADR-037 coerentes, com todas as 22 decisões solicitadas
respondidas e sem alteração de código, Room, Health Connect ou testes.

## Requisitos cobertos

- população confirmada e exclusão de `suggestedType`;
- comparabilidade das quatro proveniências;
- pace por escalares canônicos;
- sample-and-hold, média ponderada e coverage;
- falhas por linha versus fonte;
- read model bounded e anti-N+1;
- estados, UI, gráficos e labels;
- Room v20, Health Connect fora do escopo e validação física.

## Escopo incluído

- ler Specs/ADRs 032, 034, 035, 036 e 035-08;
- ler contratos, DAO, mappers, repository, Exercícios, navegação e charts;
- auditar evidências sanitizadas disponíveis;
- registrar ausência de banco físico local, se mantida;
- atualizar somente documentação da 037, se surgirem fatos novos seguros.

## Escopo excluído

- código de produção;
- migration, entidade, DAO funcional ou schema;
- mudança de Health Connect, derivação, calibração ou workflow;
- testes automatizados, fixtures ou fabricação de dados;
- UI funcional ou alteração no design system.

## Dependências

- `main` em `201a7b8c80d176c2f726176fa9107a7326a62f22`;
- Specs 032, 034, 035 e 036 concluídas;
- correção `035-08` concluída;
- ADR-029, ADR-034, ADR-035 e ADR-036.

## Arquivos e símbolos prováveis

- `docs/specs/037-running-pace-heart-rate-trend.md`;
- `docs/decisions/adr-037-running-pace-heart-rate-trend.md`;
- `WatchWorkoutActivity`, `WorkoutDerivationSnapshot`;
- `WorkoutHeartRateAnalysisPolicy`, `sampleAndHoldWindows`;
- `CardiovascularLoadWorkoutInput` e `observeCardiovascularLoadWorkoutsBetween`;
- `ChampionDaos`, `RoomRepositories`, `DatabaseMappers`;
- `ExercisesViewModel`, `ExercisesScreen`, `ChampionNavigation`;
- `ChampionChartContracts` e `ChampionCharts`.

## Passos de implementação/validação

1. Confirmar branch, HEAD, ticker livre e working tree.
2. Conferir cada população, campo, contrato e limitação contra código real.
3. Confirmar que trend principal usa somente lineage nativa válida e que
   fallback permanece apenas listável.
4. Confirmar média FC ponderada pela timeline compartilhada, sem threshold.
5. Confirmar projeção mínima, estados e estratégia ordinal do gráfico.
6. Conferir links, nomes, números e coerência entre spec, ADR e overview.

## Testes e comandos de validação

Não criar nem executar testes automatizados. Executar somente:

```bash
git diff --check
```

## Definição de pronto

- todas as decisões da 037-01 estão explícitas;
- nenhuma incerteza material está escondida;
- premissas físicas sem evidência estão marcadas como `N/A` futuro;
- spec, ADR e overview apontam para os mesmos nomes e escopos;
- nenhum código funcional foi alterado.

## Decisões auditadas

1. População: somente `WATCH_D3_D4`, `confirmedType == RUNNING`, não
   descartada e `startedAt` em `[asOf - 56 dias, asOf)`.
2. `suggestedType` não confirma corrida e não participa sozinho.
3. `TREADMILL`, `WALKING`, `FREE_WORKOUT`, `CYCLING`, `UNKNOWN` e origens phone
   ficam fora.
4. Janela única rolling de 56 dias; sem current28/previous28, delta ou score.
5. `asOf` e `ZoneId` entram explicitamente; domínio não chama `Instant.now()`.
6. Pace usa somente `canonicalDistanceMeters` e
   `canonicalCoveredDurationNanos` do snapshot ativo.
7. Fórmula interna é segundos por quilômetro, sem arredondamento prévio e sem
   `metrics.averagePaceSecondsPerKilometer`.
8. Snapshot ativo é selecionado por `activeActivityId` e `isActive = 1`; ausência
   ou fato inválido permanece estado tipado por linha.
9. Pace comparável exige duração coberta positiva, dentro da duração da
   atividade, temporalidade elegível, sem indicador bloqueador e versão nativa
   `watch-d3-d4-v4`.
10. `WATCH_NATIVE` e `WATCH_NATIVE_CONTEXT_CALIBRATED` compartilham a série
    principal, com provenance visível.
11. Fallback por comprimento de passo pode ser listado, mas fica fora da série
    comparável e recebe estado explícito.
12. Versão nativa divergente pode ser listada factualmente, mas fica fora dos
    gráficos e recebe estado não comparável.
13. Ausência de indicador de plausibilidade não exige aceite remoto; indicadores
    `IMPLAUSIBLE_CADENCE` ou `IMPLAUSIBLE_SPEED` bloqueiam comparabilidade.
14. FC vem de `WorkoutHeartRateSample`, não de agregado importado ou FC máxima.
15. Timeline usa filtro canônico, validação e sample-and-hold compartilhados com
    034; média é ponderada pela duração coberta.
16. Cobertura parcial permanece válida e sem threshold; ausência de samples deixa
    média e coverage ausentes, sem zero.
17. Falha de linha preserva outras linhas; falha batch da fonte marca fonte
    indisponível e não apresenta histórico parcial como disponível.
18. Estados top-level distinguem `AVAILABLE`, `NO_RUNS`,
    `NO_COMPARABLE_RUNS` e `UNAVAILABLE`; Compose não decide precedência.
19. Pontos carregam fatos mínimos, provenance, coverage, statuses e
    `partial`; `activityId` fica somente no record interno.
20. Query dedicada usa `LEFT JOIN` do snapshot e segunda leitura batch de
    samples, sem N+1, Flow por atividade, DAO no ViewModel ou snapshot completo.
21. UI será contextual em Exercícios, com lista factual e dois gráficos
    independentes: ritmo `s/km` e FC `bpm`; FC ausente usa `y = null`.
22. Room permanece v20, Health Connect fica fora do escopo e validação física
    usa banco real existente, cálculo independente e `N/A` sem fabricar fatos.

## Evidências da auditoria

- `feat/037-running-pace-heart-rate-trend` estava limpa no início, em
  `6c0254795c4d4c4575372f4ea332c0e78754a724`; `main` e `origin/main` apontavam
  para `201a7b8c80d176c2f726176fa9107a7326a62f22`. O HEAD contém somente o
  planejamento documental inicial da 037 sobre essa base.
- Spec, ADR, overview e seis subtarefas usam ticker `037`; overview possui uma
  única seção `## Checklist` com exatamente seis itens, um por subtarefa.
- `WorkoutActivity`, `WorkoutDerivationSnapshotEntity`,
  `WorkoutHeartRateAnalysisPolicy`, DAO, mapper, repository, Exercícios,
  navegação e contratos de gráfico foram conferidos nos caminhos listados na
  subtarefa e na Spec. O código confirma que as consultas watch atuais ainda não
  filtram `confirmedType`, o estado de tendência ainda não existe e o gráfico
  exige X estritamente crescente.
- Room efetivo é v20 (`ChampionDatabase.kt:955`); não há `champion-release-db/`
  nem arquivo `*.db` no checkout. As evidências físicas históricas são
  sanitizadas e não substituem inspeção física da composição 037.

## Arquivos alterados

- `docs/specs/037-running-pace-heart-rate-trend.md` — precisões de nomenclatura,
  mapeamento persistência/domínio, timeline e ausência de banco local.
- `docs/decisions/adr-037-running-pace-heart-rate-trend.md` — alinhamento da
  projection com campos reais e registro da auditoria.
- `docs/tasks/037-running-pace-heart-rate-trend/037-00-overview.md` — checklist,
  progresso e observações.
- este arquivo — status, decisões, evidências e riscos da auditoria.

Nenhum arquivo de produção, Room, schema, Health Connect, teste, fixture ou UI
funcional foi alterado.

## Decisões e desvios

- Decisão: manter query/read model dedicado; não ampliar
  `CardiovascularLoadWorkoutInput` nem reutilizar query watch sem filtro de tipo
  confirmado.
- Decisão: tratar `WorkoutDerivationSnapshotEntity` como fonte de projection
  mínima e mapear explicitamente diferenças para o domínio.
- Decisão: preservar `sampleAndHoldWindows`/`WorkoutHeartRateAnalysisPolicy`
  como autoridade temporal única, extraindo somente behavior-preserving na task
  037-02.
- Desvio: HEAD atual é o commit documental da 037, posterior à base declarada;
  auditoria usou o checkout atual e confirmou `main`/`origin/main` na base
  declarada. Nenhum conflito de código foi encontrado.
- Desvio: não há banco físico local sanitizado; composição física específica,
  distribuição de proveniências e cenários naturais permanecem `N/A` futuro.
- Build, lint e testes automatizados não foram executados: subtarefa é
  documental e prescreve somente `git diff --check`.

## Comandos executados

- `git status --short --branch`
- `git rev-parse HEAD`
- `git log --oneline --decorate -10`
- buscas e leituras dirigidas de `AGENTS.md`, baseline, roadmap, Specs/ADRs
  032/034/035/036, task 035-08, evidências 032/034/035/036 e código de domínio,
  DAO, entidade, mapper, repository, Exercícios, navegação e gráficos
- inspeção de ausência de `champion-release-db/` e arquivos `*.db`
- `git diff --check`

## Resultados e evidências

- Auditoria documental concluída; 22 decisões respondidas e coerentes entre
  Spec 037, ADR-037, overview e subtarefas.
- População, fórmula de pace, provenance, elegibilidade, timeline FC, coverage,
  estados, bounded read model, UI, gráficos, Room v20, Health Connect e
  validação física futura estão explicitamente congelados.
- Nenhuma alteração funcional foi feita. `git diff --check` passou.

## Revisão independente

- Agente `review` aprovou estrutura SDD, ticker, checklist, status/progresso,
  cobertura das 22 decisões, coerência entre Spec/ADR/overview/tarefa,
  evidências contra o código e ausência de alterações funcionais.
- Riscos mantidos: query dedicada, validação física/banco real e screenshot
  pertencem às subtarefas posteriores.

## Riscos residuais

- Query dedicada ainda precisa ser implementada com filtro SQL de
  `confirmedType == RUNNING`, `LEFT JOIN` ativo e projeção mínima.
- Extração da timeline pode afetar splits se não for behavior-preserving; validar
  contra a saída 034 na 037-02.
- Não há índice temporal composto em `workout_activities`; custo deve ser medido
  antes de qualquer proposta de schema.
- Não há banco físico local da 037; composição de corridas, mistura de métodos,
  cenários de ausência/parcialidade e empate temporal dependem de 037-06.
- Risco histórico de Release da 035-08 permanece: não reidratar snapshots,
  derived intervals ou splits na leitura bounded.
- Evidências físicas anteriores registram ruído de inicialização não isolável;
  037-06 não deve declarar ausência absoluta de mutações sem snapshot causal.

## Riscos e cuidados

- não transformar decisão cardiovascular da 035 em população da 037;
- não chamar `suggestedType` de confirmação;
- não tratar plausibilidade remota como automaticamente necessária para análise
  local;
- não declarar distribuição física RUNNING sem banco real sanitizado;
- não incluir activity ID, BPM individual ou dados pessoais em documentação.
