# 013-02 — Atualizar roadmap, privacidade, ADR e regras operacionais

- **Ticker:** `013`
- **Número:** `02`
- **Status:** concluída

## Objetivo e resultado esperado

Formalizar antes de código a autorização controlada de rota, foreground service da sessão e fronteira F13/F14. Resultado: roadmap/baselines/AGENTS alinhados, ADR-014 aceita e ADR-004 atualizada ou sucedida sem reescrever Fase 12.

## Contexto

Roadmap, ADR-004, baseline e `AGENTS.md` ainda proíbem rota. ADR-004 exigia decisão futura explícita. `Phase003PrivacyGuardTest` ficou incompatível. Nova política deve ser clara, estreita e auditável.

## Documentos obrigatórios

Todos da `013-01`, relatório aprovado de `013-01`, spec/overview 013 e inventário completo de `docs/decisions/`.

## Dependências

`013-01` concluída.

## Arquivos permitidos

`AGENTS.md`; roadmap; baselines afetadas; `docs/decisions/adr-004-operational-privacy-guards.md`; nova `docs/decisions/adr-014-controlled-workout-route-and-session-service.md` ou nome equivalente; spec 013; esta task; overview.

## Arquivos e símbolos prováveis

Seções de privacidade/guard rails do `AGENTS.md`, §5.6 e Fases 12-14 do roadmap, G-13/G-14, ADR-004, ADR-012 e nova ADR-014.

## Arquivos proibidos

Código, Gradle, manifesto, schemas, testes automatizados.

## Requisitos cobertos

Mudança documental obrigatória; privacidade; guard antigo; F13/F14; ADR prevista; ausência de exportação/servidor; F12 sem rota.

## Escopo

- substituir política “calcular e descartar” pela política nova;
- autorizar rota só para caminhada/corrida explícitas;
- manter backup/device transfer negados;
- definir logs permitidos/proibidos;
- mover FGS mínimo para F13 e manter `WorkManager` em F14;
- registrar guard incompatível como dívida conhecida fora da validação;
- alinhar mapeamento Health Connect e lacunas G-13/G-14.

## Fora do escopo

Alterar guard/teste, código, permissões ou banco.

## Passos de implementação

1. Confirmar ADR-014 como próximo número.
2. Criar ADR com contexto, decisão, consequências, retries, proveniência, correlação e alternativas.
3. Atualizar ADR-004 com supersessão parcial explícita, preservando logs/backup.
4. Atualizar roadmap nas seções de princípios, Fases 12, 13 e 14.
5. Atualizar baseline factual sem afirmar implementação ainda inexistente.
6. Atualizar `AGENTS.md`: remover proibição absoluta e permitir somente escopos autorizados; manter bloqueio em logs/diagnóstico/export.
7. Atualizar gaps/Health Connect mapping.
8. Registrar dívida do `Phase003PrivacyGuardTest` sem editá-lo.
9. Atualizar overview após revisão.

## Critérios de aceite

- política nova aparece literalmente ou semanticamente equivalente;
- F12 segue concluída sem `ExerciseRoute`;
- F13/F14 não são renumeradas;
- ADR-014 cobre persistência, FGS, GPS preciso, correlação, fontes e identidade HC;
- `AGENTS.md` não proíbe implementação autorizada nem libera rota fora do escopo;
- guard antigo fica explícito, sem contorno.

## Validação manual

Revisar links, numeração ADR, consistência entre documentos e busca por afirmações conflitantes.

## Comandos

Nenhum Gradle. Não executar testes.

## Evidências

Diff documental, matriz documento→decisão, busca de conflitos e `git status`.

## Screenshots

Não aplicável.

## Definição de pronto

Decisão arquitetural aceita e fontes autoritativas coerentes; overview atualizado.

## Riscos e cuidados

Linguagem ampla pode autorizar rastreamento passivo. Especificar início/fim, tipos, deleção, sem servidor/export e escopos de código.

## Rollback

Reverter ADR e documentos em conjunto; nunca deixar `AGENTS.md` divergente do roadmap/ADR.

Não criar, alterar ou executar testes automatizados.

## Registro de execução

### Estado final

- Dependência `013-01` confirmada como concluída; ticker `013` confirmado em
  spec, pasta, overview e subtarefas.
- ADR-014 confirmada como próximo número disponível no inventário
  `adr-001` a `adr-013`.
- Política documental aceita sem afirmar implementação funcional de rota,
  GPS ou foreground service.

### Decisões e desvios

- Criada `ADR-014` para autorizar rota somente em caminhada/corrida iniciada
  explicitamente, com persistência local incremental, FGS mínimo, correlação
  D3/D4, retry e identidade Health Connect canônica.
- ADR-004 recebeu supersessão parcial explícita; logs, backup, device transfer,
  exportação, testes e escopos fora da Fase 13 continuam protegidos.
- Roadmap moveu FGS mínimo de sessão para F13 e manteve WorkManager/tarefas
  adiáveis em F14, sem renumerar fases.
- Baselines de estado, banco, inventário, source map, paridade, lacunas e
  Health Connect foram alinhadas. Não foram alterados código, Gradle,
  manifesto, schemas ou `Phase003PrivacyGuardTest`.
- Desvio mínimo: além de `current-baseline.md`, foram atualizados baselines
  diretamente conflitantes (`database-reference.md`, `feature-inventory.md`,
  `feature-parity-checklist.md`, `gaps-and-open-questions.md`,
  `health-connect-mapping.md` e `source-map.md`) para evitar fontes
  autoritativas divergentes.

### Arquivos alterados

- `AGENTS.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-inventory.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/source-map.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`
- `docs/decisions/adr-014-controlled-workout-route-and-session-service.md`
- este arquivo e `013-00-overview.md`

### Comandos executados, resultados e evidências

- `git status --short`: somente documentação autorizada alterada; nenhum
  arquivo de código, Gradle, manifesto, schema ou teste alterado.
- `git diff --check`: passou, sem whitespace inválido.
- `git diff --no-index --check /dev/null docs/decisions/adr-014-controlled-workout-route-and-session-service.md`: passou; ADR nova foi validada separadamente por ainda estar não rastreada.
- Busca manual de `## Checklist`: overview mantém uma única seção.
- Busca cruzada em roadmap, AGENTS, baselines e ADRs confirmou: F12 sem rota;
  F13 com rota estreita e explícita; F14 com WorkManager adiável; sem servidor,
  exportação, backup/device transfer, logs sensíveis ou contorno do guard.
- Nenhum comando Gradle executado e nenhum teste automatizado executado,
  conforme escopo documental explícito.
- Revisão independente solicitada e concluída: aprovada com ressalvas, sem
  bloqueadores. Ressalva operacional: ADR-014 nova permanece `??` no Git até
  futura integração; conteúdo está presente e validado no working tree.

### Riscos residuais

- `Phase003PrivacyGuardTest` continua incompatível e não validará F13 até
  decisão própria; dívida registrada sem contorno.
- Implementação, migration v5→v6, retenção/deleção, provider Health Connect,
  limites de rota e validação física permanecem nas subtarefas seguintes.
- ADR-013 e documentos históricos de F15/F12 preservam a fronteira da fase em
  que foram aceitos; não foram reescritos para não alterar decisões históricas.

### Estado de conclusão

- Critérios documentais atendidos.
- Próxima subtarefa elegível: `013-03`; não iniciada automaticamente.
