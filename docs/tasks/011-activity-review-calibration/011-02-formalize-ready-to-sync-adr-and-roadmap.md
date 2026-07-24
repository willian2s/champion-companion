# Task 011-02 - Formalizar ADR de prontidão e corrigir roadmap

## Status

Concluída em 2026-07-24.

## Objetivo

Planejar e executar, durante a implementação da fase, a criação de ADR com próximo número disponível e a correção do roadmap para fixar `READY_TO_SYNC` como saída da Fase 11.

## Contexto

O roadmap atual ainda atribui à Fase 11 ação funcional de sincronizar, retry do Health Connect e responsabilidade por estados de sync. Isso conflita com a direção da fase e com a linha local-first já consolidada pela ADR-010.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-11-activity-review-calibration-direction.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`

## Dependências

- `011-01`.

## Entradas

- auditoria concluída da fronteira real da fase;
- conflito documental atual entre roadmap e direção da Fase 11.

## Arquivos exatos permitidos

- `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-02-formalize-ready-to-sync-adr-and-roadmap.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `app/build.gradle.kts`
- `AndroidManifest.xml`

## Escopo

- criar ADR da fronteira `READY_TO_SYNC`;
- atualizar roadmap;
- remover sincronização da Fase 11;
- reforçar que a Fase 12 consome `READY_TO_SYNC`;
- formalizar imutabilidade das métricas importadas;
- formalizar calibração como camada derivada;
- formalizar descarte lógico;
- formalizar bloqueio de `PARTIAL`;
- formalizar preservação de revisão na reimportação.

## Fora do escopo

- implementar Health Connect;
- implementar código Kotlin;
- alterar Room ou UI.

## Passos

1. Redigir ADR com próximo número disponível.
2. Corrigir texto da Fase 11 no roadmap.
3. Reforçar texto da Fase 12 para consumo de `READY_TO_SYNC`.
4. Atualizar spec e overview para refletir o alinhamento documental.

## Critérios de aceite

- ADR e roadmap deixam de atribuir sync real à Fase 11;
- Fase 12 fica explicitamente dona dos estados de sync e do provider;
- `title`, `notes`, tipo confirmado e distância calibrada ficam preservados como entradas da Fase 12;
- nenhuma implementação funcional é introduzida.

## Validação manual

- revisão do diff documental;
- conferência de coerência entre ADR, spec, overview e roadmap.

## Comandos de build aplicáveis

- nenhum nesta task documental.

## Evidências

- diff da ADR;
- diff do roadmap;
- referências cruzadas na spec e no overview.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-02` como concluída;
- registrar que contratos e implementação podem seguir com fronteira documental estável.

## Execução

### Arquivos alterados

- `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-02-formalize-ready-to-sync-adr-and-roadmap.md`

### Resultado da ADR

- criada `ADR-011` para formalizar que Fase 11 termina em `READY_TO_SYNC`;
- Fase 12 passa a ser dona explícita de `SYNCING`, `SYNCED`, `SYNC_FAILED` e da integração real com Health Connect;
- métricas importadas foram formalizadas como imutáveis;
- calibração foi formalizada como camada derivada;
- descarte lógico foi formalizado;
- bloqueio de `PARTIAL` foi formalizado;
- preservação de revisão na reimportação foi formalizada.

### Resultado do roadmap

- Fase 11 deixou de citar ação funcional de sincronizar e retry do provider;
- Fase 11 deixou de citar `Health Connect` como item de detalhe da fase;
- fluxo da Fase 11 foi corrigido para sair de `PENDING_REVIEW` e terminar em `READY_TO_SYNC`;
- Fase 11 ganhou fronteira explícita de fase com saída local `READY_TO_SYNC`;
- Fase 12 passou a citar explicitamente consumo de backlog `READY_TO_SYNC` e backlog D1 `PENDING`.

### Coerência preservada

- `title`, `notes`, tipo confirmado e distância calibrada permanecem registrados como entradas da Fase 12;
- nenhuma implementação funcional foi introduzida;
- nenhuma fronteira de Health Connect foi antecipada para Fase 11.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: mudanças ficaram restritas à documentação da Fase 11 e ao roadmap/ADR correspondentes.
