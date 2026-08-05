# 014-16 — Encerrar documentalmente a Fase 014

- **Ticker:** `014`
- **Número:** `16`
- **Status:** pending

## Objetivo e resultado esperado

Reconciliar spec, ADR, overview, roadmap, baselines, protocolo e evidências após a
validação física, encerrando a Fase 014 somente quando o estado documental
corresponder ao comportamento realmente entregue.

Resultado: fase `completed` ou resultado parcial explicitamente justificado, sem
correções funcionais escondidas e sem reabrir a Fase 013.

## Dependências

- `014-01` a `014-15` concluídas e revisadas.
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

1. Conferir status e evidência das 16 subtarefas.
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

- Todas as 16 tasks possuem status e evidência coerentes.
- Spec distingue planejado histórico de comportamento entregue.
- ADR-018 permanece decisão durável e não afirma implementação inexistente.
- Roadmap e baselines refletem versões e capacidades reais.
- Protocolo diferencia código, dispositivo confirmado, desconhecido e derivado.
- Riscos residuais permanecem explícitos.
- Fase 013 continua `closed` / `partially_delivered`.
- `014-00-overview.md` muda para `16/16` somente após revisão final.

## Status final

Quando todos os critérios forem atendidos:

```text
Fase 014: completed
Progresso: 16/16
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
