# 037-01 — Auditar comparabilidade e congelar contrato

- **Ticker:** `037`
- **Número:** `01`
- **Status inicial:** `pending`

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

## Riscos e cuidados

- não transformar decisão cardiovascular da 035 em população da 037;
- não chamar `suggestedType` de confirmação;
- não tratar plausibilidade remota como automaticamente necessária para análise
  local;
- não declarar distribuição física RUNNING sem banco real sanitizado;
- não incluir activity ID, BPM individual ou dados pessoais em documentação.
