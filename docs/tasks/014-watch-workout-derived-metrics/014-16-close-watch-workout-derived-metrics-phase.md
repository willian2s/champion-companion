# 014-16 — Encerrar documentalmente a Fase 014

- **Ticker:** `014`
- **Número:** `16`
- **Status:** completed

## Objetivo e resultado esperado

Reconciliar spec, ADR, overview, roadmap, baselines, protocolo e evidências após a
validação física, encerrando a Fase 014 somente quando o estado documental
corresponder ao comportamento realmente entregue.

Resultado: fase `completed` ou resultado parcial explicitamente justificado, sem
correções funcionais escondidas e sem reabrir a Fase 013.

## Dependências

- `014-01` a `014-15` e `014-17` concluídas e revisadas.
- Nenhum gate bloqueador pendente.

## Arquivos permitidos

- `docs/specs/014-watch-workout-derived-metrics.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`;
- esta task;
- ADR-018, roadmap, baselines, protocolo e evidências da Fase 014;
- `AGENTS.md` somente para retirar linguagem de planejamento e registrar estado
  factual, quando necessário.

## Arquivos proibidos

- código de produção, Gradle, manifesto, migrations e schemas;
- correção funcional;
- alteração de dados Health Connect;
- reabertura ou reclassificação da Fase 013.

## Escopo

1. Conferir status e evidência das 17 subtarefas.
2. Atualizar comportamento factual entregue.
3. Registrar versões efetivas do parser, derivação e banco.
4. Registrar política temporal, plausibilidade e limitações finais.
5. Registrar comportamento final de `TREADMILL` importada do relógio.
6. Registrar projeções filhas, retry e reconciliação Health Connect.
7. Preservar riscos residuais e follow-ups fora do escopo.
8. Atualizar overview por último.

## Fora do escopo

- implementar ou corrigir parser, UI, banco ou Health Connect;
- remover legado da Fase 013;
- adicionar WorkManager, GPS, rota, mapa ou treino phone;
- declarar `PASS` sem evidência.

## Critérios de aceite

- Todas as 17 tasks possuem status e evidência coerentes.
- Spec distingue planejado histórico de comportamento entregue.
- ADR-018 permanece decisão durável e não afirma implementação inexistente.
- Roadmap e baselines refletem versões e capacidades reais.
- Protocolo diferencia código, dispositivo confirmado, desconhecido e derivado.
- Riscos residuais permanecem explícitos.
- Fase 013 continua `closed` / `partially_delivered`.
- `014-00-overview.md` muda para `17/17` somente após revisão final.

## Status final

Quando todos os critérios forem atendidos:

```text
Fase 014: completed
Progresso: 17/17
```

Se houver entrega parcial aceita, registrar outcome e motivo explicitamente, sem
marcar critérios ausentes como sucesso.

## Validação

```bash
git diff --check
git status --short
```

Não executar Gradle nem testes nesta task documental.

## Evidências

- matriz task → status → evidência;
- resumo de versões e migrations efetivas;
- riscos e follow-ups;
- revisão independente documental.

## Definição de pronto

Documentação factual reconciliada, item `014-16` marcado e status final da fase
registrado corretamente.

## Rollback

Reverter somente afirmações documentais incorretas. Não alterar código, banco ou
records remotos.

## Registro inicial de execução

### Escopo executado

Foi reconciliado o progresso factual da spec após a conclusão de `014-15`:
`16/17`. O status da fase permanece `in_progress` porque esta subtarefa ainda não
concluiu a revisão documental final nem marcou seu item no overview.

### Arquivos alterados

- `docs/specs/014-watch-workout-derived-metrics.md`;
- este arquivo;
- `014-00-overview.md`.

Nenhum arquivo de produção, Gradle, manifesto, migration ou schema foi alterado.

### Decisões e desvios

- A spec mantém status `in_progress`; `014-16` não foi marcada como concluída.
- O progresso da spec foi alinhado ao overview em `16/17`.
- A ordem documental registra `014-15` concluída e `014-16` pendente.
- Reconciliação de ADR, roadmap, baseline, protocolo e encerramento da fase fica
  para continuação desta subtarefa.

### Comandos executados e resultados

- `git diff --check` — passou, sem saída;
- `git status --short` — alterações restritas aos documentos da Fase 014.

Não foram executados testes automatizados nem Gradle; mudança é documental.

### Riscos residuais

- A spec, ADRs, roadmap, baseline e protocolo ainda precisam de revisão factual
  final antes do fechamento da fase.
