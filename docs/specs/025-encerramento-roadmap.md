# 025 — Encerramento do roadmap

- **Ticker:** `025`
- **Status:** `completed`
- **Tipo:** encerramento documental

## Contexto e objetivo

O produto atingiu maturidade suficiente para encerrar a evolução planejada após
a Fase 16. As Fases 17–25 não serão executadas como backlog futuro. Esta spec
registra a decisão explícita, fecha essas fases sem afirmar entregas não
realizadas e torna o roadmap completo, preservando histórico, código legado,
schemas, migrations e evidências.

## Requisitos

### R1 — Encerramento explícito

- Registrar Fases 17–25 como `closed` por decisão de produto;
- distinguir fases entregues, encerradas parcialmente e encerradas sem
  implementação;
- não marcar trabalho não executado como `completed`;
- preservar numeração e histórico das fases.

### R2 — Roadmap completo

- Atualizar direção pós-Fase 15 para não manter fases futuras ativas;
- registrar roadmap como completo após Fase 16 e encerramento das fases
  posteriores;
- remover instrução de planejar Fase 17;
- manter independência entre fases e tickers.

### R3 — Consistência documental

- Alinhar README do roadmap, roadmap histórico, baseline e ADR de governança;
- corrigir referências que apresentem Fase 17 ou posteriores como próximo
  backlog;
- preservar referências históricas sem reescrever evidências antigas.

### R4 — Preservação

- Não alterar código de produção, dependências, schemas, migrations ou dados;
- não remover código legado das Fases 13 ou posteriores;
- não transformar fechamento documental em aceite físico ou funcional.

## Critérios de aceite

1. Fases 17–25 aparecem fechadas por decisão explícita, com motivo e sem
   alegação de entrega inexistente.
2. `post-phase-15-direction.md` declara roadmap completo e não possui direção
   futura ativa após Fase 16.
3. `champion-companion-roadmap.md`, `README.md`, baseline e ADR-020 não
   apresentam Fase 17 como próxima fase planejável.
4. Histórico das fases e independência entre tickers permanecem preservados.
5. Nenhum arquivo de código, Gradle, schema ou dado é alterado.
6. Links, checklist e status da task 025 permanecem consistentes.

## Comportamento atual encontrado

- Fases 0–16 já estão concluídas ou fechadas conforme baseline e direção
  autoritativa.
- Fases 17, 19 e 20 ainda aparecem como direção futura revisável.
- Fases 18 e 21–25 já aparecem removidas, mas ainda constam na ordem histórica e
  precisam de encerramento consolidado.
- ADR-020 autoriza remoção de fases, mas ainda descreve Fases 17, 19 e 20 como
  mantidas para auditoria.

## Abordagem escolhida

Executar uma única subtarefa documental para atualizar fontes autoritativas e
referências de baseline. Usar status `closed` com motivo
`product_maturity_no_further_phases` para fases não executadas, preservando
`completed` somente para Fase 16 e estados históricos já comprovados.

## Alternativas descartadas

- Marcar Fases 17–25 como `completed`: afirmaria entregas inexistentes.
- Apagar seções históricas: destruiria rastreabilidade do planejamento original.
- Criar limpeza de código legado junto do fechamento: ampliaria escopo e risco.
- Criar nova fase de manutenção: contrariaria decisão explícita de encerrar o
  roadmap.

## Arquivos e contratos afetados

- `docs/roadmap/README.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- `docs/roadmap/post-phase-15-direction.md`;
- `docs/decisions/adr-020-roadmap-phases-and-work-item-tickers.md`;
- `docs/baseline/current-baseline.md`;
- `docs/specs/023-personal-metrics-calibration.md`;
- arquivos da task 025.

Não há contratos de runtime afetados.

## Riscos e mitigação

- **Referência residual a backlog futuro:** buscar Fases 17–25 em documentos
  autoritativos e classificar referências históricas preservadas.
- **Confusão entre closed e completed:** registrar motivo de decisão e não
  inventar evidência de implementação.
- **Limpeza indevida de legado:** declarar explicitamente preservação de código,
  migrations, schemas, dados e evidências.

## Estratégia de validação

Validação documental e estrutural, sem testes de código:

```bash
git diff --check
```

Também revisar links, ticker, status, contagem do checklist e ausência de
alterações fora do escopo documental.

## Ordem das subtarefas

```text
025-01 → encerramento documental e validação
```

## Premissas explícitas

- A decisão do usuário é explícita e substitui a direção futura anterior.
- `closed` significa encerrado por decisão de produto; não equivale a
  `completed`.
- Correções futuras pontuais podem existir como specs independentes, sem reabrir
  fases macro automaticamente.
- O roadmap pode receber novo planejamento somente por decisão explícita que
  reabra sua governança; esta task não cria essa autorização.
