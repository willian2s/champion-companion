# Task 006-12 — Fechar fase de gerenciamento do dispositivo

## Status

Pendente.

## Objetivo

Consolidar conclusão da Fase 06, checklist, evidências, dívida adiada, decisões que exigem ADR e atualização planejada da baseline.

## Contexto

Fechamento deve registrar estado real, não presumido. Baseline pode estar desatualizada desde Fase 05 e deve ser planejada/atualizada nesta task final conforme regras do projeto, sem alterar roadmap ou fases concluídas.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- todas as tasks `006-01` a `006-11`
- `docs/core/current-project-baseline.md`
- ADRs vigentes em `docs/decisions/`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`

## Dependências

- `006-11`.

## Entradas

- Resultados de build/install.
- Evidência do smoke test manual.
- Checklist do overview.
- Dívidas e riscos registrados nas tasks anteriores.

## Arquivos Exatos Permitidos

- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-12-close-device-management-phase.md`
- `docs/specs/006-device-management.md`
- `docs/core/current-project-baseline.md` somente nesta task final, se a implementação real da Fase 06 já estiver concluída e exigir alinhamento factual
- novo ADR em `docs/decisions/` somente se decisão arquitetural duradoura foi efetivamente tomada e não coberta

## Arquivos Proibidos

- Qualquer arquivo Kotlin.
- Qualquer arquivo Gradle.
- Room schema.
- `docs/roadmap/champion-companion-roadmap.md`
- Specs/tasks de fases concluídas.
- `docs/specs/_inputs/phase-06-device-management-direction.md`

## Escopo

- Consolidar checklist.
- Registrar comandos executados e resultados.
- Registrar validação manual.
- Registrar falhas conhecidas.
- Registrar regressões introduzidas e status.
- Registrar dívida adiada para Fase 21.
- Registrar decisões que exigem ADR.
- Atualizar baseline se a feature real foi concluída e a baseline estiver desatualizada.

## Fora Do Escopo

- Implementar código.
- Corrigir suíte antiga.
- Alterar roadmap.
- Commit/push.

## Passos

1. Ler documentos obrigatórios.
2. Verificar status de todas as tasks `006-01` a `006-11`.
3. Consolidar checklist do overview.
4. Registrar resultados de `assembleDebug`, `assembleRelease` e `installDebug`.
5. Registrar smoke test manual.
6. Registrar falhas conhecidas e regressões.
7. Registrar dívida adiada para Fase 21.
8. Avaliar ADR sobre associação persistida versus sessão BLE efêmera.
9. Atualizar baseline se aplicável e dentro deste fechamento.
10. Rodar `git status --short`.
11. Não fazer commit nem push.

## Critérios De Aceite

- Overview reflete estado final real.
- Spec reflete comportamento entregue.
- Baseline atualizada ou justificativa documentada.
- Dívida técnica adiada está explícita.
- ADR recomendado/criado quando exigido por decisão duradoura.
- Nenhuma task pendente é marcada como concluída sem evidência.

## Validação Mínima

- Revisão documental.
- `git status --short`.

## Evidência Manual Esperada

- Lista final de comandos e resultados.
- Resultado do smoke test.
- Lista de riscos aceitos.
- Confirmação de que não houve commit/push.

## Atualização Do Overview

- Marcar `006-12` como concluída.
- Atualizar `Status` inicial do overview para concluído somente se todos critérios gerais forem atendidos.

## Validação Final

- Mostrar `git status --short` no fechamento.
- Não fazer commit.
- Não fazer push.
