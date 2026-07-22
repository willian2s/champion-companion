# Task 006-01 — Auditar feature de dispositivo e estratégia de associação

## Status

Pendente.

## Objetivo

Auditar estado atual de `feature/device`, BLE core, domínio, Room e data layer; confirmar estratégia mínima e sustentável para associação de um Champion C001.

## Contexto

Fase 05 deixou PoC debug funcional, mas tela release segue placeholder e `DeviceRepository` só observa associado. `DeviceEntity` V1 contém `associatedAtEpochMillis`, mas esquecer dispositivo sem apagar histórico exige separar associação atual de identidade histórica.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/_inputs/phase-06-device-management-direction.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/tasks/003-domain-and-persistence-foundation/003-00-overview.md`
- `docs/decisions/adr-003-local-persistence-architecture.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`

## Dependências

- Nenhuma task anterior da Fase 06.

## Entradas

- Código atual em `feature/device`, `core/bluetooth`, `core/model`, `core/database` e `data`.
- Schema Room V1 exportado.
- Falhas automatizadas conhecidas herdadas da Fase 05.

## Arquivos Exatos Permitidos

- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`

## Arquivos Proibidos

- Qualquer arquivo `.kt`.
- Qualquer arquivo Gradle.
- `docs/core/current-project-baseline.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/**`
- Specs/tasks de fases concluídas.
- `docs/specs/_inputs/phase-06-device-management-direction.md`

## Escopo

- Confirmar gaps para associação, esquecimento, última conexão e última sincronização.
- Documentar decisão recomendada: entidade própria de associação.
- Registrar necessidade de migration e possível ADR.
- Ajustar spec/overview apenas se auditoria encontrar divergência concreta.

## Fora Do Escopo

- Implementar Kotlin.
- Criar migration.
- Rodar BLE, scan ou conexão.
- Criar ADR.
- Alterar baseline.

## Passos

1. Ler documentos obrigatórios.
2. Inspecionar `DeviceEntity`, `DeviceDao`, `DeviceRepository`, mappers e `RoomRepositories`.
3. Comparar alternativas: evolução da entidade, ponteiro DataStore, entidade de associação.
4. Confirmar impacto em histórico futuro e comportamento ao esquecer.
5. Atualizar esta task com achados e decisão final se necessário.
6. Atualizar checklist do overview.

## Critérios De Aceite

- Estratégia de associação documentada com justificativa.
- Impacto Room e migration documentados.
- Comportamento ao esquecer documentado.
- Relação com dados históricos documentada.
- Possível ADR identificado.

## Validação Mínima

- Leitura e inspeção estática somente.
- `git diff -- docs/specs/006-device-management.md docs/tasks/006-device-management/006-00-overview.md docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`.

## Evidência Manual Esperada

- Lista de arquivos inspecionados.
- Resumo da alternativa escolhida.
- Confirmação de que nenhum código foi alterado.

## Atualização Do Overview

- Marcar `006-01` como concluída no checklist.
- Registrar qualquer risco novo em “Arquivos Com Risco De Conflito” ou “Dívida Técnica Adiada”.

## Validação Final

- Confirmar `git status --short`.
