# Task 011-01 - Auditar domínio, persistência e fronteiras da revisão

## Status

Planejada em 2026-07-24.

## Objetivo

Auditar estado real do domínio, Room, contracts, queries, reimportação, calibração, filtros, Home, navegação, MotionLog e fronteira entre Fase 11 e Fase 12 antes de qualquer implementação funcional.

## Contexto

O app já importa D3/D4 até `PENDING_REVIEW`, mas ainda não possui `title`, `notes`, filtros, detalhe, banner de Home nem workflow de revisão/calibração pronto. O roadmap também ainda mistura responsabilidades de Health Connect na Fase 11.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-11-activity-review-calibration-direction.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Dependências

- nenhuma.

## Entradas

- código real em `feature/exercises`, `feature/home`, `core/navigation`, `core/model`, `core/database` e `data`;
- schemas Room atuais;
- baseline documental MotionLog;
- fronteira desejada `READY_TO_SYNC`.

## Arquivos exatos permitidos

- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-01-audit-review-domain-persistence-and-phase-boundaries.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/roadmap/**`
- `docs/decisions/**`
- fases anteriores concluídas.

## Escopo

- auditar domínio atual;
- mapear campos ausentes;
- mapear contracts e queries atuais;
- mapear schema Room atual;
- confirmar plano atual de reimportação e preservação de revisão;
- auditar calibração atual;
- auditar filtros ausentes;
- auditar estado atual da Home e da navegação;
- registrar fronteira F11/F12;
- registrar hotspots e riscos.

## Fora do escopo

- implementar código funcional;
- criar ADR;
- alterar roadmap;
- alterar Kotlin, Compose, Room, Gradle ou Manifest.

## Passos

1. Auditar classes e DAOs reais envolvidos na atividade importada.
2. Confirmar campos presentes e ausentes no domínio e no schema Room.
3. Mapear repositórios, queries, reimportação e preservação de decisões.
4. Confirmar estado atual da Home, Exercícios e navegação.
5. Registrar explicitamente a fronteira entre revisão local e sincronização futura.
6. Atualizar spec e overview com achados factuais.

## Critérios de aceite

- auditoria cobre domínio, Room, contracts, queries, Home, navegação, filtros, reimportação, calibração e MotionLog;
- estado real do código fica documentado sem hipótese nova;
- hotspots e riscos de conflito ficam explícitos;
- nenhuma implementação funcional é introduzida.

## Validação manual

- conferência textual das referências citadas;
- revisão do diff documental apenas.

## Comandos de build aplicáveis

- nenhum nesta task documental.

## Evidências

- lista de arquivos auditados;
- lista de lacunas confirmadas;
- diff restrito à documentação da Fase 11.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-01` como concluída;
- registrar hotspots, dependências e riscos confirmados.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
