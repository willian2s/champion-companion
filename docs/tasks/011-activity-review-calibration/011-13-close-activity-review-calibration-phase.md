# Task 011-13 - Fechar fase de revisão e calibração

## Status

Planejada em 2026-07-24.

## Objetivo

Encerrar documentalmente a Fase 11 com spec, overview, ADR, roadmap, baseline factual e riscos alinhados ao estado entregue.

## Contexto

Depois da validação manual final, a fase precisa consolidar fronteiras, evidências, riscos aceitos, baseline factual e pendências futuras sem reabrir escopo funcional.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-12-run-manual-activity-review-validation.md`
- ADR criada na fase
- roadmap corrigido na fase
- baselines afetadas pela entrega

## Dependências

- `011-12`.

## Entradas

- fase implementada e validada manualmente;
- ADR e roadmap já alinhados;
- screenshots e achados finais registrados.

## Arquivos exatos permitidos

- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-13-close-activity-review-calibration-phase.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/roadmap/champion-companion-roadmap.md`
- ADR da fase criada em `docs/decisions/`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- fechar spec e overview;
- atualizar baseline factual e checklist de paridade quando necessário;
- registrar riscos aceitos e dívidas futuras;
- confirmar fronteira `READY_TO_SYNC` entregue;
- registrar ausência de sync real na fase.

## Fora do escopo

- nova implementação funcional;
- nova migration;
- novas telas;
- Health Connect.

## Passos

1. Atualizar spec e overview com estado final entregue.
2. Atualizar baseline factual e checklist/gaps quando necessário.
3. Registrar riscos residuais aceitos e dívidas futuras.
4. Confirmar critérios de aceite e `Definition of Done` da fase.

## Critérios de aceite

- documentação final reflete o estado real entregue;
- fronteira Fase 11/Fase 12 fica inequívoca;
- riscos aceitos, pendências e dívida ficam registrados;
- nenhuma implementação funcional nova é introduzida.

## Validação manual

- revisão final do diff documental;
- conferência de coerência entre spec, overview, ADR, roadmap e baseline factual.

## Comandos de build aplicáveis

- nenhum comando novo além do já validado em `011-12`.

## Evidências

- diff documental final;
- referências cruzadas atualizadas;
- checklist da fase marcado como concluído.

## Screenshot quando visual

- reutilizar screenshots já registradas na fase.

## Atualização do overview

- marcar `011-13` como concluída;
- fechar checklist integral da fase.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
