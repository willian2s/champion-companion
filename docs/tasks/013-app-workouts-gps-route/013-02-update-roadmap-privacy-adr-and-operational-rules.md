# 013-02 — Atualizar roadmap, privacidade, ADR e regras operacionais

- **Ticker:** `013`
- **Número:** `02`
- **Status:** pending

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
