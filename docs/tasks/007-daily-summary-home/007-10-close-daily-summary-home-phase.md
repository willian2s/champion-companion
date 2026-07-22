# Task 007-10 — Fechar Fase Home E Resumo Diário

## Status

Pendente.

## Objetivo

Consolidar conclusão da Fase 07 em spec, overview e baseline factual.

## Contexto

Após validação manual, documentos devem refletir estado real entregue e dívidas adiadas.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-09-run-manual-daily-summary-validation.md`
- `docs/baseline/current-baseline.md`

## Dependências

- `007-09`

## Entradas

- Resultado final dos comandos.
- Resultado do smoke manual.
- Lista de riscos aceitos.
- Diffs finais da fase.

## Arquivos Exatos Permitidos

- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-10-close-daily-summary-home-phase.md`
- `docs/baseline/current-baseline.md`

## Arquivos Proibidos

- Kotlin.
- Gradle.
- Manifesto.
- Roadmap.
- ADRs, salvo necessidade real explícita e separada.
- Specs/tasks concluídas de fases anteriores.

## Escopo

- Marcar fase concluída se aceite for atingido.
- Registrar comandos e smoke.
- Registrar dívida técnica adiada.
- Atualizar baseline factual.
- Confirmar que não houve Health Connect, D1, D3 ou D4.

## Fora Do Escopo

- Correções funcionais novas.
- Refatoração.
- Commit/push.

## Passos

1. Ler resultado de `007-09`.
2. Atualizar status da spec se necessário.
3. Consolidar checklist no overview.
4. Atualizar baseline factual.
5. Registrar riscos aceitos e testes adiados.
6. Registrar `git status`.

## Critérios De Aceite

- Overview final coerente.
- Baseline atualizada.
- Dívidas adiadas registradas.
- Nenhuma fase anterior alterada indevidamente.

## Validação Mínima

- Revisar diff documental.
- `git status --short`.

## Evidência Manual

- Comandos e smoke final consolidados.

## Atualização Do Overview

- Marcar `007-10` como concluída.
- Fechar checklist total.

## Validação Final

- Confirmar que não foi feito commit nem push automaticamente.