- A cobertura física de midpoint, reconciliação delete-before-insert e identidade
  congelada permanece limitada conforme evidência de `014-15`.

## Registro final de execução

### Escopo executado

Foi reconciliado o conjunto completo de 17 subtarefas. `014-15` permanece
`completed` com Gates B, C, D, E e G em `N/A` justificado e Gate F aprovado
somente nos cenários físicos exercitados após `014-17`. `014-16` fecha a fase
documentalmente sem promover limitações físicas a sucesso universal.

### Matriz task → status → evidência

| Task | Status | Evidência principal |
| --- | --- | --- |
| `014-01` | `completed` | auditoria e registro da task |
| `014-02` | `completed` | contratos e registro da task |
| `014-03` | `completed` | migration `MIGRATION_8_9`, schema v9 e registro da task |
| `014-04` | `completed` | parser `d4-v2`, protocolo e registro da task |
| `014-05` | `completed` | duração efetiva, índices e registro da task |
| `014-06` | `completed` | perfil e registro físico da task |
| `014-07` | `completed` | snapshot e derivação canônica |
| `014-08` | `completed` | métricas derivadas e registro da task |
| `014-09` | `completed` | splits, BPM temporal e registro da task |
| `014-10` | `completed` | consumidores locais, UI e screenshots |
| `014-11` | `completed` | reprocessamento explícito e registro da task |
| `014-12` | `completed` | `MIGRATION_9_10`, schema v10 e registro da task |
| `014-13` | `completed` | writer granular e registro da task |
| `014-14` | `completed` | reconciliação e registro da task |
| `014-15` | `completed` | `docs/integration/014-15-physical-validation.md` |
| `014-17` | `completed` | follow-up do provider e revalidação física |
| `014-16` | `completed` | esta reconciliação documental |

### Arquivos alterados

- `docs/specs/014-watch-workout-derived-metrics.md`;
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- `docs/baseline/current-baseline.md`;
- `docs/baseline/database-reference.md`;
- `docs/baseline/gaps-and-open-questions.md`;
- `docs/baseline/feature-inventory.md`;
- `docs/baseline/health-connect-mapping.md`;
- `docs/protocol/workouts-d3-d4.md`;
- esta task e `014-00-overview.md`.

Nenhum arquivo de produção, Gradle, manifesto, migration, schema ou dado remoto
foi alterado. Migrations e schemas foram somente registrados como versões
efetivas já entregues.

### Decisões e desvios

- O planejamento possuía 17 arquivos de subtarefa após a inclusão do follow-up
  `014-17`; por isso, o progresso final correto é `17/17`, não `16/16`.
- Spec, ADR, roadmap, baselines e protocolo distinguem decisão durável,
  comportamento entregue, confirmação física, desconhecido e derivado.
- O Gate F foi registrado como PASS somente para autorização, falha parcial e
  retry exercitados. Gates sem fixture suficiente permanecem `N/A`.
- Fase 013 permanece `closed` / `partially_delivered` por
  `product_reorientation`; nenhum fluxo phone foi reaberto.

### Comandos executados e resultados

- `git diff --check` — PASS, sem erros de whitespace;
- `git status --short` — PASS, somente documentação da Fase 014 alterada;
- Não foram executados Gradle nem testes automatizados, conforme escopo
  documental da task.

### Resultados e evidências

- Spec, ADR-018, roadmap, baselines e protocolo registram parser `d4-v2`, Room
  v9, schema v10, `stepLengthMeters` individual congelado, política temporal,
  TREADMILL canônica e projeções filhas Health Connect.
- A evidência física preserva a tentativa inicial bloqueada como histórico, a
  resolução por `014-17`, o limite write-only e os cenários exercitados de
  provider, autorização, falha parcial e retry sem duplicação observada.
- Overview final registra `completed`, `17/17` e mantém um único checklist com
  17 itens.

### Riscos residuais

- Fixtures físicas de lacuna, duplicata, residual, plausibilidade, sugestão por
  altura, TREADMILL e reprocessamento legado recuperável não cobriram todos os
  gates.
- Midpoint, delete-before-insert, round-trip físico completo, identidade
  congelada em matriz completa e variações de firmware permanecem riscos de
  evidência.
- Provider observado manualmente não equivale a readback; o app continua
  write-only. Nenhum risco residual autoriza fallback de distância ou reabertura
  da Fase 013.

### Revisão independente solicitada

Solicita-se revisão independente documental desta subtarefa contra spec, ADR-018,
overview, matriz, evidências, roadmap, baselines e protocolo antes de considerar
o encerramento definitivo.
