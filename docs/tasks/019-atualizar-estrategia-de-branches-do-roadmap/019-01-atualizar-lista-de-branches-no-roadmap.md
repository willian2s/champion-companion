# 019-01 — Atualizar lista de branches no roadmap

- **Ticker:** `019`
- **Número:** `01`
- **Status:** concluído

## Execução atual

- **Arquivos alterados:** documentação SDD 019 e
  `docs/roadmap/champion-companion-roadmap.md`.
- **Decisão:** separar inventário Git verificável de padrões de nomenclatura
  futuros, sem reservar branches ou tickers.
- **Comandos executados:** `git branch --all --no-color`.
- **Evidência:** branch atual `feat/013-phone-workouts`; branches publicadas
  incluem 001–008, 010–013, `009-watch-heart-rate-history-d1` e
  `fix/015-11-sync-integrity`. Não existem branches dedicadas para 015, 015A,
  016, 017 ou 018.
- **Riscos:** lista ficará factual no momento desta execução; novas branches
  exigem revisão futura.

## Resultado final

- Seção separa branches existentes verificadas e padrões futuros de nome.
- Baseline MotionLog permanece identificada como externa.
- Não foram reservados tickers para branches futuras; padrões de nome ficaram
  separados do inventário factual.
- `git diff --check` passou após edição.
- Revisão independente final aprovou correção sem bloqueadores.

## Objetivo

Atualizar seção de estratégia de branches de
`docs/roadmap/champion-companion-roadmap.md` com nomes verificáveis e distinção
entre histórico, integração e planejamento futuro.

## Escopo incluído

- conferir `git branch --all --no-color`;
- corrigir nomes de branches reais;
- remover branches fictícias de fases concluídas;
- separar baseline externa do MotionLog;
- registrar sugestões futuras coerentes com política atual;
- documentar comandos e evidências.

## Escopo excluído

- criar, renomear, apagar ou trocar branch;
- commit ou push;
- alterar código, specs de produto, fases ou ADRs arquiteturais;
- reescrever referências históricas da baseline externa.

## Dependências

- Spec `019-atualizar-estrategia-de-branches-do-roadmap`;
- `AGENTS.md` e baseline atual;
- inventário Git do checkout.

## Arquivos prováveis

- `docs/roadmap/champion-companion-roadmap.md`;
- este arquivo e `019-00-overview.md`.

## Critérios de aceite cobertos

1. Branch principal e integração atual identificadas.
2. Branches existentes não são inventadas.
3. Branches concluídas não aparecem como próximas sugestões.
4. Baseline MotionLog permanece explicitamente externa.
5. Branches para backup Android, device transfer e exportação de rota não
   aparecem como trabalho sugerido.
6. Checklist, status, comandos e evidências atualizados.

## Validação

```bash
git branch --all --no-color
git diff --check
```

Não executar build, lint ou testes automatizados para mudança exclusivamente
documental.

## Definição de pronto

- seção do roadmap revisada contra inventário Git;
- subtarefa e overview atualizados;
- diff sem erro de whitespace;
- revisão independente solicitada;
- nenhuma branch criada ou alterada.

## Riscos residuais

- branch pode ser criada após esta atualização e exigir nova revisão documental;
- branches remotas podem desaparecer sem atualização do roadmap;
- nomes históricos permanecem por compatibilidade documental.
