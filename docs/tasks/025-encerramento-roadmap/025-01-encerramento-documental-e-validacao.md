# 025-01 — Encerramento documental e validação

- **Ticker:** `025`
- **Número:** `01`
- **Status:** completed

## Objetivo

Alinhar fontes autoritativas do roadmap e baseline à decisão de encerrar Fases
17–25 e declarar roadmap completo após Fase 16.

## Resultado esperado

Documentação sem backlog macro futuro ativo, com histórico preservado, fases
17–25 fechadas por decisão explícita e distinção entre `completed` e `closed`.

## Requisitos e critérios cobertos

- Spec 025: R1, R2, R3 e R4;
- Fases 17–25 não aparecem como próximas fases planejáveis;
- Fase 16 permanece `completed`;
- histórico, tickers, código legado, migrations, schemas, dados e evidências
  permanecem preservados.

## Escopo incluído

- Atualizar README e documentos autoritativos de roadmap;
- atualizar ADR-020 com decisão de encerramento;
- corrigir referência da Spec 023 à próxima fase;
- atualizar baseline factual;
- atualizar status e evidências desta subtarefa e overview.

## Escopo excluído

- Código de produção, Gradle, dependências, schemas, migrations e dados;
- remoção de legado ou limpeza de permissões;
- criação de nova funcionalidade;
- novos testes automatizados ou validação física.

## Dependências

- Spec 025 e overview 025 normalizados;
- baseline atual lido;
- `docs/roadmap/post-phase-15-direction.md` e ADR-020 tratados como fontes
  autoritativas;
- nenhuma subtarefa anterior.

## Arquivos e símbolos prováveis

- `docs/roadmap/README.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- `docs/roadmap/post-phase-15-direction.md`;
- `docs/decisions/adr-020-roadmap-phases-and-work-item-tickers.md`;
- `docs/baseline/current-baseline.md`;
- `docs/specs/023-personal-metrics-calibration.md`.

## Passos de implementação

1. Registrar fechamento consolidado das Fases 17–25 com motivo
   `product_maturity_no_further_phases`.
2. Declarar roadmap completo após Fase 16, preservando Fase 13 como
   `closed/partially_delivered` e Fase 16 como `completed`.
3. Atualizar ordem, marcos, priorização e direção futura para não sugerirem
   backlog ativo após Fase 16.
4. Atualizar ADR-020, baseline e Spec 023 com decisão e preservação do histórico.
5. Verificar links, ticker, checklist e diff documental.

## Testes e comandos de validação

```bash
git diff --check
```

Não executar suites Gradle: subtarefa altera somente documentação e não altera
código, dependências ou contratos de runtime.

## Definição de pronto

- documentos autoritativos concordam sobre encerramento e completude;
- nenhuma fase futura permanece descrita como backlog ativo;
- nenhum trabalho não executado é rotulado `completed`;
- overview marca somente `025-01` como concluída e progresso vira `1/1`;
- subtarefa registra arquivos, decisões, comandos, resultados e riscos residuais;
- revisão independente é solicitada.

## Riscos e cuidados

- Manter seções históricas antigas, identificando-as como histórico encerrado.
- Não usar `completed` para Fases 17–25.
- Não interpretar fechamento documental como remoção automática de código legado.
- Não alterar documentos de evidência histórica sem necessidade.

## Execução e fechamento

- **Status final:** `completed`.
- **Data:** 2026-08-20.
- **Arquivos alterados:**
  - `AGENTS.md`;
  - `docs/roadmap/README.md`;
  - `docs/roadmap/champion-companion-roadmap.md`;
  - `docs/roadmap/post-phase-15-direction.md`;
  - `docs/decisions/adr-020-roadmap-phases-and-work-item-tickers.md`;
  - `docs/baseline/current-baseline.md`;
  - `docs/specs/023-personal-metrics-calibration.md`;
  - `docs/specs/025-encerramento-roadmap.md`;
  - arquivos do overview e desta subtarefa.
- **Decisões:** Fases 17–25 foram fechadas por
  `product_maturity_no_further_phases`; `closed` não equivale a `completed`;
  roadmap ficou `complete` após Fase 16; legado técnico permanece preservado.
- **Desvios:** nenhum requisito alterado. `AGENTS.md` foi atualizado porque é
  fonte operacional e ainda apontava Fase 17 como direção futura.
- **Comandos executados:**
  - `git diff --check` — PASS, sem saída.
  - revisão independente SDD — primeira rodada FAIL por referências residuais;
    correções aplicadas em AGENTS.md, ADR-020 e direção pós-Fase 15.
  - revisão independente SDD final — PASS, sem achados bloqueadores, altos,
    médios ou baixos.
- **Resultados e evidências:**
  - checklist 025 atualizado para `[x]` e progresso `1/1`;
  - spec e overview marcados como `completed`;
  - Fases 17–25 aparecem como `closed` em fontes autoritativas;
  - busca de referências no roadmap não encontrou direção futura ativa, somente
    registros históricos explicitamente encerrados;
  - diff contém somente documentação; arquivos novos também passaram por revisão
    estrutural sem erros de whitespace.
- **Riscos residuais:** documentos históricos ainda descrevem escopos originais
  das fases encerradas; seus cabeçalhos e registros finais deixam claro que não
  são backlog ativo. Limpeza de código legado continua fora do escopo.
