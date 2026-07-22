# Task 007-01 — Auditar Home, Resumo Diário E Fluxo FEA1

## Status

Pendente.

## Objetivo

Auditar estado real da Home, BLE, parser FEA1, repositories, DAOs, preferências e bindings Hilt antes de qualquer implementação funcional da Fase 07.

## Contexto

A Fase 06 fechou gerenciamento do dispositivo, mas Home ainda não consome resumo diário. Esta task reduz risco de implementar sync sobre premissas erradas.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/specs/_inputs/phase-07-daily-summary-home-direction.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`

## Dependências

- Nenhuma task anterior da Fase 07.

## Entradas

- Código atual em `feature/home`, `feature/device`, `core/bluetooth`, `core/model`, `core/database`, `core/preferences`, `data`.
- Spec 007.

## Arquivos Exatos Permitidos

- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-01-audit-home-daily-summary-and-fea1-flow.md`

## Arquivos Proibidos

- Qualquer `.kt`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `AndroidManifest.xml`
- `app/schemas/**`

## Escopo

- Confirmar gaps e riscos.
- Atualizar overview com achados se necessário.
- Não implementar código.

## Fora Do Escopo

- Parser novo.
- ViewModel.
- UI.
- Persistência.
- Testes.

## Passos

1. Ler docs obrigatórios.
2. Inspecionar arquivos citados na spec.
3. Confirmar que auto-connect não equivale a sync.
4. Confirmar estado de `DailySummaryRepository.save()` e riscos de redução.
5. Confirmar parser FEA1 atual.
6. Confirmar lacunas em prefs, profile metrics e Hilt.
7. Atualizar status da task e checklist do overview.

## Critérios De Aceite

- Achados registrados.
- Riscos de conflito confirmados.
- Nenhum código alterado.
- Overview atualizado.

## Validação Mínima

- Revisão documental.
- `git diff -- docs/specs/007-daily-summary-home.md docs/tasks/007-daily-summary-home/`.

## Evidência Manual

- Lista de arquivos lidos e achados no corpo da task ou overview.

## Atualização Do Overview

- Marcar `007-01` como concluída no checklist.
- Adicionar riscos descobertos, se houver.

## Validação Final

- `git status --short` para confirmar que só docs planejados mudaram.
