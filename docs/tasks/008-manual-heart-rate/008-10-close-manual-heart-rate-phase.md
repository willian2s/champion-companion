# Task 008-10 - Fechar fase de frequencia cardiaca manual

## Status

Planejada.

## Objetivo

Consolidar conclusao da Fase 08 em spec, overview e baseline factual, registrando gate, smoke fisico, divida tecnica e riscos aceitos.

## Contexto

Apos validacao fisica, documentos precisam refletir estado real entregue sem alterar fases anteriores ou roadmap.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-09-run-manual-heart-rate-physical-validation.md`
- `docs/baseline/current-baseline.md`

## Dependencias

- `008-09`

## Entradas

- Resultado final dos comandos.
- Resultado do smoke fisico.
- Lista de riscos aceitos.
- Diffs finais da fase.

## Arquivos Exatos Permitidos

- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-10-close-manual-heart-rate-phase.md`
- `docs/baseline/current-baseline.md`

## Arquivos Proibidos

- Kotlin.
- Gradle.
- Manifesto.
- Roadmap.
- ADRs, salvo necessidade real explicita e separada.
- Specs/tasks concluidas de fases anteriores.

## Escopo

- Marcar fase concluida se aceite for atingido.
- Registrar comandos e smoke.
- Registrar divida tecnica adiada.
- Atualizar baseline factual.
- Confirmar que nao houve Health Connect nem persistencia de stream/sinal.

## Fora Do Escopo

- Correcoes funcionais novas.
- Refatoracao.
- Commit/push.

## Passos

1. Ler resultado de `008-09`.
2. Atualizar status da spec se necessario.
3. Consolidar checklist no overview.
4. Atualizar baseline factual.
5. Registrar riscos aceitos e testes adiados.
6. Registrar `git status`.

## Criterios De Aceite

- Overview final coerente.
- Baseline atualizada.
- Dividas adiadas registradas.
- Nenhuma fase anterior alterada indevidamente.

## Testes Pequenos Aplicaveis

- Nenhum. Task documental de fechamento.

## Validacao Minima

- Revisao de diff documental.
- `git status --short`.

## Evidencia Manual

- Comandos e smoke final consolidados.

## Atualizacao Do Overview

- Marcar `008-10` como concluida.
- Fechar checklist total.

## Validacao Final

- Confirmar que nao foi feito commit nem push automaticamente.
